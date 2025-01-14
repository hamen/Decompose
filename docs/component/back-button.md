# Back button handling

Some devices (e.g. Android) have hardware back buttons. A very common use case is to close the current screen, or the app if there is only one screen in the stack. Another possible use case is to show a confirmation dialog before closing the app.

## Navigation with back button

`Child Stack` and `Child Pages` can automatically navigate back when the back button is pressed. All you need to do is to supply the `handleBackButton=true` argument when you initialize a navigation model.

Similarly, `Child Slot` can automatically dismiss the child component when the back button is pressed. see the [Child Slot](/Decompose/navigation/slot/overview/) documentation page for more information.

## Manual back button handling

The back button can be handled manually using `BackHandler` (comes from [Essenty](https://github.com/arkivanov/Essenty) library), which is provided by `ComponentContext`. The `decompose` module adds Essenty's `back-handler` module as `api` dependency, so you don't need to explicitly add it to your project. Please familiarise yourself with Essenty library, especially with the `BackHandler` and `BackDispatcher`.

### Usage example

```kotlin
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback

class SomeComponent(
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val backCallback = BackCallback { /* Handle the back button */ }

    init {
        backHandler.register(backCallback)
    }

    private fun updateBackCallback() {
        // Set isEnabled to true if you want to override the back button
        backCallback.isEnabled = true // or false
    }
}
```

## Predictive Back Gesture

Decompose experimentally supports the new [Android Predictive Back Gesture](https://developer.android.com/guide/navigation/custom-back/predictive-back-gesture), not only on Android. The UI part is covered by Compose extensions, please see the [related docs](../../extensions/compose#predictive-back-gesture).
