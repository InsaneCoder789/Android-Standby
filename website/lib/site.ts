/** Single source of truth for the site's identity, used by metadata, structured data, the sitemap and the social banner. */
export const SITE_URL = (process.env.SITE_URL || (process.env.VERCEL_PROJECT_PRODUCTION_URL ? `https://${process.env.VERCEL_PROJECT_PRODUCTION_URL}` : 'https://android-standby.vercel.app')).replace(/\/$/, '');
export const REPO_URL = 'https://github.com/InsaneCoder789/Android-Standby';
export const SITE_NAME = 'Android Standby';
export const TAGLINE = 'Standby for Android';
export const TITLE = 'Android Standby — Bedside Clock & Charging Screensaver';
export const DESCRIPTION = 'Android Standby is an open-source bedside clock and charging screensaver for Android 9+. Explore 23 frame families, including music, weather and night clocks.';
export const KEYWORDS = [
  'Standby for Android', 'StandBy mode for Android', 'Android Standby', 'StandBy Android app', 'iPhone StandBy alternative for Android',
  'Android bedside clock', 'Android charging screensaver', 'Android desk clock app', 'Android night clock', 'bedside clock app',
  'Android screensaver clock', 'clock while charging Android', 'landscape charging clock', 'Samsung StandBy clock', 'open source Android clock', 'Android Standby mode', 'Android desk clock while charging', 'Android bedside night clock',
];
export const THEME_COLOR = '#0b0b0c';

export const BANNER = {
  url: '/android-standby-social.jpg',
  width: 1200,
  height: 630,
  type: 'image/jpeg',
  alt: 'Android Standby bedside clock and charging screensaver, with green 9:41 digits on a landscape phone stand',
};

/** Metadata for one page. Next.js replaces (not merges) openGraph and twitter per page, so every page builds the full set here. */
export function pageMeta(o: { title: string | { absolute: string }; shareTitle: string; description: string; shareDescription?: string; path: string }): import('next').Metadata {
  const shareDescription = o.shareDescription ?? o.description;
  return {
    title: o.title,
    description: o.description,
    alternates: { canonical: o.path },
    openGraph: { type: 'website', siteName: SITE_NAME, locale: 'en_US', url: o.path, title: o.shareTitle, description: shareDescription, images: [BANNER] },
    twitter: { card: 'summary_large_image', title: o.shareTitle, description: shareDescription, images: [{ url: BANNER.url, alt: BANNER.alt }] },
  };
}
