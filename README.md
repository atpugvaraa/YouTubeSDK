# YouTubeSDK 📺

### The missing YouTube SDK for modern iOS Development.

[![](https://img.shields.io/badge/Platform-iOS%20%7C%20macOS-blue.svg)]()
[![](https://img.shields.io/badge/Swift-6.2-orange.svg)]()
[![](https://img.shields.io/badge/SPM-Compatible-green.svg)]()

## 📦 Installation

Add `YouTubeSDK` to your project via Swift Package Manager.

```swift
dependencies: [
    .package(url: "https://github.com/atpugvaraa/YouTubeSDK.git", from: "1.0.0")
]
```

## 🛠️ Quick Start

### 1. Playing a Video (The Easy Way)

No need to manually manage `AVPlayer`. Use the drop-in view:

```swift
import SwiftUI
import YouTubeSDK

struct MyPlayerView: View {
    @StateObject var player = YouTubeAVPlayer()

    var body: some View {
        YouTubePlayerView(player: player)
            .onAppear {
                // One line to load, decipher, and play
                player.load(videoId: "dQw4w9WgXcQ")
            }
    }
}

```

### 2. Music Search

Access the YouTube Music backend for rich metadata.

```swift
import YouTubeSDK

let musicClient = YouTubeMusicClient()

func searchSong() async {
    do {
        let results = try await musicClient.search("Blinding Lights")
        if let song = results.first {
            print("Found: \(song.title) by \(song.artistsDisplay)")
            // Pass song.videoId to YouTubePlayer to play it!
        }
    } catch {
        print("Error: \(error)")
    }
}

```

### 3. Authentication (Optional)

Unlock personalized data like "Liked Songs."

```swift
// 1. Show Login View
.sheet(isPresented: $showLogin) {
    GoogleLoginView { cookies in
        // 2. Save Session
        YouTubeOAuthClient.saveCookies(cookies)
    }
}

// 3. Initialize Clients with Cookies
let musicClient = YouTubeMusicClient(cookies: YouTubeOAuthClient.loadCookies())
let likedSongs = try await musicClient.getLikedSongs()

```

---

## 🤝 Credits & Inspiration

I did not invent the wheel; I refined it. This SDK stands on the shoulders of open-source giants who reverse-engineered the InnerTube API. Huge respect to:

* **YouTube.js (LuanRT):** The gold standard for YouTube reverse engineering.
* **YouTubeKit (b5i):** For proving this was possible in Swift.
* **Kaset (sozercan):** For the robust manual parsing strategy for YouTube Music.
* **NewPipe & yt-dlp:** For the deciphering logic patterns.

My goal is not to compete, but to provide a polished, Swift-native alternative that focuses on **Developer Experience**.

---

## ⚠️ Disclaimer

**YouTubeSDK is an unofficial library.** It is not affiliated with, endorsed by, or associated with YouTube, Google, or Alphabet Inc.

* This project is for **educational and research purposes only**.
* It uses internal APIs that are subject to change at any time.
* You are responsible for ensuring your usage complies with YouTube's Terms of Service.
* "YouTube", "YouTube Music", and "YouTube Charts" are registered trademarks of Google LLC.

---

### License

MIT License. Built with ❤️ by [Aarav Gupta](https://github.com/atpugvaraa).
