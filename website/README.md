# Android Standby website

A Next.js product site for the Android Standby screensaver, with a photographic bedside scene and live HTML/CSS clock faces.

## Run locally

```bash
cd website
npm ci
npm run dev
```

Open http://localhost:3027. Build with `npm run build`; serve the production build with `npm start`. `npm run typecheck` checks TypeScript. Set `SITE_URL` to the eventual deployment origin for absolute social-image metadata. No deployment has been configured by this iteration.

## Live collection

The catalog follows the Android app's 23 frame families and 44 looks, plus a Calls showcase (incoming and on-call screens) for the alert screen the app shows over any frame. These are web interpretations based on app source and reference captures, not Android code executing in the browser. All clock faces share one second-aligned clock provider. Locale and timezone default to the visitor's browser settings and can be changed with the timezone selector. The clock refreshes immediately when a hidden tab becomes visible again.

On phones every face has a Full screen button that turns the face sideways to fill the screen (Escape or Close returns). The `/screens` field guide organizes faces into clocks, everyday information, and atmosphere. A linked contents section, per-face look controls, and lazy mounting make the collection easy to explore. Fullscreen uses a native modal dialog, contains keyboard focus, restores the trigger on close, and keeps the same timer/music demo state.

The hero uses generated photographic artwork with a blank front-facing screen, overlaid with the same live face components. It replaces the initial procedural Three.js model; no WebGL support is required. [Image provenance and exact prompt](public/scene/PROVENANCE.md).

## Location and weather

Timezone detection does not request geolocation. Only the explicit location button requests browser permission. If granted, coordinates are sent to `/api/weather`, which validates them and requests Celsius conditions from Open-Meteo with a bounded timeout. Successful responses supply temperature, daily high/low, condition code, and timezone. No API key is needed. Denial, timeout, invalid requests, or provider failure leave the clock usable; no sample weather is substituted. Coordinates are not persisted or logged by application code.

The displayed timezone region is not reverse-geocoded city detection. A chosen timezone changes clocks and calendars; location-based weather is cleared when a different timezone is selected to avoid presenting the previous place's conditions as local.

## Boundaries

- Music, calls and timers are website demos. They do not access a connected phone, Spotify account, or Android media session.
- Weather is live only after optional location access and successful provider response. Astronomy without location is labeled as approximate/illustrative.
- The generated phone scene is promotional artwork; actual phone captures remain in the Android repository's `docs/assets`.
- The install guide reflects the repository's source-build flow. There is no invented public APK or app-store destination.
- Reduced-motion preferences disable decorative animation while the clock remains accurate.

Production deployment, real-device browser coverage, and external weather-provider availability require separate verification.
