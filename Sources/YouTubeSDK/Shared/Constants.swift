//
//  Constants.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 26/12/25.
//

import Foundation

public struct YouTubeSDKConstants {
    public struct URLS {
        public static let googleSearchBaseURL = "https://www.google.com"
        public static let youtubeBaseURL = "https://www.youtube.com"
        public static let youtubeMusicBaseURL = "https://music.youtube.com"
        public static let youtubeSuggestions = "https://suggestqueries-clients6.youtube.com"
        public static let youtubeUpload = "https://upload.youtube.com"
        
        public struct API {
            /// Generic Base URL
            public static let baseURL = "https://youtubei.googleapis.com"
            
            /// Generic Production URLs
            public static let youtubeInnerTubeURL = "https://www.youtube.com/youtubei"
            public static let youtubeMusicInnerTubeURL = "https://music.youtube.com/youtubei"
            public static let youtubeChartsInnerTubeURL = "https://charts.youtube.com/youtubei"
            
            /// Random InnerTube API URLs
            public static let googleapisInnerTubeURL = "https://youtubei.googleapis.com/youtubei"
            public static let stagingURL = "https://green-youtubei.sandbox.googleapis.com/youtubei"
            public static let releaseURL = "https://release-youtubei.sandbox.googleapis.com/youtubei"
            public static let testURL = "https://test-youtubei.sandbox.googleapis.com/youtubei"
            public static let camiURL = "https://cami-youtubei.sandbox.googleapis.com/youtubei"
            public static let uytfeURL = "https://uytfe.sandbox.googleapis.com/youtubei"
        }
    }
}
