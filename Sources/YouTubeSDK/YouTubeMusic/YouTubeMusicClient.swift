//
//  YouTubeMusicClient.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 26/12/25.
//

import Foundation

public actor YouTubeMusicClient {
    
    private let network: NetworkClient
    
    /// Initializes the Client.
    /// - Parameter cookies: Optional "Cookie" header string. If provided, requests will be authenticated.
    public init(cookies: String? = nil) {
        // Pass the cookies into the Context
        let context = InnerTubeContext(client: ClientConfig.androidMusic, cookies: cookies)
        self.network = NetworkClient(context: context)
    }
    
    /// Searches for a song.
    /// - Returns: For now, we return EmptyJSONResponse just to see if the connection works.
    public func search(_ query: String) async throws {
        // The endpoint for music search
        let response: EmptyJSONResponse = try await network.send("/v1/search", body: ["query": query])
        print("✅ Connected to YouTube Music! Search successful.")
    }
}
