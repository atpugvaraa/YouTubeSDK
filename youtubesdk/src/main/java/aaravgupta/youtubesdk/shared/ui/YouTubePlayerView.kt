package aaravgupta.youtubesdk.shared.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.media3.ui.PlayerView
import aaravgupta.youtubesdk.youtubeAVPlayer.YouTubeAVPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

/**
 * Android View wrapper around YouTubeAVPlayer, mirroring the Swift YouTubePlayerView role.
 */
class YouTubePlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    enum class PlayerMode {
        VIDEO,
        AUDIO,
    }

    private var mode: PlayerMode = PlayerMode.VIDEO
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var isLoading = false
    private var loadingJob: Job? = null
    private var errorJob: Job? = null
    private var artworkJob: Job? = null

    private val playerView = PlayerView(context).apply {
        setBackgroundColor(Color.BLACK)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
    }

    private val artworkView = ImageView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT,
        )
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(Color.BLACK)
        imageAlpha = 190
        visibility = View.GONE
    }

    private val loadingView = ProgressBar(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        )
        visibility = View.GONE
    }

    private val errorView = TextView(context).apply {
        layoutParams = LayoutParams(
            LayoutParams.WRAP_CONTENT,
            LayoutParams.WRAP_CONTENT,
            Gravity.CENTER,
        )
        setTextColor(Color.WHITE)
        setBackgroundColor(0x66000000)
        setPadding(24, 12, 24, 12)
        visibility = View.GONE
    }

    init {
        setBackgroundColor(Color.BLACK)
        addView(playerView)
        addView(artworkView)
        addView(loadingView)
        addView(errorView)
    }

    fun bind(player: YouTubeAVPlayer, mode: PlayerMode = PlayerMode.VIDEO) {
        this.mode = mode
        playerView.player = player.player
        loadingJob?.cancel()
        errorJob?.cancel()
        artworkJob?.cancel()

        loadingJob = uiScope.launch {
            player.isLoading.collect { loading ->
                isLoading = loading
                playerView.useController = !loading
                loadingView.visibility = if (loading) View.VISIBLE else View.GONE
                updateArtworkVisibility()
            }
        }

        errorJob = uiScope.launch {
            player.playbackError.collect { message ->
                if (message.isNullOrBlank()) {
                    errorView.visibility = View.GONE
                    errorView.text = ""
                } else {
                    errorView.text = message
                    errorView.visibility = View.VISIBLE
                }
            }
        }

        artworkJob = uiScope.launch {
            player.currentVideo.collect { video ->
                val thumbnailUrl = video?.thumbnailUrl
                artworkView.setImageDrawable(null)

                if (!thumbnailUrl.isNullOrBlank()) {
                    val bitmap = withContext(Dispatchers.IO) {
                        runCatching {
                            URL(thumbnailUrl).openStream().use { input ->
                                BitmapFactory.decodeStream(input)
                            }
                        }.getOrNull()
                    }
                    artworkView.setImageBitmap(bitmap)
                }

                updateArtworkVisibility()
            }
        }

        updateArtworkVisibility()
    }

    private fun updateArtworkVisibility() {
        artworkView.visibility = if (mode == PlayerMode.AUDIO || isLoading) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        loadingJob?.cancel()
        errorJob?.cancel()
        artworkJob?.cancel()
        uiScope.cancel()
    }
}
