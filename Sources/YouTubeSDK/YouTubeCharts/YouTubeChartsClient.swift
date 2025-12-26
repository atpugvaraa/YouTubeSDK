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
        // Charts works best with the Web Client hitting the dedicated host
        let context = InnerTubeContext(client: ClientConfig.web)
        self.network = NetworkClient(context: context, baseURL: "https://charts.youtube.com/youtubei")
    }
    
    // MARK: - Global & Local Charts
    
    /// Top Songs Chart
    /// - Parameter country: ISO 3166-1 alpha-2 code (e.g., "US", "IN", "JP", "ZZ" for Global)
    public func getTopSongs(country: String = "ZZ") async throws -> [YouTubeChartItem] {
        return try await fetchChart(browseId: "FEmusic_analytics_charts_songs", country: country, type: .song)
    }
    
    /// Top Music Videos Chart
    public func getTopVideos(country: String = "ZZ") async throws -> [YouTubeChartItem] {
        return try await fetchChart(browseId: "FEmusic_analytics_charts_videos", country: country, type: .video)
    }
    
    /// Top Artists Chart
    public func getTopArtists(country: String = "ZZ") async throws -> [YouTubeChartItem] {
        return try await fetchChart(browseId: "FEmusic_analytics_charts_artists", country: country, type: .artist)
    }
    
    /// Trending (Global/Local)
    public func getTrending(country: String = "ZZ") async throws -> [YouTubeChartItem] {
        return try await fetchChart(browseId: "FEmusic_analytics_charts_trending", country: country, type: .video)
    }
    
    // MARK: - Private Helpers
    
    private func fetchChart(browseId: String, country: String, type: YouTubeChartItem.ChartItemType) async throws -> [YouTubeChartItem] {
        let body: [String: Any] = [
            "browseId": browseId,
            "formData": [
                "selectedValues": [
                    country // This magic array filters the chart by country
                ]
            ]
        ]
        
        let data = try await network.sendComplexRequest("browse", body: body)
        
        // Since NetworkClient expects [String: String], and "formData" is complex,
        // we might need to serialize formData to a JSON string or rely on NetworkClient updates.
        // CHECK: If NetworkClient only takes [String: String], we need to adapt it.
        // Assuming you can pass complex body or updated NetworkClient to support Any:
        // If not, we serialize formData manually:
        
        // WORKAROUND if NetworkClient is strict:
        // We can't send nested JSON in [String: String].
        // We MUST verify NetworkClient supports [String: Any] (which you updated earlier) or use a raw request helper.
        // Assuming NetworkClient allows [String: Any] internally or we fix it.
        
        return parseCharts(from: data, type: type)
    }
    
    // MARK: - Parsing Logic
    
    private func parseCharts(from data: Data, type: YouTubeChartItem.ChartItemType) -> [YouTubeChartItem] {
        guard let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return [] }
        
        var items: [YouTubeChartItem] = []
        
        // Charts uses "musicResponsiveListItemRenderer" (Songs/Videos) and "musicTableRowRenderer" (Artists)
        let rowRenderers = findAll(key: "musicResponsiveListItemRenderer", in: json) + findAll(key: "musicTableRowRenderer", in: json)
        
        for renderer in rowRenderers {
            if let dict = renderer as? [String: Any],
               let item = YouTubeChartItem(from: dict, type: type) {
                items.append(item)
            }
        }
        
        return items
    }
    
    private func findAll(key: String, in container: Any) -> [Any] {
        var results: [Any] = []
        if let dict = container as? [String: Any] {
            if let found = dict[key] { results.append(found) }
            for value in dict.values { results.append(contentsOf: findAll(key: key, in: value)) }
        } else if let array = container as? [Any] {
            for element in array { results.append(contentsOf: findAll(key: key, in: element)) }
        }
        return results
    }
}
