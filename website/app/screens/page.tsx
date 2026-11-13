import type { Metadata } from 'next';
import '@/components/screens.css';
import { Header, Footer } from '@/components/chrome';
import ScreensEditorial from '@/components/screens-editorial';
import { Breadcrumbs, FacesGraph } from '@/components/json-ld';
import { frames } from '@/lib/frames';
import { copy } from '@/lib/screens-copy';
import { pageMeta } from '@/lib/site';
export const metadata: Metadata = pageMeta({
  title: 'Live clock faces for Android Standby',
  shareTitle: 'Android Standby clock faces and live demos',
  description: 'Explore 69 live Android Standby looks, including digital, analog, flip and Nixie clocks, plus music, weather, calendar and call previews.',
  shareDescription: 'Every Android Standby face, live: clocks, everyday information and atmosphere.',
  path: '/screens',
});
export default function Screens() { return <><FacesGraph items={frames.map(f => ({ name: f.name, description: copy[f.id] ?? f.name, image: f.looks[0].image, anchor: `plate-${f.id}` }))} /><Breadcrumbs items={[{name:'Android Standby',path:'/'},{name:'The screens',path:'/screens'}]} /><Header /><ScreensEditorial /><Footer /></>; }
