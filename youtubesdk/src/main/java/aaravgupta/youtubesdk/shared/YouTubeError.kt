package aaravgupta.youtubesdk.shared

sealed class YouTubeError(
    override val message: String,
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    class NetworkError(error: Throwable) :
        YouTubeError(message = "Network Error: ${error.localizedMessage ?: error.message ?: "Unknown"}", cause = error)

    class ApiError(details: String) : YouTubeError(message = "API Error: $details")

    class ParsingError(details: String) : YouTubeError(message = "Parsing Error: $details")

    class DecipheringFailed(videoId: String) :
        YouTubeError(message = "Deciphering Failed for video ID: $videoId")

    data object AuthenticationRequired :
        YouTubeError(message = "Authentication Required: Please sign in to perform this action.")

    data object Unknown : YouTubeError(message = "An unknown error occurred.")
}
