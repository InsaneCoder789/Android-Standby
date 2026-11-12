import type { Metadata } from 'next';
import '@/components/screens.css';
import { Header, Footer } from '@/components/chrome';
import ScreensEditorial from '@/components/screens-editorial';
import { Breadcrumbs, FacesGraph } from '@/components/json-ld';
import { frames } from '@/lib/frames';
import { copy } from '@/lib/screens-copy';
import { pageMeta } from '@/lib/site';
export const metadata: Metadata = pageMeta({
  title: 'Every Standby clock face for Android',
  shareTitle: 'Every Standby clock face for Android — Android Standby',
  description: 'Browse every Android Standby screen: digital, analog, flip, nixie and word clocks, music, weather, photos, sun, moon and aurora faces. Each preview is live.',
  shareDescription: 'Every Android Standby face, live: clocks, everyday information and atmosphere.',
  path: '/screens',
});
export default function Screens() { return <><FacesGraph items={frames.map(f => ({ name: f.name, description: copy[f.id] ?? f.name, image: f.looks[0].image, anchor: `plate-${f.id}` }))} /><Breadcrumbs items={[{name:'Android Standby',path:'/'},{name:'The screens',path:'/screens'}]} /><Header /><ScreensEditorial /><Footer /></>; }
