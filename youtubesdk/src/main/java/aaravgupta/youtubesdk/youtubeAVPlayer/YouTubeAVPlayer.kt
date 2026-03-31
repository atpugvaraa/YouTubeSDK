package aaravgupta.youtubesdk.youtubeAVPlayer

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import aaravgupta.youtubesdk.youtube.YouTubeClient
import aaravgupta.youtubesdk.youtube.models.YouTubeVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Android equivalent of the Swift YouTubeAVPlayer helper.
 */
class YouTubeAVPlayer(
    context: Context,
    private val client: YouTubeClient = YouTubeClient(),
    externalScope: CoroutineScope? = null,
) {
    private val ownsScope = externalScope == null
    private val scope: CoroutineScope = externalScope
        ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .build()
        .apply {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build()
            setAudioAttributes(audioAttributes, true)
        }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentVideo = MutableStateFlow<YouTubeVideo?>(null)
    val currentVideo: StateFlow<YouTubeVideo?> = _currentVideo.asStateFlow()

    private val _playbackError = MutableStateFlow<String?>(null)
    val playbackError: StateFlow<String?> = _playbackError.asStateFlow()

    fun load(videoId: String, preferAudio: Boolean = false) {
        _isLoading.value = true
        _playbackError.value = null

        scope.launch {
            try {
                val video = withContext(Dispatchers.IO) {
                    client.video(videoId)
                }
                _currentVideo.value = video

                val streamUrl = selectBestStream(video = video, preferAudio = preferAudio)
                    ?: throw IllegalStateException("No playable stream URL found for videoId=$videoId")

                player.setMediaItem(MediaItem.fromUri(streamUrl))
                player.prepare()
                player.playWhenReady = true
                _isLoading.value = false
            } catch (error: Throwable) {
                _playbackError.value = error.message ?: "Unknown playback error"
                _isLoading.value = false
            }
        }
    }

    fun release() {
        player.release()
        if (ownsScope) {
            scope.cancel()
        }
    }

    private fun selectBestStream(video: YouTubeVideo, preferAudio: Boolean): String? {
        if (!preferAudio && !video.hlsUrl.isNullOrBlank()) {
            return video.hlsUrl
        }

        if (preferAudio) {
            val audioUrl = video.bestAudioStream?.url
            if (!audioUrl.isNullOrBlank()) {
                return audioUrl
            }
        }

        val muxedUrl = video.bestMuxedStream?.url
        if (!muxedUrl.isNullOrBlank()) {
            return muxedUrl
        }

        return null
    }
}
