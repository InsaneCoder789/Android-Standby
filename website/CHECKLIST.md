# Launch and security checklist

Status: ✅ done and verified · 🛠 done, needs a live check · 👤 needs you · ➖ not applicable (static site, no login or database)

## Transport and domain
1. ✅ HTTPS with auto-renewing certificate (Vercel)
2. ✅ HSTS header (`next.config.ts` and Vercel)
3. 👤 Registrar 2FA, domain lock and DNSSEC (needs a custom domain)
4. ➖ Dangling DNS records (no custom DNS yet)
5. 👤 One canonical host: `www` to apex redirect (set when a custom domain is added; canonical URLs are already set)

## Browser-facing security
6. 🛠 Content Security Policy (production only)
7. 🛠 `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`, `Permissions-Policy`, COOP
8. ➖ Secure cookies (the site sets none)
9. ➖ CSRF (no state-changing forms)
10. ✅ CORS: no cross-origin access to `/api/weather`
11. ✅ Input validation on `/api/weather` (range-checked, rounded, upstream payload validated)
12. 👤 Rate limiting on `/api/weather` (enable Vercel Firewall rate limit rule)
13. ➖ File uploads
14. ✅ No debug output: custom error pages, `poweredByHeader` off
15. ✅ Secrets: `.env*` ignored, none committed, no `NEXT_PUBLIC_` secrets
16. 👤 2FA on GitHub, Vercel and any domain account
17. ➖ Admin routes, database access
18. ✅ Lockfile committed, exact Next.js version pinned
19. 👤 Turn on Dependabot and GitHub secret scanning

## Pages and errors
20. ✅ Custom 404 (real 404 status, `noindex`)
21. ✅ Custom error page and global error page
22. ➖ Loading and empty states (static pages)
23. ✅ Redirect behaviour reviewed (single URL per page)

## Search and sharing
24. ✅ Unique titles, descriptions and canonical URLs per page
25. ✅ Open Graph and Twitter cards with preview banner
26. ✅ `sitemap.xml` and `robots.txt` (no accidental `noindex`)
27. ✅ Favicon, Apple touch icon and web manifest
28. ✅ Structured data (WebSite, SoftwareApplication, FAQ, breadcrumbs)
29. 👤 Verify in Google Search Console and Bing Webmaster Tools, submit the sitemap

## Quality and speed
30. 🛠 Cache headers for fonts, frames and scene images
31. 👤 Lighthouse run on the live URL (performance, accessibility, SEO)
32. ✅ Accessibility basics: `lang`, skip link, alt text on images, focus styles
33. 🛠 No broken links or images (checked on the production build)
34. ✅ No placeholder text, no `console.log` in site code
35. 👤 Test on a real phone over mobile data, in a private window
36. ✅ Reduced-motion and mobile layouts exist
37. ✅ UTF-8 and page language set

## Legal and trust
38. ✅ Privacy page, linked in the footer and sitemap
39. ➖ Cookie notice (no cookies)
40. ✅ Contact route (GitHub issues)
41. ✅ `security.txt` at `/.well-known/security.txt`, plus `humans.txt`

## Build and release
42. ✅ Production build passes (verified from a `/tmp` copy; see the README note)
43. 👤 Protect `main` on GitHub (require review or a passing check)
44. ✅ Preview deployments per branch (Vercel)
45. ✅ Rollback: promote an earlier deployment in Vercel
46. 👤 Pin the Node version (set it in Vercel project settings)
47. 👤 Staging: use a preview deployment, keep it `noindex` (Vercel does this for previews)

## Reliability and operations
48. 👤 Uptime monitoring (free external checker)
49. 👤 Error tracking (Sentry or Vercel logs alerts)
50. ✅ Weather outage handled: clock works without it
51. 👤 Spend limit and usage alerts in Vercel
52. ✅ Dates and time zones come from the visitor's device
53. ✅ Third parties: none loaded in the browser

## After launch
54. 👤 Add a custom domain, then set `SITE_URL` in Vercel
55. 👤 Check link previews in WhatsApp, X, LinkedIn after the deploy
56. 👤 Calendar reminders: dependency updates, domain renewal, checking logs

## Performance (added with the 3D redesign)
57. ✅ Homepage hero uses baked renders of the 3D scene (`public/scene/posters`), no WebGL: DOMContentLoaded ~0.3 s, load ~0.5 s on a local production build
58. ✅ Hero render preloaded with the HTML (`preload` in `app/page.tsx`), portrait renders on phones
59. 🛠 `/screens` live 3D: one render pass (no post-processing, no transmission, static shadows), draws only while scrolling, pauses off-screen, DPR ≤ 1.25; phones, low-memory devices and data-saver get the baked chapter renders instead. Frame rate not yet measured on a visible display
60. ✅ Scene textures 7.5 MB → 0.65 MB; no CPU texture generation at runtime
61. ✅ 0 npm vulnerabilities; all 69 frame captures present
