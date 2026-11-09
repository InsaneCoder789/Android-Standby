import Experience from '@/components/experience';
import { FaqGraph } from '@/components/json-ld';
import { DESCRIPTION, TITLE, pageMeta } from '@/lib/site';

export const metadata = pageMeta({ title: { absolute: TITLE }, shareTitle: TITLE, description: DESCRIPTION, path: '/' });

export default function Home() { return <><FaqGraph /><Experience /></>; }
