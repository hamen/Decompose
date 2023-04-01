package com.arkivanov.decompose.extensions.compose.jetpack.stack.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack

internal class StackAnimation<C : Any, in T : Any> {

    @Composable
    operator fun invoke(stack: ChildStack<C, T>, content: @Composable (C) -> Unit) {
        var activePage by remember { mutableStateOf(stack.activePage()) }
        var items by remember { mutableStateOf(getAnimationItems(newPage = activePage, oldPage = null)) }

        if (stack.active.configuration != activePage.child.configuration) {
            val oldPage = activePage
            activePage = stack.activePage()
            items = getAnimationItems(newPage = activePage, oldPage = oldPage)
        }

        Box {
            items.forEach { (configuration, item) ->
                key(configuration) {
                    Child(
                        isEnter = item.isEnter,
                        child = item.child,
                        onFinished = {
                            if (item.isEnter) {
                                items += (configuration to item.copy(otherChild = null))
                            } else {
                                items -= configuration
                            }
                        },
                        content = content,
                    )
                }
            }
        }
    }

    @Composable
    private fun Child(
        isEnter: Boolean,
        child: Child.Created<C, T>,
        onFinished: () -> Unit,
        content: @Composable (C) -> Unit,
    ) {
        val animator = remember(child.configuration) {
            StackAnimator()
        }

        animator(
            isEnter = isEnter,
            onFinished = onFinished,
        ) { modifier ->
            Box(modifier = modifier) {
                content(child.configuration)
            }
        }
    }

    private fun ChildStack<C, T>.activePage(): Page<C, T> =
        Page(child = active, index = items.lastIndex)

    private fun getAnimationItems(newPage: Page<C, T>, oldPage: Page<C, T>?): Map<C, AnimationItem<C, T>> =
        when {
            oldPage == null ->
                listOf(AnimationItem(child = newPage.child, isEnter = true))

            newPage.index >= oldPage.index ->
                listOf(
                    AnimationItem(child = oldPage.child, isEnter = false, otherChild = newPage.child),
                    AnimationItem(child = newPage.child, isEnter = true, otherChild = oldPage.child),
                )

            else ->
                listOf(
                    AnimationItem(child = newPage.child, isEnter = true, otherChild = oldPage.child),
                    AnimationItem(child = oldPage.child, isEnter = false, otherChild = newPage.child),
                )
        }.associateBy { it.child.configuration }

    private data class AnimationItem<out C : Any, out T : Any>(
        val child: Child.Created<C, T>,
        val isEnter: Boolean,
        val otherChild: Child.Created<C, T>? = null,
    )

    private class Page<out C : Any, out T : Any>(
        val child: Child.Created<C, T>,
        val index: Int,
    )
}
