import Testing
@testable import YouTubeSDK

struct YouTubeChartsTests {
    @Test("Connect to YouTube Charts")
    func testChartsConnection() async throws {
        let client = YouTubeChartsClient()
        try await client.getTopSongs()
    }
}
