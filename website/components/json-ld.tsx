import { DESCRIPTION, REPO_URL, SITE_NAME, SITE_URL, TAGLINE } from '@/lib/site';
import { faqs } from '@/lib/faqs';

/** Renders one JSON-LD block. Server component, so the data is in the initial HTML that crawlers read. */
export function JsonLd({ data }: { data: object }) {
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: JSON.stringify(data).replace(/</g, '\\u003c') }} />;
}

const screenshots = ['/frames/float.webp', '/frames/digital.webp', '/frames/calendar.webp'].map((p) => `${SITE_URL}${p}`);

/** Site-wide identity: what the site is and what the product is. Facts only; no ratings, prices or store links the project does not have. */
export function SiteGraph() {
  return <JsonLd data={{
    '@context': 'https://schema.org',
    '@graph': [
      { '@type': 'WebSite', '@id': `${SITE_URL}/#website`, url: SITE_URL, name: SITE_NAME, alternateName: [TAGLINE, 'Android Standby mode'], description: DESCRIPTION, inLanguage: 'en' },
      {
        '@type': 'SoftwareApplication', '@id': `${SITE_URL}/#app`, name: SITE_NAME, alternateName: [TAGLINE, 'Android Standby clock'],
        url: SITE_URL, description: DESCRIPTION, softwareVersion: '1.0', applicationCategory: 'LifestyleApplication', operatingSystem: 'Android 9 or later',
        softwareRequirements: 'Android 9 (API 28) or newer', isAccessibleForFree: true, image: screenshots[0], screenshot: screenshots,
        featureList: ['23 clock, music, everyday and ambient frame families', 'Runs as an Android screensaver or an optional Instant mode', 'Automatic night mode', 'Music controls, calls, alarms and timers on their own frames', 'Charging ring, weather and photo frames'],
        sameAs: [REPO_URL], mainEntityOfPage: { '@id': `${SITE_URL}/#website` },
      },
    ],
  }} />;
}

export function FaqGraph() {
  return <JsonLd data={{ '@context': 'https://schema.org', '@type': 'FAQPage', mainEntity: faqs.map(([q, a]) => ({ '@type': 'Question', name: q, acceptedAnswer: { '@type': 'Answer', text: a } })) }} />;
}

export function Breadcrumbs({ items }: { items: { name: string; path: string }[] }) {
  return <JsonLd data={{ '@context': 'https://schema.org', '@type': 'BreadcrumbList', itemListElement: items.map((it, i) => ({ '@type': 'ListItem', position: i + 1, name: it.name, item: `${SITE_URL}${it.path}` })) }} />;
}

/** The faces on /screens as an ItemList, each with its preview image, so search engines can show them as a collection. */
export function FacesGraph({ items }: { items: { name: string; description: string; image: string; anchor: string }[] }) {
  return <JsonLd data={{ '@context': 'https://schema.org', '@type': 'ItemList', name: 'Android Standby clock faces', numberOfItems: items.length,
    itemListElement: items.map((it, i) => ({ '@type': 'ListItem', position: i + 1, url: `${SITE_URL}/screens#${it.anchor}`, item: { '@type': 'CreativeWork', name: it.name, description: it.description, image: `${SITE_URL}${it.image}` } })) }} />;
}
