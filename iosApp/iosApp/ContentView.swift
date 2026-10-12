import SwiftUI
import Shared   // the framework Gradle builds (baseName = "Shared")

// GUIDE: Module 2.4. SwiftUI just hosts the Compose UI.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController { IosPlatformModuleKt.MainViewController() }
    func updateUIViewController(_ vc: UIViewController, context: Context) {}
}
struct ContentView: View { var body: some View { ComposeView() } }
