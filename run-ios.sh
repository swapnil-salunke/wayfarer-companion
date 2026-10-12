#!/bin/sh
# Build and launch the iOS app on the iOS 26.2 simulator without the IDE's iOS run support.
set -e
cd "$(dirname "$0")"
SIM="${1:-iPhone 17 Pro}"
OUT=build/ios
xcrun simctl boot "$SIM" 2>/dev/null || true
open "$(xcode-select -p)/Applications/Simulator.app" || true
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -sdk iphonesimulator \
  -destination "platform=iOS Simulator,name=$SIM,OS=26.2" -derivedDataPath "$OUT" build
xcrun simctl install booted "$OUT/Build/Products/Debug-iphonesimulator/iosApp.app"
xcrun simctl launch booted com.wayfarer.app.ios
