package com.arkivanov.decompose.extensions.compose.jetpack.stack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import org.junit.Rule
import org.junit.Test
import java.io.Serializable

@Suppress("TestFunctionName")
class ChildrenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun GIVEN_child_A_displayed_WHEN_push_child_B_THEN_child_A_disposed() {

        val state = mutableStateOf(routerState(activeConfig = Config.A))
        setContent(state)

        state.setValueOnIdle(routerState(activeConfig = Config.B, backstack = listOf(Config.A)))

        composeRule.onNodeWithText(text = "ChildA", substring = true).assertDoesNotExist()
    }

    private fun setContent(stack: State<ChildStack<Config, Config>>) {
        composeRule.setContent {
            Children(stack = stack.value) { config ->
                when (config) {
                    Config.A -> Child(name = "A")
                    Config.B -> Child(name = "B")
                }
            }
        }

        runOnIdle {}
    }

    private fun routerState(activeConfig: Config, backstack: List<Config> = emptyList()): ChildStack<Config, Config> =
        ChildStack(
            active = Child.Created(configuration = activeConfig, instance = activeConfig),
            backStack = backstack.map { Child.Created(configuration = it, instance = it) }
        )

    @Composable
    private fun Child(name: String) {
        var count by rememberSaveable { mutableStateOf(0) }

        BasicText(
            text = "Child$name=$count",
            modifier = Modifier.clickable { count++ }
        )
    }

    private fun <T> MutableState<T>.setValueOnIdle(value: T) {
        runOnIdle { this.value = value }
        runOnIdle {}
    }

    private fun runOnIdle(block: () -> Unit) {
        composeRule.runOnIdle(block)
    }

    sealed class Config : Serializable {
        object A : Config()
        object B : Config()
    }
}
