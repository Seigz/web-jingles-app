package com.seigz.webjingles.player

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.random.Random

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    private val clickSoundId: Int
    var enabled: Boolean = true

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attrs)
            .build()

        clickSoundId = soundPool.load(context, com.seigz.webjingles.R.raw.sound, 1)
    }

    fun playClick() {
        if (!enabled) return
        val pitch = 0.85f + Random.nextFloat() * 0.30f // 0.85 – 1.15
        soundPool.play(clickSoundId, 0.5f, 0.5f, 1, 0, pitch)
    }

    fun release() {
        soundPool.release()
    }
}
