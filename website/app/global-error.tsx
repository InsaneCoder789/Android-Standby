'use client';

// Last-resort page if the root layout itself fails. It brings its own <html> and plain styles because the app's CSS may not have loaded.
export default function GlobalError({ retry }: { error: unknown; retry: () => void }) {
  return <html lang="en"><body style={{ margin: 0, minHeight: '100vh', display: 'grid', placeItems: 'center', background: '#0d0a08', color: '#f4ead9', fontFamily: 'Arial, sans-serif', textAlign: 'center', padding: 24 }}><main><h1 style={{ fontWeight: 500, fontSize: 40, margin: '0 0 12px' }}>Something went wrong.</h1><p style={{ color: '#b9a791', margin: '0 0 24px' }}>The page failed to load. Please try again.</p><button onClick={() => retry()} style={{ background: '#f4ead9', color: '#140f0b', border: 0, borderRadius: 2, padding: '14px 24px', fontSize: 14, cursor: 'pointer' }}>Try again</button></main></body></html>;
}
