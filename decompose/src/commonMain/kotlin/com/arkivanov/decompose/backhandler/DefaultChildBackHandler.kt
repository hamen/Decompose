package com.arkivanov.decompose.backhandler

import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandler
import kotlin.properties.Delegates.observable

internal class DefaultChildBackHandler(
    private val parent: BackHandler,
    isEnabled: Boolean,
    priority: Int,
) : ChildBackHandler {

    private val parentCallback =
        BackCallback(
            isEnabled = false,
            priority = priority,
            onBackStarted = ::onBackStarted,
            onBackProgressed = ::onBackProgressed,
            onBackCancelled = ::onBackCancelled,
            onBack = ::onBack,
        )

    private var set = emptySet<BackCallback>()
    private val enabledChangedListener: (Boolean) -> Unit = { updateParentCallbackEnabledState() }

    override var isEnabled: Boolean by observable(isEnabled) { _, _, _ -> updateParentCallbackEnabledState() }
    private var isStarted = false

    override fun start() {
        if (!isStarted) {
            isStarted = true
            parent.register(parentCallback)
        }
    }

    override fun stop() {
        if (isStarted) {
            isStarted = false
            parent.unregister(parentCallback)
        }
    }

    override fun register(callback: BackCallback) {
        check(callback !in set) { "Callback is already registered" }

        this.set += callback
        callback.addEnabledChangedListener(enabledChangedListener)
        updateParentCallbackEnabledState()
    }

    override fun unregister(callback: BackCallback) {
        check(callback in set) { "Callback is not registered" }

        callback.removeEnabledChangedListener(enabledChangedListener)
        this.set -= callback
        updateParentCallbackEnabledState()
    }

    private fun updateParentCallbackEnabledState() {
        parentCallback.isEnabled = isEnabled && set.any(BackCallback::isEnabled)
    }

    private fun onBackStarted(backEvent: BackEvent) {
        set.findMostImportant()?.onBackStarted(backEvent)
    }

    private fun onBackProgressed(backEvent: BackEvent) {
        set.findMostImportant()?.onBackProgressed(backEvent)
    }

    private fun onBackCancelled() {
        set.findMostImportant()?.onBackCancelled()
    }

    private fun onBack() {
        set.findMostImportant()?.onBack()
    }

    private fun Iterable<BackCallback>.findMostImportant(): BackCallback? =
        sortedBy(BackCallback::priority).lastOrNull(BackCallback::isEnabled)
}
