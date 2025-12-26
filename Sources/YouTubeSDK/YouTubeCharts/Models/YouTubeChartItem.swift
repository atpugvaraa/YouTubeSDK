//
//  YouTubeChartItem.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 31/12/25.
//

import Foundation

public struct YouTubeChartItem: Identifiable, Sendable {
    public let id: String
    public let title: String
    public let subtitle: String // Artist or Channel Name
    public let rank: String
    public let thumbnailURL: URL?
    public let type: ChartItemType
    
    // Metadata
    public let viewCount: String?
    public let change: String? // e.g., "NEW", "+1", "-3"
    
    public enum ChartItemType: String, Sendable {
        case song
        case video
        case artist
    }
    
    /// Robust Initializer for Charts
    init?(from data: [String: Any], type: ChartItemType) {
        // 1. ID Extraction
        if let videoId = data["videoId"] as? String {
            self.id = videoId
        } else if let channelId = data["browseId"] as? String {
            self.id = channelId
        } else if let nav = data["navigationEndpoint"] as? [String: Any],
                  let watch = nav["watchEndpoint"] as? [String: Any],
                  let vid = watch["videoId"] as? String {
            self.id = vid
        } else {
            return nil
        }
        
        self.type = type
        
        // 2. Title
        if let titleData = data["title"] as? [String: Any],
           let runs = titleData["runs"] as? [[String: Any]],
           let text = runs.first?["text"] as? String {
            self.title = text
        } else if let simple = (data["title"] as? [String: Any])?["simpleText"] as? String {
            self.title = simple
        } else {
            self.title = "Unknown"
        }
        
        // 3. Subtitle
        var foundSubtitle = ""
        if let subtitleData = data["subtitle"] as? [String: Any],
           let runs = subtitleData["runs"] as? [[String: Any]] {
            foundSubtitle = runs.compactMap { $0["text"] as? String }.joined()
        } else if let flex = data["flexColumns"] as? [[String: Any]], flex.count > 1,
                  let textData = (flex[1]["musicResponsiveListItemFlexColumnRenderer"] as? [String: Any])?["text"] as? [String: Any],
                  let runs = textData["runs"] as? [[String: Any]] {
            foundSubtitle = runs.compactMap { $0["text"] as? String }.joined()
        }
        self.subtitle = foundSubtitle
        
        // 4. Rank
        if let indexCol = (data["customIndexColumn"] as? [String: Any])?["musicCustomIndexColumnRenderer"] as? [String: Any],
           let textData = indexCol["text"] as? [String: Any],
           let runs = textData["runs"] as? [[String: Any]],
           let rankText = runs.first?["text"] as? String {
            self.rank = rankText
        } else {
            self.rank = "0"
        }
        
        // 5. Thumbnail (FIXED)
        // We separate the logic because 'image' is not optional here, breaking the 'if let' chain
        var foundURL: URL? = nil
        if let thumbContainer = (data["thumbnail"] ?? data["musicThumbnailRenderer"]) as? [String: Any] {
            // If inner keys are missing, fallback to the container itself
            let image = (thumbContainer["thumbnail"] ?? thumbContainer["thumbnails"]) as? [String: Any] ?? thumbContainer
            
            if let thumbs = image["thumbnails"] as? [[String: Any]],
               let urlString = thumbs.last?["url"] as? String {
                foundURL = URL(string: urlString)
            }
        }
        self.thumbnailURL = foundURL
        
        // 6. View Count
        self.viewCount = (data["viewCountText"] as? [String: Any])?["simpleText"] as? String
        
        // 7. Change
        self.change = nil
    }
}
