package com.nernal.frame.hardware

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.nernal.frame.data.DeviceCapabilities

object DeviceCapabilitiesDetector {
    private val EXTERNAL_MIC_TYPES = setOf(
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    )

    private val SPEAKER_TYPES = setOf(
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
        AudioDeviceInfo.TYPE_USB_DEVICE,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    )

    fun detect(context: Context): DeviceCapabilities {
        val pm = context.packageManager

        val (hasCamera, hasFrontCamera, preferredCameraId) = detectCamera(context, pm)

        val audioManager = context.getSystemService(AudioManager::class.java)
        val inputDevices = runCatching {
            audioManager?.getDevices(AudioManager.GET_DEVICES_INPUTS).orEmpty()
        }.getOrDefault(emptyArray())
        val outputDevices = runCatching {
            audioManager?.getDevices(AudioManager.GET_DEVICES_OUTPUTS).orEmpty()
        }.getOrDefault(emptyArray())

        // Only trust a built-in mic when Android declares FEATURE_MICROPHONE (some Allwinner
        // picture frames expose a dummy TYPE_BUILTIN_MIC on an unwired codec pin).
        val hasMicrophone = pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE) ||
            inputDevices.any { it.type in EXTERNAL_MIC_TYPES }

        val hasAudioOutput = (pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT) &&
            outputDevices.any { it.type in SPEAKER_TYPES }) ||
            outputDevices.any { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }

        val hasOnDeviceSpeech = hasMicrophone && runCatching {
            SpeechRecognizer.isRecognitionAvailable(context)
        }.getOrDefault(false)

        val hasTextToSpeech = hasAudioOutput && runCatching {
            pm.queryIntentServices(Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE), 0).isNotEmpty()
        }.getOrDefault(false)

        return DeviceCapabilities(
            hasCamera = hasCamera,
            hasFrontCamera = hasFrontCamera,
            preferredCameraId = preferredCameraId,
            hasMicrophone = hasMicrophone,
            hasAudioOutput = hasAudioOutput,
            hasOnDeviceSpeech = hasOnDeviceSpeech,
            hasTextToSpeech = hasTextToSpeech,
        )
    }

    private fun detectCamera(context: Context, pm: PackageManager): Triple<Boolean, Boolean, String?> {
        val featureClaimsCamera = pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) ||
            pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) ||
            pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        val cm = context.getSystemService(CameraManager::class.java)
            ?: return Triple(false, false, null)
        val ids = runCatching { cm.cameraIdList }.getOrNull().orEmpty()
        if (ids.isEmpty()) return Triple(false, false, null)

        var frontId: String? = null
        for (id in ids) {
            val facing = runCatching {
                cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
            }.getOrNull()
            if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                frontId = id
                break
            }
        }
        val preferred = frontId ?: ids.firstOrNull()
        val hasAny = featureClaimsCamera || preferred != null
        return Triple(hasAny, frontId != null, preferred)
    }
}
