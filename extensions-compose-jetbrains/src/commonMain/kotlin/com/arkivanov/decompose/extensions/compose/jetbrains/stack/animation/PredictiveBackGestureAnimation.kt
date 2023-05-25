package com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

/**
 * Wraps the provided [animation], handles the predictive back gesture and animates
 * the transition from the current [Child] to the previous one.
 * Calls [onBack] when the animation is finished.
 */
@ExperimentalDecomposeApi
fun <C : Any, T : Any> predictiveBackGestureAnimation(
    backHandler: BackHandler,
    animation: StackAnimation<C, T>? = null,
    activeModifier: (progress: Float, edge: BackEvent.SwipeEdge) -> Modifier = ::getPredictiveBackGestureAnimationModifier,
    onBack: () -> Unit,
): StackAnimation<C, T> =
    PredictiveBackGestureAnimation(
        backHandler = backHandler,
        animation = animation,
        activeModifier = activeModifier,
        onBack = onBack,
    )

private fun getPredictiveBackGestureAnimationModifier(
    progress: Float,
    edge: BackEvent.SwipeEdge,
): Modifier =
    Modifier
        .scale(1F - progress * 0.25F)
        .absoluteOffset(
            x = when (edge) {
                BackEvent.SwipeEdge.LEFT -> 32.dp * progress
                BackEvent.SwipeEdge.RIGHT -> (-32).dp * progress
                BackEvent.SwipeEdge.UNKNOWN -> 0.dp
            },
        )
        .alpha(((1F - progress) * 2F).coerceAtMost(1F))
        .clip(RoundedCornerShape(percent = 10))

internal class PredictiveBackGestureAnimation<C : Any, T : Any>(
    private val backHandler: BackHandler,
    private val animation: StackAnimation<C, T>?,
    private val activeModifier: (progress: Float, edge: BackEvent.SwipeEdge) -> Modifier,
    private val onBack: () -> Unit,
) : StackAnimation<C, T> {

    @Composable
    override fun invoke(
        stack: ChildStack<C, T>,
        modifier: Modifier,
        content: @Composable (child: Child.Created<C, T>) -> Unit,
    ) {
        val data: MutableState<GestureData<C, T>?> = remember(stack) { mutableStateOf(null) }
        val childContents = remember { ChildContents(content) }

        BackGestureTracker(
            stack = stack,
            dataState = data,
            childContents = childContents,
        )

        Content(
            stack = stack,
            modifier = modifier,
            dataState = data,
            childContents = childContents,
        )
    }

    @Composable
    private fun BackGestureTracker(
        stack: ChildStack<C, T>,
        dataState: MutableState<GestureData<C, T>?>,
        childContents: ChildContents<C, T>,
    ) {
        DisposableEffect(stack) {
            if (stack.backStack.isEmpty()) {
                return@DisposableEffect onDispose {}
            }

            val callback =
                BackCallback(
                    onBackStarted = { event ->
                        dataState.value =
                            GestureData(
                                activeChild = stack.active,
                                prevChild = stack.backStack.last(),
                                progress = event.progress,
                                edge = event.swipeEdge,
                            )
                    },
                    onBackProgressed = { event ->
                        dataState.value =
                            dataState.value?.copy(
                                progress = event.progress,
                                edge = event.swipeEdge,
                            )
                    },
                    onBackCancelled = {
                        dataState.value?.also {
                            childContents.remove(it.prevChild.configuration)
                            dataState.value = null
                        }
                    },
                    onBack = {
                        if (dataState.value == null) {
                            onBack()
                        } else {
                            dataState.value = dataState.value?.copy(isButtonReleased = true)
                        }
                    },
                )

            backHandler.register(callback)
            onDispose { backHandler.unregister(callback) }
        }
    }

    @Composable
    private fun Content(
        stack: ChildStack<C, T>,
        modifier: Modifier,
        dataState: MutableState<GestureData<C, T>?>,
        childContents: ChildContents<C, T>,
    ) {
        val animKey = remember { mutableStateOf(0) }
        val data = dataState.value

        if (data?.isButtonReleased == true) {
            LaunchedEffect(Unit) {
                var p = data.progress
                while (p <= 1F && isActive) {
                    dataState.value = data.copy(progress = p)
                    delay(16.milliseconds)
                    p += 0.075F
                }

                if (isActive) {
                    childContents.remove(data.activeChild.configuration)
                    dataState.value = null
                    animKey.value++
                    onBack()
                }
            }
        }

        val anim = animation ?: emptyStackAnimation()

        key(animKey.value) {
            anim(stack = stack, modifier = modifier) {
                if (data == null) {
                    childContents(it)
                }
            }
        }

        if (data != null) {
            Box(modifier) {
                childContents(data.prevChild)

                Box(modifier = if (data.progress == 0F) Modifier else activeModifier(data.progress, data.edge)) {
                    childContents(data.activeChild)
                }
            }
        }
    }

    private data class GestureData<out C : Any, out T : Any>(
        val activeChild: Child.Created<C, T>,
        val prevChild: Child.Created<C, T>,
        val isButtonReleased: Boolean = false,
        val progress: Float = 0F,
        val edge: BackEvent.SwipeEdge = BackEvent.SwipeEdge.UNKNOWN,
    )

    private class ChildContents<in C : Any, in T : Any>(
        private val content: @Composable (Child.Created<C, T>) -> Unit
    ) {
        private val map = HashMap<C, @Composable (Child.Created<C, T>) -> Unit>()

        @Composable
        operator fun invoke(child: Child.Created<C, T>) {
            val childContent = map.getOrPut(key = child.configuration) { movableContentOf(content) }
            childContent(child)
        }

        fun remove(config: C) {
            map -= config
        }
    }
}
