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
    /// Sends a request and decodes it immediately.
    public func send<T: Decodable>(_ endpoint: String, body: [String: String] = [:]) async throws -> T {
        let data = try await get(endpoint, body: body)
        let decoder = JSONDecoder()
        return try decoder.decode(T.self, from: data)
    }
    
    /// Sends a request and returns the Raw Data (no decoding).
    /// Useful for complex endpoints where we need to inspect the JSON before decoding.
    public func get(_ endpoint: String, body: [String: String] = [:]) async throws -> Data {
        // 1. Build URL (Robust way)
        guard let url = URL(string: baseURL) else { throw URLError(.badURL) }
        
        // This handles the "/" logic automatically so "v1/search" and "/v1/search" both work
        var components = URLComponents(url: url.appendingPathComponent("v1").appendingPathComponent(endpoint), resolvingAgainstBaseURL: true)
        components?.queryItems = [URLQueryItem(name: "key", value: context.apiKey)]
        
        guard let url = components?.url else { throw URLError(.badURL) }
        
        // 2. Setup Request
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        // 3. Add Headers
        for (key, value) in context.headers {
            request.setValue(value, forHTTPHeaderField: key)
        }
        
        // 4. Build Payload
        var payload = context.body
        for (key, value) in body {
            payload[key] = value
        }
        
        request.httpBody = try JSONSerialization.data(withJSONObject: payload)
        
        // 5. Send
        let (data, response) = try await session.data(for: request)
        
        // 6. Validate
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        
        if httpResponse.statusCode != 200 {
            if let errorString = String(data: data, encoding: .utf8) {
                print("❌ YouTube Error (\(httpResponse.statusCode)): \(errorString)")
            }
            throw URLError(.badServerResponse)
        }
        
#if DEBUG
        let path = (NSString(string: "~/Downloads/debug.json")).expandingTildeInPath
        FileManager.default.createFile(atPath: path, contents: data)
        print("💾 Debug: Saved response to \(path)")
#endif
        
        return data
    }
    
    // Overload for complex bodies (needed for Charts & Analytics)
    /// Sends a request with a complex nested body (required for Like, Subscribe, etc.)
    public func sendComplexRequest(_ endpoint: String, body: [String: Any]) async throws -> Data {
        guard let baseUrlURL = URL(string: baseURL) else { throw URLError(.badURL) }
        
        var components = URLComponents(url: baseUrlURL.appendingPathComponent(endpoint), resolvingAgainstBaseURL: true)
        components?.queryItems = [URLQueryItem(name: "key", value: context.apiKey)]
        
        guard let url = components?.url else { throw URLError(.badURL) }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        
        // Headers
        for (key, value) in context.headers {
            request.setValue(value, forHTTPHeaderField: key)
        }
        
        // Merge Context + Complex Body
        var payload = context.body
        for (key, value) in body {
            payload[key] = value
        }
        
        request.httpBody = try JSONSerialization.data(withJSONObject: payload)
        
        let (data, response) = try await session.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw URLError(.badServerResponse)
        }
        
        if httpResponse.statusCode != 200 {
            if let errorString = String(data: data, encoding: .utf8) {
                print("❌ YouTube Error (\(httpResponse.statusCode)): \(errorString)")
            }
            throw URLError(.badServerResponse)
        }
        
        return data
    }
}
