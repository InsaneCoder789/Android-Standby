# Android Standby website

A Next.js product site for the Android Standby screensaver, with a photographic bedside scene and live HTML/CSS clock faces.

**Live:** [android-standby.vercel.app](https://android-standby.vercel.app) · [The screens](https://android-standby.vercel.app/screens) · [Setup guide](https://android-standby.vercel.app/guide)

## Run locally

```bash
cd website
npm ci
npm run dev
```

Open http://localhost:3027. Build with `npm run build`; serve the production build with `npm start`. `npm run typecheck` checks TypeScript. Vercel supplies the production origin for social-image metadata automatically; `SITE_URL` can override it for another host.

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

Real-device browser coverage, Safari/Firefox behavior, and external weather-provider availability beyond the checks below remain separate acceptance work.

## Search and sharing (SEO)

Everything that search engines and link previews read is generated from one place, `lib/site.ts` (name, title, description, keywords, canonical URL).

| What | Where |
| --- | --- |
| Title, description, keywords, canonical, robots, Open Graph, Twitter card | `app/layout.tsx` and each page's `metadata` |
| Structured data (WebSite, SoftwareApplication, FAQPage, BreadcrumbList) | `components/json-ld.tsx` |
| Social banner, 1200 × 630 JPEG, shared across Open Graph and Twitter cards | [Banner](public/android-standby-social.jpg) and [generation prompt](public/scene/SOCIAL-PREVIEW.md) |
| `sitemap.xml`, `robots.txt`, web manifest, icons | `app/sitemap.ts`, `app/robots.ts`, `app/manifest.ts`, `app/icon.tsx`, `app/apple-icon.tsx` |
| FAQ text (shown on the page and in the FAQ structured data) | `lib/faqs.ts` |

Environment variables (all optional, set them in Vercel):

- `SITE_URL`: the canonical origin, for example a custom domain. Defaults to the Vercel production URL.
- `GOOGLE_SITE_VERIFICATION`: the token from Google Search Console's HTML tag method.
- `BING_SITE_VERIFICATION`: the token from Bing Webmaster Tools.

After deploying: verify the site in Google Search Console, submit `/sitemap.xml`, and request indexing for `/`, `/screens` and `/guide`. Check a share with the Facebook Sharing Debugger or LinkedIn Post Inspector, and the structured data with Google's Rich Results Test.

## Deployment

Hosted on Vercel in the `android-standby` project under Rochiee’s projects. The GitHub connection builds `main` from the `website/` root directory, using `npm ci`, `npm run build`, the Next.js framework preset, and Node.js 22. Production is publicly accessible; preview deployments retain Vercel authentication. Only the website is built and hosted. The Android application is not deployed by Vercel.

## Final review — 8 November 2026

The production build and TypeScript checks passed. An independent Chromium pass exercised all 46 looks at desktop 1440×1000 and mobile 390×932, including fullscreen focus/return, timer and music-state continuity, numeric geometry at 09:59→10:00, coarse-pointer targets, direct mobile navigation at 320px, and optional weather denial/retry/timezone recovery. Identified clipping, modal, navigation, formatting and weather-expiry issues were repaired and retested. The production dependency audit reported zero known vulnerabilities at review time.

Public production checks returned 200 for `/`, `/screens`, `/guide`, the hero image, fonts and icon; invalid weather coordinates returned 400; a live Open-Meteo request returned a validated Celsius forecast. A production browser journey confirmed navigation, timezone switching and fullscreen timer retention without observed console errors. Local detailed evidence is in the ignored project `.qa/runs/20261108T104613Z-website-final-vercel-9eae3b7c/` directory. This is scoped verification, not an exhaustive browser or accessibility guarantee.
