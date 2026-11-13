'use client';
/**
 * The top of the homepage, in two parts.
 *  - Hero: a wall of live faces, rows of framed screens in three sizes drifting past each other on a tilt, with the headline over it.
 *  - Reel: a white section below. One screen stays in view while the page scrolls, and it changes face as each description
 *    passes, so reading down the page plays like a short film of the app.
 * All movement is CSS (transforms, opacity, scroll-driven animation where the browser has it). The only script work is noticing
 * which description is in the middle of the window.
 */
import { useEffect, useRef, useState, type CSSProperties } from 'react';
import { frames } from '@/lib/frames';
import { LiveFace } from './live-face';
import { useLocalClock } from './local-clock';

const total = frames.length;
// The wall: three rows, each its own size and speed. Faces that need a permission or a sample identity are left out.
const WALL: number[][] = [[11, 0, 7, 18, 9, 12], [6, 2, 21, 1, 5, 16], [8, 14, 10, 13, 19, 20]];
const nameOf = (id: number) => frames.find(f => f.id === id)?.name ?? '';
const Words = ({ text }: { text: string }) => <>{text.split(' ').map((w, k) => <span key={k} className="word" aria-hidden="true"><i style={{ '--k': k } as CSSProperties}>{w}</i></span>)}</>;

export function Hero() {
  const ref = useRef<HTMLElement>(null), [seen, setSeen] = useState(true);
  const clock = useLocalClock();
  const place = clock.timeZone.replaceAll('_', ' ').split('/').at(-1) || '';
  // The wall only drifts while it is on screen.
  useEffect(() => { const el = ref.current; if (!el) return; const io = new IntersectionObserver(([e]) => setSeen(e.isIntersecting)); io.observe(el); return () => io.disconnect(); }, []);
  return <section ref={ref} className={`hero${seen ? '' : ' is-away'}`} aria-labelledby="hero-title">
    <div className="wall" aria-hidden="true">
      <div className="wall-tilt">{WALL.map((row, r) => <div key={r} className={`wall-row row-${r}`}>{row.map(id => <div key={id} className="tile"><LiveFace frame={id} /></div>)}</div>)}</div>
    </div>
    <div className="wall-shade" aria-hidden="true" />
    <div className="hero-words">
      <h1 id="hero-title" aria-label="Sideways, it’s a clock."><Words text="Sideways, it’s a clock." /></h1>
      <div className="hero-row">
        <p>Put your Android on the charger, turn it on its side, and it becomes a bedside clock. Every screen behind these words is live{place && `, on ${place} time`}.</p>
        <p className="hero-acts"><a className="btn" href="/screens">See all {total} faces</a><a className="link" href="/guide">Set it up</a></p>
      </div>
    </div>
  </section>;
}

type Scene = { say: string; face: number; note: string; acc: string; interactive?: boolean };
const SCENES: Scene[] = [
  { say: 'It runs on your time.', face: 0, acc: '#3fae2a', note: 'A giant time, a thin line for the seconds, the date and weather in glass pills. This is the real face, not a picture of one.' },
  { say: 'Read it from across a dark room.', face: 6, acc: '#0b0b0c', note: 'Four mechanical cards with a hinge through the middle, set big enough to read across a dark room.' },
  { say: 'Your music takes the whole screen.', face: 2, acc: '#c2318a', interactive: true, note: 'The artwork sets the colour and the controls are big enough to find half asleep. Press play: it is a web demo, and nothing reaches your phone.' },
  { say: 'Four cities. No arithmetic.', face: 5, acc: '#2b7fe0', note: 'Four cities as four cards, each with its own day and night bar. No arithmetic at midnight.' },
  { say: 'Something slow to fall asleep to.', face: 18, acc: '#1f9c78', note: 'Slow, drifting colour behind the time. Nothing moves faster than you can ignore.' },
  { say: `${total} faces. Keep three. Swipe.`, face: 9, acc: '#d9711a', note: 'Pick up to three, arrange them your way and swipe between them on the phone.' },
];

export function Reel() {
  const [at, setAt] = useState(0);
  const steps = useRef<(HTMLElement | null)[]>([]);
  useEffect(() => {
    // On a narrow screen the pinned face covers the top of the window, so the switch happens lower down, as the next heading arrives.
    const io = new IntersectionObserver(es => es.forEach(e => { if (e.isIntersecting) setAt(Number((e.target as HTMLElement).dataset.i)); }), { rootMargin: window.innerWidth <= 900 ? '-68% 0px -32% 0px' : '-50% 0px -50% 0px' });
    steps.current.forEach(el => el && io.observe(el));
    return () => io.disconnect();
  }, []);
  return <section className="reel" id="tour" aria-label="Six faces, one after another">
    <div className="wrap reel-grid">
      <div className="reel-stage">
        <div className="reel-screen">
          {SCENES.map((s, i) => <div key={i} className={`reel-face${i === at ? ' is-on' : ''}`} aria-hidden={i === at ? undefined : true}>{Math.abs(i - at) <= 1 && <LiveFace frame={s.face} interactive={!!s.interactive && i === at} />}</div>)}
        </div>
        <p className="reel-count" aria-hidden="true"><b style={{ color: SCENES[at].acc }}>{nameOf(SCENES[at].face)}</b><span>{at + 1} / {SCENES.length}</span></p>
      </div>
      <div className="reel-steps">
        {SCENES.map((s, i) => <article key={i} data-i={i} ref={el => { steps.current[i] = el; }} className={`reel-step${i === at ? ' is-on' : ''}`}>
          <h2>{s.say}</h2>
          <p>{s.note}</p>
          {i === SCENES.length - 1 && <p className="reel-acts"><a className="btn btn-ink" href="/screens">See all {total} faces</a><a className="link" href="/guide">Set it up</a></p>}
        </article>)}
      </div>
    </div>
  </section>;
}
