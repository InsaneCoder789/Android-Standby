import type { Metadata } from 'next';
import { Footer, Header } from '@/components/chrome';

export const metadata: Metadata = { title: 'Page not found', robots: { index: false, follow: true } };

export default function NotFound() {
  return <><Header /><main id="main" className="page wrap"><h1>Nothing here.</h1><p className="page-lead">That page doesn’t exist, or it moved. The home page, the faces and the setup guide are all one tap away.</p><p className="page-acts"><a className="btn" href="/">Back to the start</a><a className="link" href="/screens">See every face</a><a className="link" href="/guide">Setup guide</a></p></main><Footer /></>;
}
