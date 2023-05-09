package com.arkivanov.sample.app

import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.predictiveBackGestureAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.navigate
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.sample.app.MainComponent.Child
import kotlinx.parcelize.Parcelize

@Composable
fun MainContent(component: MainComponent) {
    Children(
        stack = component.stack,
//        animation = stackAnimation(fade() + scale()),
        animation = predictiveBackGestureAnimation(
            backHandler = component.backHandler,
            animation = stackAnimation(fade() + scale()),
            onBack = component::onBackPressed,
        ),
    ) {
        when (val child = it.instance) {
            is Child.List -> ListContent(child.component)
            is Child.Details -> DetailsContent(child.component)
        }
    }
}

@Composable
fun ListContent(component: ListComponent) {
    Column {
        TopAppBar(
            title = { Text("Main screen") },
        )

        LazyColumn {
            items(component.items) {
                Text(
                    text = it,
                    modifier = Modifier.fillMaxWidth().clickable { component.onItemClicked(it) }.padding(16.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun DetailsContent(component: DetailsComponent) {
    Surface {
        Column {
            TopAppBar(
                title = { Text("Details screen") },
                navigationIcon = {
                    IconButton(
                        onClick = component.onBackPressed,
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            Text(text = component.text, modifier = Modifier.padding(16.dp))
        }
    }
}


class MainComponent(componentContext: ComponentContext) : BackHandlerOwner, ComponentContext by componentContext {

    private val navigation = StackNavigation<Config>()

    val stack: Value<ChildStack<*, Child>> =
        childStack(
            source = navigation,
            initialConfiguration = Config.List,
//            handleBackButton = true,
        ) { config, context ->
            when (config) {
                is Config.List -> Child.List(ListComponent { text ->
                    navigation.navigate { it + listOf(Config.Details(text), Config.Details(text + "_")) }
//                    navigation.push(Config.Details(it))
                })
                is Config.Details -> Child.Details(DetailsComponent(context, config.text, ::onBackPressed))
            }
        }

    fun onBackPressed() {
        navigation.pop()
    }

    sealed class Child {
        class List(val component: ListComponent) : Child()
        class Details(val component: DetailsComponent) : Child()
    }

    @Parcelize
    private sealed class Config : Parcelable {
        object List : Config()
        data class Details(val text: String) : Config()
    }
}

class ListComponent(
    val onItemClicked: (String) -> Unit,
) {
    val items: List<String> = List(100) { LOREN }
}

class DetailsComponent(
    componentContext: ComponentContext,
    val text: String,
    val onBackPressed: () -> Unit,
) : ComponentContext by componentContext {

//    init {
//        lateinit var callback: BackCallback
//        callback = BackCallback { callback.isEnabled = false }
//
//        backHandler.register(callback)
//    }
}

private val LOREN =
    """
Lorem ipsum dolor sit amet. Sit ducimus autem sit ipsum nostrum aut consequatur dignissimos ut similique aliquam aut quam natus At asperiores aliquid. Et porro dolorum qui dolorum quasi aut cumque earum eum unde ducimus sit pariatur adipisci non nemo alias. Vel magni quia qui obcaecati nobis in nulla harum? In natus nihil nam porro sunt eos odio dolores id quisquam alias id consequuntur error.

Et consequuntur Quis sed optio dolores aut quod rerum. In assumenda molestiae et corporis natus non aspernatur aperiam vel delectus laboriosam ut laboriosam expedita qui dolore libero rem nisi maiores?

Est cumque alias eum molestiae asperiores sit aliquid delectus aut ipsum distinctio eum ducimus voluptas. Qui explicabo corrupti in atque consectetur est nemo facere in consectetur dolorem ut facilis eius.

Quo expedita ullam At placeat officiis aut dolor provident. Est quibusdam excepturi aut delectus consectetur qui porro nulla eum sunt laborum. Sit impedit molestias cum voluptatem fugiat et unde error et facere quia. Et porro sint ea sint nesciunt aut fugiat maxime qui error dolores.

Sed recusandae quae qui nesciunt numquam cum galisum rerum eum galisum labore sit dolore earum. In eveniet galisum sed aliquam nesciunt et galisum consectetur est architecto dolorem ut aspernatur quisquam. 33 doloribus delectus cum autem autem vel iure facilis sed quidem voluptas. Aut consequuntur assumenda et dolores voluptas quo laborum beatae aut quam optio id doloribus dolore quo veniam corrupti ut galisum galisum.

Et libero dolorem aut expedita distinctio et cupiditate voluptatem aut facilis veniam et mollitia saepe et amet aperiam. Ut corporis velit non possimus perspiciatis ut quibusdam explicabo. Sit deleniti repudiandae est quibusdam nemo ut voluptatem consequuntur nam dolorem ipsum quo consequatur amet qui temporibus dignissimos. Sit alias distinctio ut suscipit reprehenderit aut optio quidem ab quae modi?

Qui quia consequuntur rem repudiandae nulla vel atque enim aut illum veritatis id eaque dolor eos rerum laudantium. Ex saepe fuga qui facere esse et veniam dolorem est excepturi dolorem ex vitae exercitationem ut corporis dolores qui internos sunt. Vel numquam unde aut totam nulla eum magnam officiis ex doloribus quia. Et commodi incidunt qui labore quod eos doloremque rerum.

Sit alias ducimus id laboriosam commodi quo pariatur vero et repellat voluptatum et ipsam enim ab consequatur quia qui voluptates voluptatem. Ut mollitia earum aut perferendis dolorem sed nemo enim qui nobis esse sit recusandae deleniti?
""".trimIndent()
