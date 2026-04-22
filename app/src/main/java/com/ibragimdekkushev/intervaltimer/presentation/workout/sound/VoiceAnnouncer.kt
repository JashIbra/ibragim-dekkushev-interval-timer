package com.ibragimdekkushev.intervaltimer.presentation.workout.sound

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceAnnouncer @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    private val isReady = AtomicBoolean(false)
    private var pendingText: String? = null

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            applyLocale()
            isReady.set(true)
            pendingText?.let { speak(it) }
            pendingText = null
        }
    }

    fun announce(text: String) {
        if (text.isBlank()) return
        if (!isReady.get()) {
            pendingText = text
            return
        }
        speak(text)
    }

    fun stop() {
        runCatching { tts.stop() }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun applyLocale() {
        val russian = Locale("ru", "RU")
        val result = tts.setLanguage(russian)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts.language = Locale.getDefault()
        }
    }

    companion object {
        private const val UTTERANCE_ID = "interval_timer_announcement"
    }
}
