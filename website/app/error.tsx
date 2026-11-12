'use client';
import { useEffect } from 'react';

export default function ErrorPage({ error, retry }: { error: unknown; retry: () => void }) {
  useEffect(() => { console.error(error); }, [error]);
  return <main id="main" className="page wrap"><h1>That didn’t load.</h1><p className="page-lead">An unexpected error stopped this page. Nothing you did caused it. Try again, or head back to the start.</p><p className="page-acts"><button className="btn" onClick={() => retry()}>Try again</button><a className="link" href="/">Back to the start</a></p></main>;
}
