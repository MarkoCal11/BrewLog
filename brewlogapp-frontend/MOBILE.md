# BrewLog — iOS (Capacitor) build guide

The web app is wrapped with **Capacitor 8** to run as a native iOS app for personal
sideload (no App Store). The JS side is already scaffolded:

- `@capacitor/core` + `@capacitor/camera` (deps), `@capacitor/cli` + `@capacitor/ios` (dev deps)
- `capacitor.config.ts` — appId `com.brewlog.app`, appName `BrewLog`, webDir `dist`
- `src/api/config.ts` — single `VITE_API_URL` env var drives every REST call, the SSE stream, and photo URLs
- `src/utils/pickImage.ts` — uses the native camera/photo library on device, `<input type=file>` on web
- `.env.example` — documents the backend URL options

The native `ios/` project is **not** generated yet because it needs full Xcode + CocoaPods,
which aren't installed on this machine. Steps below are yours to run once those exist.

---

## One-time prerequisites (you, GUI)
1. **Install Xcode** from the Mac App Store (~7 GB), launch it once to accept the license.
2. Point the command-line tools at it:
   ```
   sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
   ```
3. **Install CocoaPods** (Capacitor uses it to install native pods):
   ```
   sudo gem install cocoapods
   ```
   (or `brew install cocoapods` if you install Homebrew first)
4. A free **Apple ID** is enough to sideload to your own iPhone (7-day signing). A paid
   Apple Developer account ($99/yr) gives 1-year signing.

## Build & run on your iPhone
1. **Set the backend URL** the phone can reach. For LAN/Wi-Fi testing, find your Mac's IP:
   ```
   ipconfig getifaddr en0
   ```
   Create `.env` (copy from `.env.example`) with e.g. `VITE_API_URL=http://192.168.1.20:8080`.
   The phone must be on the **same Wi-Fi** as the Mac, and the Spring backend must bind to
   `0.0.0.0` (not just localhost) and allow that origin via CORS.
2. **Build the web app** (bakes in `VITE_API_URL`):
   ```
   npm run build
   ```
3. **Add the iOS platform** (first time only — creates `ios/`):
   ```
   npx cap add ios
   ```
4. **Sync** the built web assets + plugins into the native project (re-run after every
   `npm run build` or dependency change):
   ```
   npx cap sync ios
   ```
5. **Edit `ios/App/App/Info.plist`** (see snippets below) for camera permission + the
   LAN-HTTP exception.
6. **Open in Xcode**:
   ```
   npx cap open ios
   ```
   - Select the `App` target → Signing & Capabilities → set your **Team** (your Apple ID).
   - Plug in your iPhone, select it as the run destination, press ▶.
   - First run: on the phone, trust the developer cert under Settings → General → VPN &
     Device Management.

## Day-to-day after a code change
```
npm run build && npx cap sync ios && npx cap open ios   # then ▶ in Xcode
```

---

## Info.plist additions (step 5)

**Camera + photo library permission strings** (required, or the app crashes when picking a
photo — `src/utils/pickImage.ts` uses `CameraSource.Prompt`):
```xml
<key>NSCameraUsageDescription</key>
<string>BrewLog uses the camera to photograph your coffee bags.</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>BrewLog lets you attach photos of your coffee bags from your library.</string>
<key>NSPhotoLibraryAddUsageDescription</key>
<string>BrewLog may save coffee photos to your library.</string>
```

**App Transport Security exception** — ONLY needed if `VITE_API_URL` is plain `http://`
(i.e. a LAN IP). iOS blocks cleartext HTTP by default. Scope it to your Mac's IP rather than
allowing all insecure loads:
```xml
<key>NSAppTransportSecurity</key>
<dict>
  <key>NSExceptionDomains</key>
  <dict>
    <key>192.168.1.20</key>   <!-- replace with your Mac's LAN IP -->
    <dict>
      <key>NSExceptionAllowsInsecureHTTPLoads</key>
      <true/>
    </dict>
  </dict>
</dict>
```
If you switch to a **hosted HTTPS backend**, drop this block entirely — HTTPS needs no exception.

---

## Notes
- **SSE report push works in the webview** — `@microsoft/fetch-event-source` runs fine inside
  WKWebView, so the live "new report arrived" banner keeps working on device.
- **OS-level push when the app is closed** is a separate, later feature (needs APNs +
  `@capacitor/push-notifications`); the in-app live banner does not require it.
- **`ios/` is regenerable** — it's gitignored except the project shell; if it ever breaks,
  delete it and re-run `npx cap add ios`.
- Re-run `npx cap sync ios` whenever you change `VITE_API_URL`, rebuild, or add a plugin.
