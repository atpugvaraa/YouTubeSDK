//
//  YouTubeClient+Parsing.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 13/03/26.
//

import Foundation

extension YouTubeClient {

    // MARK: - Continuation

    /// Fetches the next page of results using a continuation token.
    /// - Parameter token: The token retrieved from a previous result.
    public func fetchContinuation(token: String) async throws -> YouTubeContinuation<YouTubeItem> {
        let body = ["continuation": token]
        let data = try await network.get("browse", body: body)
        return await parseContinuationResults(from: data)
    }

    // MARK: - Helpers
    func findContinuationToken(in container: Any) -> String? {
        if let dict = container as? [String: Any] {
            if let token = dict["continuation"] as? String { return token }
            if let continuationData = (dict["continuationEndpoint"] as? [String: Any])?["continuationCommand"] as? [String: Any] {
                return continuationData["token"] as? String
            }
            for value in dict.values {
                if let found = findContinuationToken(in: value) { return found }
            }
        } else if let array = container as? [Any] {
            for element in array {
                if let found = findContinuationToken(in: element) { return found }
            }
        }
        return nil
    }
    
    func findAll(key: String, in container: Any) -> [Any] {
        var results: [Any] = []
        if let dict = container as? [String: Any] {
            if let found = dict[key] { results.append(found) }
            for value in dict.values { results.append(contentsOf: findAll(key: key, in: value)) }
        } else if let array = container as? [Any] {
            for element in array { results.append(contentsOf: findAll(key: key, in: element)) }
        }
        return results
    }

    // MARK: - Generic Video Parser (For Channel/Playlist/Home)
    func parseVideos(from data: Data) -> [YouTubeVideo] {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        
        let keys = YouTubeSDKConstants.InternalKeys.Renderers.self
        let videos = findAll(key: keys.video, in: json) + findAll(key: keys.gridVideo, in: json)
        
        return videos.compactMap { item in
            guard let dict = item as? [String: Any] else { return nil }
            return YouTubeVideo(from: dict)
        }
    }
    
    func parseContinuationResults(from data: Data) -> YouTubeContinuation<YouTubeItem> {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { 
            return YouTubeContinuation(items: [], continuationToken: nil)
        }
        
        var results: [YouTubeItem] = []
        let keys = YouTubeSDKConstants.InternalKeys.Renderers.self
        
        let videos = findAll(key: keys.video, in: json) + findAll(key: keys.gridVideo, in: json)
        for item in videos {
            if let dict = item as? [String: Any], let video = YouTubeVideo(from: dict) {
                results.append(.video(video))
            }
        }
        
        let channels = findAll(key: keys.channel, in: json)
        for item in channels {
            if let dict = item as? [String: Any], let channel = YouTubeChannel(from: dict) {
                results.append(.channel(channel))
            }
        }
        
        let playlists = findAll(key: keys.playlist, in: json)
        for item in playlists {
            if let dict = item as? [String: Any], let playlist = YouTubePlaylist(from: dict) {
                results.append(.playlist(playlist))
            }
        }
        
        let songs = findAll(key: keys.musicResponsiveListItem, in: json)
        for item in songs {
            if let dict = item as? [String: Any], let song = YouTubeMusicSong(from: dict) {
                results.append(.song(song))
            }
        }
        
        let shelves = findAll(key: keys.musicShelf, in: json) + findAll(key: keys.musicCarouselShelf, in: json)
        for item in shelves {
            if let dict = item as? [String: Any], let title = (dict["title"] as? [String: Any])?["simpleText"] as? String {
                results.append(.shelf(YouTubeShelf(title: title, items: []))) 
            }
        }
        
        let token = findContinuationToken(in: json)
        return YouTubeContinuation(items: results, continuationToken: token)
    }
}
