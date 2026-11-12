'use client';
import * as Accordion from '@radix-ui/react-accordion';
import { Plus, Minus } from '@phosphor-icons/react';
import { Hero, Reel } from './deck';
import { Header, Footer } from './chrome';
import { faqs } from '@/lib/faqs';
import { useLocalClock } from './local-clock';

export default function Experience() {
  const clock = useLocalClock();
  const time = clock.now ? new Intl.DateTimeFormat(clock.locale, { timeZone: clock.timeZone, hour: 'numeric', minute: '2-digit' }).formatToParts(clock.now).filter(p => p.type !== 'dayPeriod').map(p => p.value).join('').trim() : '';
  return <><Header /><main id="main">
    <Hero />
    <Reel />

    <section className="how" id="details" aria-labelledby="how-title">
      <div className="wrap">
        <h2 id="how-title">Three steps, then leave it alone.</h2>
        <ol className="how-steps">
          <li><h3>Make it yours</h3><p>Preview every look. Pick up to three faces and arrange them your way.</p></li>
          <li><h3>Give it a place</h3><p>Connect your charger. Rest the phone sideways on an inclined stand.</p></li>
          <li><h3>Settle in</h3><p>Swipe between favourites. Hold for brightness. Keep the essentials close.</p></li>
        </ol>
        <p className="fine">Standby runs only while the phone is charging, as Android’s system screensaver or through the optional Instant mode. Activation varies with device settings and manufacturer policies.</p>
      </div>
    </section>

    <section className="asks" id="questions" aria-labelledby="asks-title">
      <div className="wrap asks-grid">
        <h2 id="asks-title">Good to know.</h2>
        <Accordion.Root type="single" collapsible className="asks-list">{faqs.map(([q, a], i) => <Accordion.Item key={q} value={`q${i}`} className="ask"><Accordion.Header><Accordion.Trigger>{q}<span className="ask-plus"><Plus size={18} /></span><span className="ask-minus"><Minus size={18} /></span></Accordion.Trigger></Accordion.Header><Accordion.Content><p>{a}</p></Accordion.Content></Accordion.Item>)}</Accordion.Root>
      </div>
    </section>

    <section className="end" aria-labelledby="end-title">
      <div className="wrap">
        <p className="end-clock" aria-hidden="true" suppressHydrationWarning>{time || ' '}</p>
        <h2 id="end-title">A little less phone. A little more standby.</h2>
        <p className="end-acts"><a className="btn" href="/guide">Set it up</a><a className="link" href="/screens">See all faces</a></p>
        <p className="fine">Build from source. Android 9 and newer.</p>
      </div>
    </section>
  </main><Footer /></>;
}
