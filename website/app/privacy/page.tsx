import type { Metadata } from 'next';
import { Footer, Header } from '@/components/chrome';
import { Breadcrumbs } from '@/components/json-ld';
import { REPO_URL, pageMeta } from '@/lib/site';

export const metadata: Metadata = pageMeta({
  title: 'Privacy',
  shareTitle: 'Privacy — Android Standby',
  description: 'What the Android Standby website does and does not collect: no accounts, no cookies, no analytics. Location is used only when you ask for local weather.',
  path: '/privacy',
});

export default function Privacy() {
  return <><Breadcrumbs items={[{ name: 'Android Standby', path: '/' }, { name: 'Privacy', path: '/privacy' }]} /><Header /><main id="main" className="page wrap"><h1>Short and plain.</h1><p className="page-lead">This page covers the Android Standby website. The Android app is open source and runs on your phone; you can read exactly what it does in the repository.</p><div className="prose">
    <section><h2>What this site collects</h2><p>Nothing about you on purpose. There are no accounts, no forms, no cookies and no analytics or advertising scripts. Fonts and images are served from this site, not from a third party.</p></section>
    <section><h2>Local time and weather</h2><p>The live clock on the page uses your device’s time zone. Weather is optional: it only runs if you press the button that asks for your location, and your browser asks permission first. Your coordinates, rounded to two decimals, are sent to this site’s weather endpoint, which asks Open-Meteo for the forecast. They are not stored, and you can use the clock without ever allowing location.</p></section>
    <section><h2>Hosting</h2><p>The site is hosted on Vercel. Like any host, it may keep standard server logs such as IP address and requested page for security and reliability. We do not use them to identify visitors.</p></section>
    <section><h2>Questions</h2><p>Open an issue on <a href={`${REPO_URL}/issues`} rel="noreferrer" target="_blank">GitHub</a> and we will answer there.</p></section>
  </div></main><Footer /></>;
}
