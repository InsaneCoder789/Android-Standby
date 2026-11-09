import type { MetadataRoute } from 'next';
import { DESCRIPTION, SITE_NAME, THEME_COLOR } from '@/lib/site';

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: `${SITE_NAME}: Standby for Android`, short_name: 'Standby', description: DESCRIPTION, start_url: '/', scope: '/', display: 'standalone',
    background_color: THEME_COLOR, theme_color: THEME_COLOR, categories: ['lifestyle', 'utilities'], lang: 'en',
    icons: [{ src: '/icon/small', sizes: '192x192', type: 'image/png' }, { src: '/icon/large', sizes: '512x512', type: 'image/png' }],
  };
}
