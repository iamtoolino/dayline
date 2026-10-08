package io.github.iamtoolino.dayline

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator

/** Reminders use notification policy, even when another app is in the foreground. */
object ReminderVibration {
    fun play(context: Context, pulse: ReminderPolicy.Pulse): Boolean {
        if (pulse == ReminderPolicy.Pulse.NONE) return false
        val vibrator = context.getSystemService(Vibrator::class.java)
        if (!vibrator.hasVibrator()) return false
        val effect = if (pulse == ReminderPolicy.Pulse.TICK)
            VibrationEffect.createOneShot(35, if (vibrator.hasAmplitudeControl()) 80 else VibrationEffect.DEFAULT_AMPLITUDE)
        else VibrationEffect.createOneShot(160, VibrationEffect.DEFAULT_AMPLITUDE)
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_NOTIFICATION))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }
        // Android does not report whether user policy suppressed the requested vibration.
        return true
    }
}
