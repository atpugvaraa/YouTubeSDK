//
//  YouTubeMusicClient+Discovery.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 13/03/26.
//

import Foundation

extension YouTubeMusicClient {
    
    public func getHome() async throws -> [YouTubeMusicSection] {
        return try await browseSection(browseId: YouTubeSDKConstants.InternalKeys.BrowseIDs.Music.home)
    }
    
    public func getCharts() async throws -> [YouTubeMusicSection] {
        return try await browseSection(browseId: YouTubeSDKConstants.InternalKeys.BrowseIDs.Music.charts)
    }
    
    public func getNewReleases() async throws -> [YouTubeMusicSection] {
        return try await browseSection(browseId: YouTubeSDKConstants.InternalKeys.BrowseIDs.Music.newReleases)
    }
    
    public func getMoods() async throws -> [YouTubeMusicSection] {
        return try await browseSection(browseId: YouTubeSDKConstants.InternalKeys.BrowseIDs.Music.moods)
    }
    
    // MARK: - User Library
        
    public func getLikedSongs() async throws -> [YouTubeMusicSong] {
        let data = try await network.get("browse", body: ["browseId": YouTubeSDKConstants.InternalKeys.BrowseIDs.Music.likedVideos])
        return await parseMusicItems(from: data)
    }
    
    public func getHistory() async throws -> [YouTubeMusicSong] {
        let data = try await network.get("browse", body: ["browseId": YouTubeSDKConstants.InternalKeys.BrowseIDs.Music.history])
        return await parseMusicItems(from: data)
    }
    
    public func getLibrary() async throws -> [YouTubeMusicSection] {
        let data = try await network.get("browse", body: ["browseId": YouTubeSDKConstants.InternalKeys.BrowseIDs.Music.library])
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        return await parseSections(from: json)
    }
    
    private func browseSection(browseId: String) async throws -> [YouTubeMusicSection] {
        let body = ["browseId": browseId]
        let data = try await network.get("browse", body: body)
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        return await parseSections(from: json)
    }
}
