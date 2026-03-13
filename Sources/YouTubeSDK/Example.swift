//
//  Example.swift
//  YouTubeSDK
//
//  Created by Aarav Gupta on 13/03/26.
//

import SwiftUI
import Observation
import YouTubeSDK

/*
 NOTE: To run this example, it must be part of an app target that imports the YouTubeSDK package.
 Running 'swift Example.swift' directly will fail because it's a module source file.
*/

@MainActor
@Observable
public final class YouTubeViewModel {
    
    public var searchResults: [YouTubeItem] = []
    public var isLoading: Bool = false
    public var errorMessage: String?
    
    // Dependency Injection: Inject the central YouTube manager
    private let youtube: YouTube
    
    public init(youtube: YouTube = .shared) {
        self.youtube = youtube
    }
    
    /// Performs a search and updates the state.
    public func performSearch(query: String) async {
        self.isLoading = true
        self.errorMessage = nil
        
        do {
            let result = try await youtube.main.search(query)
            self.searchResults = result.items
        } catch {
            self.errorMessage = error.localizedDescription
        }
        
        self.isLoading = false
    }
}

// MARK: - SwiftUI View (Showcasing Observation & DI)
public struct YouTubeExampleView: View {
    
    @State private var viewModel: YouTubeViewModel
    @State private var searchText: String = "Lo-fi"
    
    @Environment(\.youtubeManager) private var youtubeManager
    
    public init(youtube: YouTube = .shared) {
        // Injecting the dependency during initialization
        _viewModel = State(wrappedValue: YouTubeViewModel(youtube: youtube))
    }
    
    public var body: some View {
        NavigationStack {
            VStack {
                // Search Bar
                HStack {
                    TextField("Search YouTube...", text: $searchText)
                        .textFieldStyle(.roundedBorder)
                    
                    Button("Go") {
                        Task { await viewModel.performSearch(query: searchText) }
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding()
                
                // Content List
                List(viewModel.searchResults, id: \.self) { item in
                    switch item {
                    case .video(let video):
                        VideoRow(video: video)
                    case .playlist(let playlist):
                        Label(playlist.title, systemImage: "play.square.stack")
                    default:
                        EmptyView()
                    }
                }
                .overlay {
                    if viewModel.isLoading {
                        ProgressView("Fetching...")
                    } else if let error = viewModel.errorMessage {
                        ContentUnavailableView("Error", systemImage: "exclamationmark.triangle", description: Text(error))
                    }
                }
            }
            .navigationTitle("YouTube Example")
        }
    }
}

// Basic row component for modularity
struct VideoRow: View {
    let video: YouTubeVideo
    
    var body: some View {
        HStack(alignment: .top) {
            // Placeholder for thumbnail (Real implementation would use AsyncImage)
            Color.gray.opacity(0.3)
                .frame(width: 120, height: 67)
                .cornerRadius(8)
            
            VStack(alignment: .leading) {
                Text(video.title)
                    .font(.headline)
                    .lineLimit(2)
                
                Text(video.author ?? "Unknown Artist")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                
                if let views = video.viewCountText {
                    Text(views)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Environment Support
// Boilerplate to allow injecting the YouTube manager via .environment()
private struct YouTubeManagerKey: EnvironmentKey {
    static let defaultValue: YouTube = .shared
}

extension EnvironmentValues {
    public var youtubeManager: YouTube {
        get { self[YouTubeManagerKey.self] }
        set { self[YouTubeManagerKey.self] = newValue }
    }
}

// MARK: - Preview
#Preview {
    YouTubeExampleView()
}
