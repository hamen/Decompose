package com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation

import androidx.compose.foundation.layout.Box

internal fun <C : Any, T : Any> emptyStackAnimation(): StackAnimation<C, T> =
    StackAnimation { stack, modifier, isEnabled, childContent ->
        if (isEnabled) {
            Box(modifier = modifier) {
                childContent(stack.active)
            }
        }
    }
