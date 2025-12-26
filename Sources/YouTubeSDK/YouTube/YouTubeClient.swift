//
//  YouTubeClient.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 26/12/25.
//

import Foundation

public actor YouTubeClient {
    private let network: NetworkClient
    
    /// Initializes the Client.
    /// - Parameter cookies: Optional "Cookie" header string. If provided, requests will be authenticated.
    public init(cookies: String? = nil) {
        let context = InnerTubeContext(client: ClientConfig.ios, cookies: cookies)
        self.network = NetworkClient(context: context)
    }
    
    // MARK: - Player (Essential for Streaming)
        
    /// Fetches the full video details, including Streaming URLs (HLS).
    /// Use this to play a video or song.
    public func video(id: String) async throws -> YouTubeVideo {
        // 1. Fetch & Decode
        let data = try await network.get("player", body: ["videoId": id])
        let decoder = JSONDecoder()
        var video = try decoder.decode(YouTubeVideo.self, from: data)
        
        // 2. Check if we need to hack the mainframe
        if video.requiresDeciphering {
            if var streamingData = video.streamingData {
                var newFormats: [Stream] = []
                
                // We only loop through adaptiveFormats because that's where the high-quality audio lives
                for var stream in streamingData.adaptiveFormats {
                    
                    // Only decipher if it HAS a cipher
                    if let cipher = stream.signatureCipher {
                        do {
                            // We pass "" as the base URL because the cipher string usually contains the url itself
                            let decryptedURL = try await Cipher.shared.decipher(url: stream.url ?? "", signatureCipher: cipher, network: network)
                            
                            // 4. Update the Stream
                            stream.url = decryptedURL.absoluteString
                            stream.signatureCipher = nil
                            print("✅ Deciphered Stream: \(stream.mimeType)")
                        } catch {
                            print("⚠️ Failed to decipher stream: \(error)")
                        }
                    }
                    
                    newFormats.append(stream)
                }
                
                // 5. Save back to video
                streamingData.adaptiveFormats = newFormats
                video.streamingData = streamingData
            }
        }
        
        return video
    }
    
    // MARK: - Search
    
    public func search(_ query: String) async throws -> [YouTubeSearchResult] {
        let data = try await network.get("search", body: ["query": query])
        return parseSearchResults(from: data)
    }
    
    // MARK: - Browsing (Home/Trending)
    
    public func getHome() async throws -> [YouTubeVideo] {
        // "FEwhat_to_watch" is the internal ID for the Home Feed
        let data = try await network.get("browse", body: ["browseId": "FEwhat_to_watch"])
        return parseVideos(from: data)
    }
    
    public func getTrending() async throws -> [YouTubeVideo] {
        let data = try await network.get("browse", body: ["browseId": "FEtrending"])
        return parseVideos(from: data)
    }
    
    // MARK: - Channel Details
    
    public func getChannelVideos(channelId: String) async throws -> [YouTubeVideo] {
        let data = try await network.get("browse", body: ["browseId": channelId, "params": "EgZ2aWRlb3M%3D"]) // "Videos" tab param
        return parseVideos(from: data)
    }
    
    // MARK: - Playlist Details
    
    public func getPlaylist(id: String) async throws -> [YouTubeVideo] {
        let browseId = id.hasPrefix("PL") ? "VL\(id)" : id
        let data = try await network.get("browse", body: ["browseId": browseId])
        
        // Playlists use "playlistVideoRenderer"
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        let items = findAll(key: "playlistVideoRenderer", in: json)
        
        return items.compactMap { item in
            guard let dict = item as? [String: Any] else { return nil }
            return YouTubeVideo(from: dict)
        }
    }
}

extension YouTubeClient {
    
    // MARK: - Search Parser
    func parseSearchResults(from data: Data) -> [YouTubeSearchResult] {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        var results: [YouTubeSearchResult] = []
        
        // 1. Videos
        let videos = findAll(key: "videoRenderer", in: json)
        for item in videos {
            if let dict = item as? [String: Any], let video = YouTubeVideo(from: dict) {
                results.append(.video(video))
            }
        }
        
        // 2. Channels
        let channels = findAll(key: "channelRenderer", in: json)
        for item in channels {
            if let dict = item as? [String: Any], let channel = YouTubeChannel(from: dict) {
                results.append(.channel(channel))
            }
        }
        
        // 3. Playlists
        let playlists = findAll(key: "playlistRenderer", in: json)
        for item in playlists {
            if let dict = item as? [String: Any], let playlist = YouTubePlaylist(from: dict) {
                results.append(.playlist(playlist))
            }
        }
        
        return results
    }
    
    // MARK: - Generic Video Parser (For Channel/Playlist/Home)
    func parseVideos(from data: Data) -> [YouTubeVideo] {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        
        // YouTube uses gridVideoRenderer for channels, videoRenderer for lists
        let videos = findAll(key: "videoRenderer", in: json) + findAll(key: "gridVideoRenderer", in: json)
        
        return videos.compactMap { item in
            guard let dict = item as? [String: Any] else { return nil }
            return YouTubeVideo(from: dict)
        }
    }
    
    // MARK: - Helper
    private func findAll(key: String, in container: Any) -> [Any] {
        var results: [Any] = []
        if let dict = container as? [String: Any] {
            if let found = dict[key] { results.append(found) }
            for value in dict.values { results.append(contentsOf: findAll(key: key, in: value)) }
        } else if let array = container as? [Any] {
            for element in array { results.append(contentsOf: findAll(key: key, in: element)) }
        }
        return results
    }
}

extension YouTubeClient {
    // MARK: - Interactions
    
    /// Likes a video.
    public func like(videoId: String) async throws {
        let body: [String: Any] = [
            "target": ["videoId": videoId]
        ]
        let _ = try await network.sendComplexRequest("like/like", body: body)
    }
    
    /// Removes a like from a video.
    public func removeLike(videoId: String) async throws {
        let body: [String: Any] = [
            "target": ["videoId": videoId]
        ]
        let _ = try await network.sendComplexRequest("like/removelike", body: body)
    }
    
    /// Dislikes a video.
    public func dislike(videoId: String) async throws {
        let body: [String: Any] = [
            "target": ["videoId": videoId]
        ]
        let _ = try await network.sendComplexRequest("like/dislike", body: body)
    }
    
    /// Subscribes to a channel.
    public func subscribe(channelId: String) async throws {
        let body: [String: Any] = [
            "channelIds": [channelId]
        ]
        let _ = try await network.sendComplexRequest("subscription/subscribe", body: body)
    }
    
    /// Unsubscribes from a channel.
    public func unsubscribe(channelId: String) async throws {
        let body: [String: Any] = [
            "channelIds": [channelId]
        ]
        let _ = try await network.sendComplexRequest("subscription/unsubscribe", body: body)
    }
}
