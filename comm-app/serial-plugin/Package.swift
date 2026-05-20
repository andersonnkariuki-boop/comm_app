// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "SerialPlugin",
    platforms: [.iOS(.v15)],
    products: [
        .library(
            name: "SerialPlugin",
            targets: ["SerialPluginPlugin"])
    ],
    dependencies: [
        .package(url: "https://github.com/ionic-team/capacitor-swift-pm.git", from: "8.0.0")
    ],
    targets: [
        .target(
            name: "SerialPluginPlugin",
            dependencies: [
                .product(name: "Capacitor", package: "capacitor-swift-pm"),
                .product(name: "Cordova", package: "capacitor-swift-pm")
            ],
            path: "ios/Sources/SerialPluginPlugin"),
        .testTarget(
            name: "SerialPluginPluginTests",
            dependencies: ["SerialPluginPlugin"],
            path: "ios/Tests/SerialPluginPluginTests")
    ]
)