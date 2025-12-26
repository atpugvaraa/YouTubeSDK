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
        // Pass the cookies into the Context
        let context = InnerTubeContext(client: ClientConfig.ios, cookies: cookies)
        self.network = NetworkClient(context: context)
    }
    
    /// Fetches details for a specific video.
    /// - Parameter videoId: The 11-character YouTube ID (e.g., "dQw4w9WgXcQ")
    public func video(id: String) async throws -> YouTubeVideo {
        // We hit the 'player' endpoint.
        // This is the main endpoint that the real YouTube app uses when you tap a video.
        return try await network.send("/v1/player", body: ["videoId": id])
    }
}
