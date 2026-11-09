import type { Metadata } from 'next';
import '@/components/screens.css';
import { Header, Footer } from '@/components/experience';
import ScreensEditorial from '@/components/screens-editorial';
import { Breadcrumbs } from '@/components/json-ld';
import { pageMeta } from '@/lib/site';
export const metadata: Metadata = pageMeta({
  title: 'Every Standby clock face for Android',
  shareTitle: 'Every Standby clock face for Android — Android Standby',
  description: 'Browse every Android Standby screen: digital, analog, flip, nixie and word clocks, music, weather, photos, sun, moon and aurora faces. Each preview is live.',
  shareDescription: 'Every Android Standby face, live: clocks, everyday information and atmosphere.',
  path: '/screens',
});
export default function Screens() { return <><Breadcrumbs items={[{name:'Android Standby',path:'/'},{name:'The screens',path:'/screens'}]} /><Header /><ScreensEditorial /><Footer /></>; }
