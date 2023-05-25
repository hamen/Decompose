package com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.milliseconds

class PredictiveBackParams(
    val backHandler: BackHandler,
    val activeModifier: (progress: Float, edge: BackEvent.SwipeEdge) -> Modifier = ::getPredictiveBackGestureAnimationModifier,
    val onBack: () -> Unit,
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

internal abstract class AbstractStackAnimation<C : Any, T : Any>(
    private val disableInputDuringAnimation: Boolean,
    private val predictiveBackParams: PredictiveBackParams?,
) : StackAnimation<C, T> {

    @Composable
    protected abstract fun Child(
        item: AnimationItem<C, T>,
        onFinished: () -> Unit,
        content: @Composable (child: Child.Created<C, T>) -> Unit,
    )

    protected data class GestureData<out C : Any, out T : Any>(
        val activeChild: Child.Created<C, T>,
        val prevChild: Child.Created<C, T>,
        val isButtonReleased: Boolean = false,
        val progress: Float = 0F,
        val edge: BackEvent.SwipeEdge = BackEvent.SwipeEdge.UNKNOWN,
    )

    @Composable
    override operator fun invoke(stack: ChildStack<C, T>, modifier: Modifier, content: @Composable (child: Child.Created<C, T>) -> Unit) {
        val gestureData: MutableState<GestureData<C, T>?> = remember(stack) { mutableStateOf(null) }
        var activePage by remember(gestureData.value != null) { mutableStateOf(stack.activePage()) }
        var items by remember(gestureData.value != null) { mutableStateOf(getAnimationItems(newPage = activePage, oldPage = null)) }

        if (stack.active.configuration != activePage.child.configuration) {
            val oldPage = activePage
            activePage = stack.activePage()
            items = getAnimationItems(newPage = activePage, oldPage = oldPage)
        }

        val itms =
            gestureData.value?.let {
                mapOf(
                    it.prevChild.configuration to AnimationItem(
                        child = it.prevChild,
                        direction = Direction.ENTER_BACK,
                        isInitial = true,
                    ),
                    it.activeChild.configuration to AnimationItem(
                        child = it.activeChild,
                        direction = Direction.EXIT_FRONT,
                        isInitial = true,
                        gestureData = it,
                    ),
                )
            } ?: items

        Box(modifier = modifier) {
            itms.forEach { (configuration, item) ->
                key(configuration) {
                    Box(
                        modifier = item.gestureData?.let { predictiveBackParams?.activeModifier?.invoke(it.progress, it.edge) } ?: Modifier,
                    ) {
                        Child(
                            item = item,
                            onFinished = {
                                if (!item.isInitial) {
                                    if (item.direction.isExit) {
                                        items -= configuration
                                    } else {
                                        items += (configuration to item.copy(otherChild = null))
                                    }
                                }
                            },
                            content = content,
                        )
                    }
                }
            }

            // A workaround until https://issuetracker.google.com/issues/214231672.
            // Normally only the exiting child should be disabled.
            if (disableInputDuringAnimation && (items.size > 1)) {
                Overlay(modifier = Modifier.matchParentSize())
            }
        }

        if ((predictiveBackParams != null) && stack.backStack.isNotEmpty()) {
            DisposableEffect(stack) {
                val callback =
                    BackCallback(
                        onBackStarted = { event ->
                            gestureData.value =
                                GestureData(
                                    activeChild = stack.active,
                                    prevChild = stack.backStack.last(),
                                    progress = event.progress,
                                    edge = event.swipeEdge,
                                )
                        },
                        onBackProgressed = { event ->
                            gestureData.value =
                                gestureData.value?.copy(
                                    progress = event.progress,
                                    edge = event.swipeEdge,
                                )
                        },
                        onBackCancelled = {
                            gestureData.value = null
                        },
                        onBack = {
                            if (gestureData.value == null) {
                                predictiveBackParams.onBack()
                            } else {
                                gestureData.value = gestureData.value?.copy(isButtonReleased = true)
                            }
                        },
                    )

                predictiveBackParams.backHandler.register(callback)

                onDispose { predictiveBackParams.backHandler.unregister(callback) }
            }

            if (gestureData.value?.isButtonReleased == true) {
                LaunchedEffect(Unit) {
                    val data = gestureData.value ?: return@LaunchedEffect

                    var p = data.progress
                    while (p <= 1F && isActive) {
                        gestureData.value = data.copy(progress = p)
                        delay(16.milliseconds)
                        p += 0.075F
                    }

                    if (isActive) {
                        gestureData.value = null
                        predictiveBackParams.onBack()
                    }
                }
            }
        }
    }

    @Composable
    private fun Overlay(modifier: Modifier) {
        Box(
            modifier = modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        )
    }

    private fun ChildStack<C, T>.activePage(): Page<C, T> =
        Page(child = active, index = items.lastIndex)

    private fun getAnimationItems(newPage: Page<C, T>, oldPage: Page<C, T>?): Map<C, AnimationItem<C, T>> =
        when {
            oldPage == null ->
                listOf(AnimationItem(child = newPage.child, direction = Direction.ENTER_FRONT, isInitial = true))

            newPage.index >= oldPage.index ->
                listOf(
                    AnimationItem(child = oldPage.child, direction = Direction.EXIT_BACK, otherChild = newPage.child),
                    AnimationItem(child = newPage.child, direction = Direction.ENTER_FRONT, otherChild = oldPage.child),
                )

            else ->
                listOf(
                    AnimationItem(child = newPage.child, direction = Direction.ENTER_BACK, otherChild = oldPage.child),
                    AnimationItem(child = oldPage.child, direction = Direction.EXIT_FRONT, otherChild = newPage.child),
                )
        }.associateBy { it.child.configuration }

    protected data class AnimationItem<out C : Any, out T : Any>(
        val child: Child.Created<C, T>,
        val direction: Direction,
        val gestureData: GestureData<C, T>? = null,
        val isInitial: Boolean = false,
        val otherChild: Child.Created<C, T>? = null,
    )

    private class Page<out C : Any, out T : Any>(
        val child: Child.Created<C, T>,
        val index: Int,
    )
}
