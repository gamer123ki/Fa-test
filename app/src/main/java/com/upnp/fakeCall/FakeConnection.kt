package com.upnp.fakeCall

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.telecom.CallAudioState
import android.telecom.Connection
import android.telecom.DisconnectCause
import android.telecom.PhoneAccount
import android.telecom.TelecomManager

class FakeConnection(
    private val context: Context,
    private val callerName: String,
    private val callerNumber: String
) : Connection() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager: AudioManager = context.getSystemService(AudioManager::class.java)

    init {
        val displayName = callerName.ifBlank { callerNumber }
        setAddress(Uri.fromParts(PhoneAccount.SCHEME_TEL, callerNumber, null), TelecomManager.PRESENTATION_ALLOWED)
        setCallerDisplayName(displayName, TelecomManager.PRESENTATION_ALLOWED)
        setInitializing()
        setRinging()
    }

    override fun onAnswer() {
        setActive()
        try {
            setAudioRoute(CallAudioState.ROUTE_EARPIECE)
        } catch (_: Throwable) {
            // Device or OEM may reject forced route changes.
        }
        startVoicePlayback()
    }

    override fun onReject() {
        disconnectWithCause(DisconnectCause.REJECTED)
    }

    override fun onDisconnect() {
        disconnectWithCause(DisconnectCause.LOCAL)
    }

    override fun onAbort() {
        disconnectWithCause(DisconnectCause.CANCELED)
    }

    private fun disconnectWithCause(code: Int) {
        stopAndReleasePlayer()
        setDisconnected(DisconnectCause(code))
        destroy()
    }

    private fun startVoicePlayback() {
        stopAndReleasePlayer()

        if (!requestAudioFocus()) {
            return
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        val selectedUri = loadSelectedAudioUri()
        val rawFallbackUri = Uri.parse("android.resource://${context.packageName}/${R.raw.fake_voice}")

        val started = runCatching {
            val player = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                @Suppress("DEPRECATION")
                setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
                if (selectedUri != null) {
                    setDataSource(context, selectedUri)
                } else {
                    setDataSource(context, rawFallbackUri)
                }
                setOnCompletionListener {
                    stopAndReleasePlayer()
                }
                setOnErrorListener { _, _, _ ->
                    stopAndReleasePlayer()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            true
        }.getOrElse { false }

        if (!started && selectedUri != null) {
            runCatching {
                val player = MediaPlayer().apply {
                    setAudioAttributes(audioAttributes)
                    @Suppress("DEPRECATION")
                    setAudioStreamType(AudioManager.STREAM_VOICE_CALL)
                    setDataSource(context, rawFallbackUri)
                    setOnCompletionListener {
                        stopAndReleasePlayer()
                    }
                    setOnErrorListener { _, _, _ ->
                        stopAndReleasePlayer()
                        true
                    }
                    prepare()
                    start()
                }
                mediaPlayer = player
            }.onFailure {
                stopAndReleasePlayer()
            }
        } else if (!started) {
            stopAndReleasePlayer()
        }
    }

    private fun loadSelectedAudioUri(): Uri? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_AUDIO_URI, "").orEmpty()
        if (value.isBlank()) return null
        return runCatching { Uri.parse(value) }.getOrNull()
    }

    private fun requestAudioFocus(): Boolean {
        val focusResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setOnAudioFocusChangeListener { }
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }

        return focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }

    private fun stopAndReleasePlayer() {
        mediaPlayer?.run {
            runCatching {
                if (isPlaying) {
                    stop()
                }
            }
            reset()
            release()
        }
        mediaPlayer = null
        abandonAudioFocus()
    }

    companion object {
        private const val PREFS_NAME = "fake_call_prefs"
        private const val KEY_AUDIO_URI = "audio_uri"
    }
}
