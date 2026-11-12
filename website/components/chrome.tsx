'use client';
/** The little chrome there is: a wordmark and two links above, a plain line of facts below. The faces are the interface. */
import { usePathname } from 'next/navigation';
import { REPO_URL } from '@/lib/site';

export function Header() {
  const path = usePathname();
  const cur = (p: string) => path === p ? 'page' as const : undefined;
  return <header className="top">
    <a className="mark" href="/" aria-label="Android Standby home">standby</a>
    <nav aria-label="Main navigation">
      <a href="/screens" aria-current={cur('/screens')}>All faces</a>
      <a href="/guide" aria-current={cur('/guide')}>Set it up</a>
    </nav>
  </header>;
}

export function Footer() {
  return <footer className="foot">
    <div className="wrap">
      <a className="mark" href="/">standby</a>
      <p>An independent, open-source project inspired by StandBy. Not affiliated with Apple or Samsung.</p>
      <nav aria-label="Footer"><a href="/screens">All faces</a><a href="/guide">Set it up</a><a href="/privacy">Privacy</a><a href={REPO_URL} target="_blank" rel="noreferrer">GitHub</a></nav>
    </div>
  </footer>;
}
