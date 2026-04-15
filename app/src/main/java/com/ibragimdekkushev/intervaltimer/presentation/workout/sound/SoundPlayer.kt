package com.ibragimdekkushev.intervaltimer.presentation.workout.sound

import android.media.AudioManager
import android.media.ToneGenerator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Singleton
class SoundPlayer @Inject constructor() {

    private val toneGenerator = runCatching {
        ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)
    }.getOrNull()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun playStart() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_MS)
    }

    fun playIntervalTransition() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_MS)
    }

    fun playFinish() {
        val tg = toneGenerator ?: return
        scope.launch {
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, FINISH_BEEP_MS)
            delay(FINISH_BEEP_MS + FINISH_GAP_MS)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP, FINISH_BEEP_MS)
        }
    }

    companion object {
        private const val VOLUME = 100
        private const val BEEP_MS = 200
        private const val FINISH_BEEP_MS = 120
        private const val FINISH_GAP_MS = 80L
    }
}
