import Testing
@testable import YouTubeSDK

struct YouTubeMusicTests {
    @Test("Connect to YouTube Music")
    func testMusicConnection() async throws {
        let client = YouTubeMusicClient()
        try await client.search("Pink Floyd")
    }
}
