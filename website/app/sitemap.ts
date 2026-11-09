import type { MetadataRoute } from 'next';
import { SITE_URL } from '@/lib/site';

// One entry per indexable page. lastModified is the build time, which is when the content was last known to be current.
export default function sitemap(): MetadataRoute.Sitemap {
  const now = new Date();
  return [
    { url: `${SITE_URL}/`, lastModified: now, changeFrequency: 'monthly', priority: 1 },
    { url: `${SITE_URL}/screens`, lastModified: now, changeFrequency: 'monthly', priority: 0.8 },
    { url: `${SITE_URL}/guide`, lastModified: now, changeFrequency: 'monthly', priority: 0.7 },
  ];
}
