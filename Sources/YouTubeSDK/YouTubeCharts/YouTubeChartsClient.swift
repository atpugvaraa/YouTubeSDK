//
//  YouTubeChartsClient.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 26/12/25.
//

import Foundation

public actor YouTubeChartsClient {
    
    private let network: NetworkClient
    
    public init() {
        // Charts uses the WEB Music Analytics client, but talks to "charts.youtube.com"
        let context = InnerTubeContext(client: ClientConfig.webMusicAnalytics)
        
        // We inject the special Charts URL here!
        self.network = NetworkClient(
            context: context,
            baseURL: YouTubeSDKConstants.URLS.API.youtubeChartsInnerTubeURL
        )
    }
    
    /// Fetches the Top Songs chart for a specific country.
    /// - Parameter countryCode: The ISO 3166-1 alpha-2 code (e.g., "US", "IN", "ZZ" for Global).
    public func topSongs(countryCode: String = "ZZ") async throws {
        // Based on your URL: https://charts.youtube.com/youtubei/v1/browse
        // Charts are retrieved via the "browse" endpoint with specific params.
        // For now, we just test the connection.
        
        let body: [String: Any] = [
            "browseId": "FEmusic_analytics_charts_home", // The standard ID for the charts page
            "query": "perspective=CHART_HOME&chart_params_country_code=global"
        ]
        
        // We use a dummy response just to verify connectivity
        let _: EmptyJSONResponse = try await network.send("/v1/browse", body: body)
        print("✅ Connected to YouTube Charts for \(countryCode)!")
    }
}

// Temporary helper until we share it or make it public in Core
struct EmptyJSONResponse: Decodable {}
