'use client';
import { useEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import PreviewEnvironment from './preview-environment';
import { ArrowsOut, X } from '@phosphor-icons/react';
import { frames } from '@/lib/frames';
import { chapters, copy, pullQuotes, type Layout } from '@/lib/screens-copy';
import { LiveFace } from './live-face';
import { useLocalClock } from './local-clock';

const pad = (n: number) => String(n).padStart(2, '0');
const WORDS = ['', 'One', 'Two', 'Three', 'Four', 'Five', 'Six', 'Seven', 'Eight', 'Nine', 'Ten', 'Eleven', 'Twelve', 'Thirteen', 'Fourteen', 'Fifteen', 'Sixteen', 'Seventeen', 'Eighteen', 'Nineteen', 'Twenty'];
const words = (n: number) => n <= 20 ? WORDS[n] : n < 30 ? `Twenty-${WORDS[n - 20].toLowerCase()}` : String(n);
const byId = (id: number) => frames.find(f => f.id === id)!;
// Plate numbers follow reading order across the chapters.
const plateNo: Record<number, number> = {}; let counter = 0; chapters.forEach(c => c.order.forEach(([id]) => { plateNo[id] = ++counter; }));

function FullScreenPreview({ surface, returnTo, name, onClose }: { surface: HTMLDivElement; returnTo: HTMLDivElement; name: string; onClose: () => void }) {
  const ref = useRef<HTMLDialogElement>(null);
  const stage = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    if (!dialog || !stage.current) return;
    const previousFocus = document.activeElement as HTMLElement | null;
    const previousOverflow = document.body.style.overflow;
    stage.current.appendChild(surface);
    document.body.style.overflow = 'hidden';
    dialog.showModal();
    return () => {
      dialog.close();
      returnTo.appendChild(surface);
      document.body.style.overflow = previousOverflow;
      previousFocus?.focus({ preventScroll: true });
    };
  }, [surface, returnTo]);
  return <dialog ref={ref} className="lf-expand" aria-label={`${name} full screen`} onCancel={e => { e.preventDefault(); onClose(); }} onClick={e => { if (e.target === e.currentTarget) onClose(); }}>
    <button className="lf-expand-close" aria-label="Close full screen" onClick={onClose}><X size={20} />Close</button>
    <p className="lf-expand-note">{name === 'Calls' ? 'Call interface demo · sample identity and controls' : name === 'Music' || name === 'Clock and Music' ? 'Playback demo · no audio or account connection' : name === 'Timer' ? 'Browser countdown demo · no sound or background alarm' : 'Live web preview'}</p>
    <div className="lf-expand-stage" ref={stage} />
  </dialog>;
}

function Plate({ id, variant, flipped, group }: { id: number; variant: Layout; flipped: boolean; group: string }) {
  const f = byId(id);
  const [look, setLook] = useState(0), [seen, setSeen] = useState(false), [open, setOpen] = useState(false);
  const [surface, setSurface] = useState<HTMLDivElement | null>(null);
  const ref = useRef<HTMLElement>(null), slot = useRef<HTMLDivElement>(null);
  useEffect(() => {
    const element = document.createElement('div');
    element.className = 'ed-live-surface';
    slot.current?.appendChild(element);
    setSurface(element);
    return () => { element.remove(); };
  }, []);
  useEffect(() => {
    const el = ref.current; if (!el || seen) return;
    const io = new IntersectionObserver(([entry]) => { if (entry.isIntersecting) { setSeen(true); io.disconnect(); } }, { rootMargin: '700px 0px' });
    io.observe(el); return () => io.disconnect();
  }, [seen]);
  return <figure ref={ref} id={`plate-${id}`} className={`ed-plate ed-${variant}${flipped ? ' is-flipped' : ''}`}>
    <div className="ed-stage"><div className="ed-screen" ref={slot}>{!seen && <img src={f.looks[look].image} alt={`${f.name} preview`} loading="lazy" />}</div></div>
    {seen && surface && createPortal(<LiveFace frame={id} look={look} interactive />, surface)}
    <figcaption>
      <p className="tag">{pad(plateNo[id])} / {group}</p>
      <h3>{f.name}</h3>
      <p>{copy[id]}</p>
      <div className="ed-actions">
        {f.looks.length > 1 && <div className="ed-looks" role="group" aria-label={`${f.name} looks`}>{f.looks.map((l, j) => <button key={l.name} aria-pressed={look === j} onClick={() => setLook(j)}>{l.name}</button>)}</div>}
        <button className="ed-expand" aria-label={`View ${f.name} full screen`} onClick={() => { setSeen(true); setOpen(true); }}><ArrowsOut size={16} />Full screen</button>
      </div>
    </figcaption>
    {open && surface && slot.current && <FullScreenPreview surface={surface} returnTo={slot.current} name={f.name} onClose={() => setOpen(false)} />}
  </figure>;
}

