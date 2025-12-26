//
//  YouTubeMusicSong.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 26/12/25.
//

import Foundation
import Core

public struct YouTubeMusicSong: Decodable, Identifiable, Sendable {
    public let id: String
    public let title: String
    public let artist: String
    public let album: String
    public let duration: String
    
    // Coding keys to navigate the messy JSON
    // Note: This is a simplified parser. Real Music JSON is deeply nested.
    // We will start with a basic search parser for the next step.
    enum CodingKeys: String, CodingKey {
        case musicResponsiveListItemRenderer
    }
    
    enum MusicResponsiveListItemRendererKeys: String, CodingKey {
        case videoId
        case title
        case artist
        case album
        case duration
    }
    
    // ... We will fill in the init logic once we see the real JSON response ...
    public init(from decoder: Decoder) throws {
        // 1. Enter the root object
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // 2. Enter the nested "videoDetails" object

        let details = try container.nestedContainer(keyedBy: MusicResponsiveListItemRendererKeys.self, forKey: .musicResponsiveListItemRenderer)
        
        // 3. Extract the values
        self.id = try details.decode(String.self, forKey: .videoId)
        self.title = try details.decode(String.self, forKey: .title)
        self.artist = try details.decode(String.self, forKey: .artist)
        self.album = try details.decode(String.self, forKey: .album)
        self.duration = try details.decode(String.self, forKey: .duration)
    }
}
