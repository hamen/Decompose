package com.arkivanov.decompose.extensions.compose.jetpack.stack.animation

import android.util.Log
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.isFinished
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

internal class StackAnimator(
    private val animationSpec: FiniteAnimationSpec<Float> = tween(),
) {

    @Composable
    operator fun invoke(
        isEnter: Boolean,
        onFinished: () -> Unit,
        content: @Composable (Modifier) -> Unit,
    ) {
        // This is printed for both isEnter true and false, for every child
        Log.i("MyTest", "Animator: $isEnter")

        val animationState = remember(isEnter) {
            // This is not re-evaluated on isEnter change from true to false (when the child exits the scene)
            Log.i("MyTest", "Animator remember: $isEnter")
            AnimationState(initialValue = 0F)
        }

        // As a result, animationState is not updated on isEnter change, and this effect is not re-executed
        LaunchedEffect(animationState) {
            animationState.animateTo(
                targetValue = 1F,
                animationSpec = animationSpec, // Commenting out this line fixes the issue
                sequentialAnimation = !animationState.isFinished,
            )

            // So this is not called when isEnter changes the second time, and the child is not removed
            onFinished()
        }

        content(Modifier.alpha(if (isEnter) animationState.value else 1F - animationState.value))
    }
}