export default function ScreensEditorial() {
  const clock = useLocalClock(); const total = frames.length, looks = frames.reduce((n, f) => n + f.looks.length, 0);
  const stamp = clock.now ? new Intl.DateTimeFormat(clock.locale, { timeZone: clock.timeZone, hour: 'numeric', minute: '2-digit' }).format(clock.now) : '—';
  const place = clock.city.replace(/ · timezone region$/, '') || clock.timeZone.split('/').pop()?.replaceAll('_', ' ') || '';
  return <main id="main" className="ed">
    <section className="ed-title">
      <div className="wrap">
        <h1>{total} faces.<br />All of them live.</h1>
        <div className="ed-stand">
          <p className="ed-lead">Every face Android Standby can wear, {looks} looks in all. Each one below is live: set to your clock, your calendar and your time zone, not a picture of a phone.</p>
          <div className="ed-now"><p className="tag">Running on <b suppressHydrationWarning>{stamp}</b></p><p className="fine">{place ? `${place} time. ` : ''}Change the time zone or add local weather below.</p></div>
        </div>
        <PreviewEnvironment />
      </div>
    </section>
    <section className="ed-contents" id="contents" aria-label="Contents">
      <div className="wrap ed-toc">{chapters.map(c => <div key={c.key}><h2><span>{c.numeral}</span>{c.title}</h2><ol>{c.order.map(([id]) => <li key={id}><a href={`#plate-${id}`}><b>{pad(plateNo[id])}</b>{byId(id).name}<em>{byId(id).looks.length} {(byId(id).looks.length as number) === 1 ? 'look' : 'looks'}</em></a></li>)}</ol></div>)}</div>
    </section>
    {chapters.map((c, ci) => <div key={c.key}>
      <section className="ed-chapter" id={`chapter-${c.key.toLowerCase()}`}>
        <div className="wrap">
          <header className="ed-chead"><div><p className="tag">{c.numeral} / {c.order.length} faces</p><h2>{c.title}</h2></div><div className="ed-intro">{c.intro.map((p, i) => <p key={i}>{p}</p>)}</div></header>
          <div className="ed-plates">{c.order.map(([id, variant], i) => <Plate key={id} id={id} variant={variant} group={c.key} flipped={variant === 'feature' && c.order.slice(0, i).filter(([, v]) => v === 'feature').length % 2 === 1} />)}</div>
        </div>
      </section>
      {ci < pullQuotes.length && <section className="ed-quote"><blockquote className="wrap"><p>{pullQuotes[ci]}</p></blockquote></section>}
    </div>)}
    <section className="ed-colophon">
      <div className="wrap ed-colgrid">
        <div><h2>Made to sit on a bedside table.</h2><a className="btn" href="/guide">Set it up</a></div>
        <ul><li><b>Live, not recorded.</b> These are web redraws of the Android app’s screens, running on your clock. They are close, not pixel-identical.</li><li><b>Demos stay on the page.</b> Music, calls and timers are examples. They do not reach your phone, an account or an Android media session.</li><li><b>Weather asks first.</b> Location is optional, used only on request, and nothing is invented without it.</li><li><b>On a phone,</b> every face has a Full screen button that turns it sideways.</li></ul>
      </div>
    </section>
  </main>;
}
