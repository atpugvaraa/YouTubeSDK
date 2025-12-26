import Testing
@testable import YouTubeSDK

struct YouTubeMusicTests {
    @Test("Connect to YouTube Music")
    func testMusicConnection() async throws {
        let client = YouTubeMusicClient()
        let response = try await client.search("banda kaam ka")
        
        print(response)
    }
}
