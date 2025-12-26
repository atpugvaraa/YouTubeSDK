//
//  YouTubeMusicClient.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 26/12/25.
//

import Foundation

public actor YouTubeMusicClient {
    
    private let network: NetworkClient
    
    // MARK: - Constants (From Kaset API Discovery)
    private enum BrowseID {
        static let home = "FEmusic_home"
        static let explore = "FEmusic_explore"
        static let charts = "FEmusic_charts"
        static let newReleases = "FEmusic_new_releases"
        static let moods = "FEmusic_moods_and_genres"
        
        /// user type shit
        static let userLikedVideos = "FEmusic_liked_videos"
        static let userHistory = "FEmusic_history"
        static let userLibrary = "FEmusic_library"
    }
    
    public init(cookies: String? = nil) {
        let context = InnerTubeContext(client: ClientConfig.webRemix, cookies: cookies)
        self.network = NetworkClient(context: context, baseURL: YouTubeSDKConstants.URLS.API.youtubeMusicInnerTubeURL)
    }
    
    // MARK: - Home & Discovery
    
    public func getHome() async throws -> [YouTubeMusicSection] {
        return try await browseSection(browseId: BrowseID.home)
    }
    
    public func getCharts() async throws -> [YouTubeMusicSection] {
        return try await browseSection(browseId: BrowseID.charts)
    }
    
    public func getNewReleases() async throws -> [YouTubeMusicSection] {
        return try await browseSection(browseId: BrowseID.newReleases)
    }
    
    public func getMoods() async throws -> [YouTubeMusicSection] {
        return try await browseSection(browseId: BrowseID.moods)
    }
    
    // MARK: - Details (Artist, Album, Playlist)
    
    /// Fetches full Artist details (Songs, Albums, Singles)
    public func getArtist(browseId: String) async throws -> YouTubeMusicArtistDetail {
        let body = ["browseId": browseId]
        let data = try await network.get("browse", body: body)
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            throw URLError(.cannotParseResponse)
        }
        // Use the robust recursive finder to get all shelves
        let sections = parseSections(from: json)
        return YouTubeMusicArtistDetail(id: browseId, sections: sections)
    }
    
    /// Fetches Album details (Tracks)
    public func getAlbum(browseId: String) async throws -> [YouTubeMusicSong] {
        let body = ["browseId": browseId]
        let data = try await network.get("browse", body: body)
        return parseMusicItems(from: data)
    }
    
    /// Fetches Playlist details (Tracks)
    public func getPlaylist(browseId: String) async throws -> [YouTubeMusicSong] {
        // Note: Playlists often need the 'browse' endpoint with 'VL' prefix if passed 'PL...'
        let browseId = browseId.hasPrefix("PL") ? "VL\(browseId)" : browseId
        
        let body = ["browseId": browseId]
        let data = try await network.get("browse", body: body)
        return parseMusicItems(from: data)
    }
    
    // MARK: - Actions
    
    public func getLyrics(videoId: String) async throws -> String? {
        // 1. Get 'Next' endpoint to find the Lyrics browseId
        let nextData = try await network.get("next", body: ["videoId": videoId])
        guard let json = try? JSONSerialization.jsonObject(with: nextData) as? [String: Any] else { return nil }
        
        // Find the "Lyrics" tab
        let tabs = findAll(key: "tabRenderer", in: json)
        guard let lyricsTab = tabs.first(where: { ($0 as? [String: Any])?["title"] as? String == "Lyrics" }) as? [String: Any],
              let endpoint = lyricsTab["endpoint"] as? [String: Any],
              let browseId = (endpoint["browseEndpoint"] as? [String: Any])?["browseId"] as? String else {
            return nil
        }
        
        // 2. Browse the Lyrics ID
        let body = ["browseId": browseId]
        let lyricsData = try await network.get("browse", body: body)
        guard let lyricsJson = try? JSONSerialization.jsonObject(with: lyricsData) as? [String: Any] else { return nil }
        
        // 3. Extract text
        // Usually in musicDescriptionShelfRenderer -> description -> runs -> text
        let descriptions = findAll(key: "musicDescriptionShelfRenderer", in: lyricsJson)
        if let shelf = descriptions.first as? [String: Any],
           let desc = shelf["description"] as? [String: Any],
           let runs = desc["runs"] as? [[String: Any]] {
            return runs.compactMap { $0["text"] as? String }.joined()
        }
        
        return nil
    }
    
    // MARK: - Search
    
    public func search(_ query: String) async throws -> [YouTubeMusicSong] {
        let data = try await network.get("search", body: ["query": query])
        return parseMusicItems(from: data)
    }
    
    public func getSearchSuggestions(_ query: String) async throws -> [String] {
        let data = try await network.get("music/get_search_suggestions", body: ["input": query])
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        
        // Look for 'searchSuggestionRenderer'
        let suggestions = findAll(key: "searchSuggestionRenderer", in: json)
        return suggestions.compactMap { item in
            let dict = item as? [String: Any]
            let runs = (dict?["suggestion"] as? [String: Any])?["runs"] as? [[String: Any]]
            return runs?.compactMap { $0["text"] as? String }.joined()
        }
    }
    
    // MARK: - User Library
        
        /// Fetches the user's "Liked Music" playlist.
        public func getLikedSongs() async throws -> [YouTubeMusicSong] {
            // "LM" is the internal ID for Liked Music (or "FEmusic_liked_videos")
            let data = try await network.get("browse", body: ["browseId": "FEmusic_liked_videos"])
            return parseMusicItems(from: data)
        }
        
        /// Fetches the user's Listen History.
        public func getHistory() async throws -> [YouTubeMusicSong] {
            let data = try await network.get("browse", body: ["browseId": "FEmusic_history"])
            return parseMusicItems(from: data)
        }
        
        /// Fetches the user's Library (Playlists, Albums, etc.)
        /// Note: This returns a Section list, not just songs.
    public func getLibrary() async throws -> [YouTubeMusicSection] {
        let data = try await network.get("browse", body: ["browseId": "FEmusic_library_landing"])
        return parseSections(from: try JSONSerialization.jsonObject(with: data) as? [String: Any] ?? [:])
    }

    // MARK: - Helpers
    
    private func browseSection(browseId: String) async throws -> [YouTubeMusicSection] {
        let body = ["browseId": browseId]
        let data = try await network.get("browse", body: body)
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        return parseSections(from: json)
    }
    
    private func parseMusicItems(from data: Data) -> [YouTubeMusicSong] {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        let items = findAll(key: "musicResponsiveListItemRenderer", in: json)
        return items.compactMap { ($0 as? [String: Any]).flatMap { YouTubeMusicSong(from: $0) } }
    }
    
    private func parseSections(from json: [String: Any]) -> [YouTubeMusicSection] {
        // Look for "musicCarouselShelfRenderer" or "musicShelfRenderer"
        var sections: [YouTubeMusicSection] = []
        
        // 1. Carousels (Horizontal)
        let carousels = findAll(key: "musicCarouselShelfRenderer", in: json)
        for carousel in carousels {
            if let dict = carousel as? [String: Any],
               let section = YouTubeMusicSection(from: dict) {
                sections.append(section)
            }
        }
        
        // 2. Shelves (Vertical)
        let shelves = findAll(key: "musicShelfRenderer", in: json)
        for shelf in shelves {
            if let dict = shelf as? [String: Any],
               let section = YouTubeMusicSection(from: dict) {
                sections.append(section)
            }
        }
        
        return sections
    }
    
    /// Recursively finds all dictionaries with a specific key.
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

extension YouTubeMusicClient {
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
