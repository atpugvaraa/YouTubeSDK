//
//  NetworkClient.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 26/12/25.
//

import Foundation

/// The simplified networking engine.
/// It takes the Context we built and sends it to the URLs we defined.
public actor NetworkClient {
    
    private let context: InnerTubeContext
    private let session: URLSession
    private let baseURL: String
    
    /// - Parameters:
    /// - baseURL: The host URL (e.g., "https://music.youtube.com/youtubei")
    /// - easier usage: `YouTubeSDKConstants.URLS.API.<api you want to use>`
    public init(context: InnerTubeContext, session: URLSession = .shared, baseURL: String = YouTubeSDKConstants.URLS.API.youtubeInnerTubeURL) {
        self.context = context
        self.session = session
        self.baseURL = baseURL
    }
    
    /// Sends a request to YouTube.
    /// - Parameters:
    ///   - endpoint: The API path (e.g., "/v1/player")
    ///   - body: The unique data for this request (e.g., ["videoId": "123"])
    /// - Returns: The decoded response
    public func send<T: Decodable>(_ endpoint: String, body: [String: Any] = [:]) async throws -> T {
        
        // 1. Build the full URL with the API Key
        // Result: https://www.youtube.com/youtubei/v1/player?key=AIza...
        guard let url = URL(string: "\(baseURL)\(endpoint)?key=\(context.apiKey)") else {
            throw URLError(.badURL)
        }
        
        // 2. Setup the Request
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        // 3. Add Headers (User-Agent, etc.)
        for (key, value) in context.headers {
            request.setValue(value, forHTTPHeaderField: key)
        }
        
        // 4. Build the Payload
        // Start with the standard InnerTube "context"
        var payload = context.body
        
        // Add the specific parameters for this request
        // (This is the simple manual merge we discussed)
        for (key, value) in body {
            payload[key] = value
        }
        
        // 6. Encode to JSON
        request.httpBody = try JSONSerialization.data(withJSONObject: payload)
        
        // 7. Send it
        let (data, response) = try await session.data(for: request)
        
        // 8. Check if it worked
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        
        if httpResponse.statusCode != 200 {
            // ⚠️ PRINT THE ERROR FROM GOOGLE
            if let errorString = String(data: data, encoding: .utf8) {
                print("❌ YouTube Error (\(httpResponse.statusCode)): \(errorString)")
            }
            throw URLError(.badServerResponse)
        }
        
        // Debug Response.json
        let path = (NSString(string: "~/Downloads/debug.json")).expandingTildeInPath

        FileManager.default.createFile(
            atPath: path,
            contents: data,
            attributes: nil
        )
        
        print("DEBUG: Saved raw JSON to \(path)")
        
        // 9. Decode the result
        // We use a standard decoder for now. We can add custom date strategies later if needed.
        let decoder = JSONDecoder()
        return try decoder.decode(T.self, from: data)
    }
}
