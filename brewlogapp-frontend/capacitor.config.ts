import type { CapacitorConfig } from '@capacitor/cli'

const config: CapacitorConfig = {
  appId: 'com.brewlog.app',
  appName: 'BrewLog',
  // Vite builds the web app here; `npx cap sync` copies it into the native shell.
  webDir: 'dist',
}

export default config
