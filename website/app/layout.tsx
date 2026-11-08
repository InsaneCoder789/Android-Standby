import type { Metadata } from 'next';
import './globals.css';
import '@/components/live-face.css';
import '@/components/live-experience.css';
import { LocalClockProvider } from '@/components/local-clock';
export const metadata: Metadata = {
  metadataBase: new URL(process.env.SITE_URL || (process.env.VERCEL_PROJECT_PRODUCTION_URL ? `https://${process.env.VERCEL_PROJECT_PRODUCTION_URL}` : 'http://localhost:3027')),
  title: 'Android Standby — Time, well spent.',
  description: 'A different side of your Android. Explore 23 clock, music and ambient frame families in a native bedside display inspired by StandBy.',
  applicationName: 'Android Standby',
  openGraph: { title: 'Android Standby — Time, well spent.', description: 'Your Android. A little more present.', images: [{ url: '/frames/float.webp', width: 1800, height: 831 }] },
  robots: { index: true, follow: true },
};
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
 return <html lang="en"><body><a className="skip-link" href="#main">Skip to content</a><LocalClockProvider>{children}</LocalClockProvider></body></html>;
}
