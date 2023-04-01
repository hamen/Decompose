package com.arkivanov.decompose.router.stack

import com.arkivanov.decompose.Child

/**
 * A state holder for `Child Stack`.
 */
// Moving this class together with Child to extensions-compose-jetpack fixes the issue
data class ChildStack<out C : Any, out T : Any>(
    val active: Child.Created<C, T>,
    val backStack: List<Child.Created<C, T>> = emptyList(),
) {

    /**
     * Returns the full stack of component configurations, ordered from tail to head.
     */
    val items: List<Child.Created<C, T>> = backStack + active
}
