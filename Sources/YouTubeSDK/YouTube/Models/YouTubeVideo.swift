//
//  YouTubeVideo.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 26/12/25.
//

import Foundation

/// Represents a single YouTube Video.
public struct YouTubeVideo: Decodable, Identifiable, Sendable {
    public let id: String
    public let title: String
    public let viewCount: String
    public let author: String
    public let channelId: String
    
    // This maps the JSON keys to our Swift variable names
    enum CodingKeys: String, CodingKey {
        case videoDetails
    }
    
    // This maps the keys INSIDE the "videoDetails" object
    enum VideoDetailsKeys: String, CodingKey {
        case videoId
        case title
        case viewCount
        case author
        case channelId
    }
    
    public init(from decoder: Decoder) throws {
        // 1. Enter the root object
        let container = try decoder.container(keyedBy: CodingKeys.self)
        
        // 2. Enter the nested "videoDetails" object
        let details = try container.nestedContainer(keyedBy: VideoDetailsKeys.self, forKey: .videoDetails)
        
        // 3. Extract the values
        self.id = try details.decode(String.self, forKey: .videoId)
        self.title = try details.decode(String.self, forKey: .title)
        self.viewCount = try details.decode(String.self, forKey: .viewCount)
        self.author = try details.decode(String.self, forKey: .author)
        self.channelId = try details.decode(String.self, forKey: .channelId)
    }
}
