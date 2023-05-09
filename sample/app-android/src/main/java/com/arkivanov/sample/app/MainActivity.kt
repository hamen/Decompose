package com.arkivanov.sample.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.window.BackEvent
import android.window.OnBackAnimationCallback
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.darkColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.android.DefaultViewContext
import com.arkivanov.decompose.extensions.compose.jetbrains.PredictiveBackGestureOverlay
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.backHandler
import com.arkivanov.essenty.lifecycle.essentyLifecycle
import com.arkivanov.sample.shared.dynamicfeatures.dynamicfeature.DefaultFeatureInstaller
import com.arkivanov.sample.shared.root.RootComponent
import com.arkivanov.sample.shared.root.DefaultRootComponent
import com.arkivanov.sample.shared.root.RootContent
import com.arkivanov.sample.shared.root.RootView

class MainActivity : AppCompatActivity() {

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val backDispatcher = BackDispatcher()

        val root =
            MainComponent(
//                componentContext = defaultComponentContext(),
                componentContext = DefaultComponentContext(
                    lifecycle = essentyLifecycle(),
                    backHandler = backDispatcher,
                ),
            )

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colors.background) {
                    PredictiveBackGestureOverlay(
                        backDispatcher = backDispatcher,
                        backIcon = Icons.Default.ArrowBack,
                        modifier = Modifier.fillMaxSize(),
                        onClose = ::finish,
                    ) {
                        MainContent(component = root)
                    }
                }
            }
        }
    }

}
