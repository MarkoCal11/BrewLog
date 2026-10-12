import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Capacitor's iOS webview serves the app over the `capacitor://localhost` scheme.
// Module scripts/styles emitted with the `crossorigin` attribute fail to load
// under that scheme → blank white screen. Stripping the attribute is harmless on
// web (the app is same-origin there) and fixes the native boot.
function stripCrossorigin() {
  return {
    name: 'strip-crossorigin',
    transformIndexHtml(html: string) {
      return html.replace(/\s+crossorigin/g, '')
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), stripCrossorigin()],
})
