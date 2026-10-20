package com.taleon.reader

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Bridges Activity-level volume key events to the reader. Volume keys are only consumed while a
 * reader screen has explicitly marked itself active, so the rest of the app keeps normal behavior.
 */
object ReaderHardwareKeys {
    private val _volumeKeys = MutableSharedFlow<Int>(extraBufferCapacity = 2)
    val volumeKeys: SharedFlow<Int> = _volumeKeys

    @Volatile
    var isReaderActive: Boolean = false

    fun onVolumeKey(keyCode: Int): Boolean {
        if (!isReaderActive) return false
        _volumeKeys.tryEmit(keyCode)
        return true
    }
}
