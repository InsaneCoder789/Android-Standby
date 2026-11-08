import type { Metadata } from 'next';
import '@/components/screens.css';
import { Header, Footer } from '@/components/experience';
import ScreensEditorial from '@/components/screens-editorial';
export const metadata: Metadata = { title: 'The screens — Android Standby', description: 'A field guide to every face Android Standby can wear: clocks, everyday information and atmosphere, each one live.', openGraph: { title: 'The screens — Android Standby', description: 'Every face, live.', images: [{ url: '/frames/float.webp', width: 1800, height: 831 }] } };
export default function Screens() { return <><Header /><ScreensEditorial /><Footer /></>; }
