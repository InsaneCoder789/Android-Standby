import type { Metadata, Viewport } from 'next';
import './globals.css';
import '@/components/live-face.css';
import { LocalClockProvider } from '@/components/local-clock';
import { SiteGraph } from '@/components/json-ld';
import { BANNER, DESCRIPTION, KEYWORDS, REPO_URL, SITE_NAME, SITE_URL, THEME_COLOR, TITLE } from '@/lib/site';

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  title: { default: TITLE, template: `%s — ${SITE_NAME}` },
  description: DESCRIPTION,
  applicationName: SITE_NAME,
  keywords: KEYWORDS,
  category: 'technology',
  authors: [{ name: SITE_NAME, url: REPO_URL }],
  creator: SITE_NAME,
  publisher: SITE_NAME,
  alternates: { canonical: '/' },
  manifest: '/manifest.webmanifest',
  formatDetection: { telephone: false, email: false, address: false },
  openGraph: { type: 'website', siteName: SITE_NAME, locale: 'en_US', url: '/', title: TITLE, description: DESCRIPTION, images: [BANNER] },
  twitter: { card: 'summary_large_image', title: TITLE, description: DESCRIPTION, images: [{ url: BANNER.url, alt: BANNER.alt }] },
  robots: { index: true, follow: true, googleBot: { index: true, follow: true, 'max-image-preview': 'large', 'max-snippet': -1, 'max-video-preview': -1 } },
  // Set GOOGLE_SITE_VERIFICATION / BING_SITE_VERIFICATION in Vercel to emit the verification tags for Search Console and Bing Webmaster Tools.
  verification: { google: process.env.GOOGLE_SITE_VERIFICATION, other: process.env.BING_SITE_VERIFICATION ? { 'msvalidate.01': process.env.BING_SITE_VERIFICATION } : undefined },
};

export const viewport: Viewport = { width: 'device-width', initialScale: 1, themeColor: THEME_COLOR };

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
 return <html lang="en"><body><SiteGraph /><a className="skip-link" href="#main">Skip to content</a><LocalClockProvider>{children}</LocalClockProvider></body></html>;
}
