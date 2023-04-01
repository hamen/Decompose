package com.arkivanov.decompose.extensions.compose.jetpack.stack

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.arkivanov.decompose.extensions.compose.jetpack.stack.animation.StackAnimation
import com.arkivanov.decompose.router.stack.ChildStack

@Composable
fun <C : Any, T : Any> Children(
    stack: ChildStack<C, T>,
    content: @Composable (C) -> Unit,
) {
    val anim = remember { StackAnimation<C, T>() }
    anim(stack = stack, content = content)
}
