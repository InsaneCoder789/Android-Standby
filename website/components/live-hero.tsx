'use client';
import { useState } from 'react';
import { Moon, Sun, Check, ArrowDown } from '@phosphor-icons/react';
import { LiveFace } from './live-face';
import { useLocalClock } from './local-clock';
const featured=[{name:'Float',frame:0,look:1},{name:'Digital',frame:0,look:0},{name:'Calendar',frame:1,look:1}];
export default function LiveHero(){
 const [face,setFace]=useState(0);const [night,setNight]=useState(false);const clock=useLocalClock();const current=featured[face];
 return <section className={`live-hero ${night?'live-hero-night':''}`} aria-labelledby="hero-title">
 <div className="live-hero-copy"><p className="eyebrow">A different side of your Android</p><h1 id="hero-title">Time, <span>well spent.</span></h1><p>Beautiful clocks. Your music. A little more presence.</p><a className="text-link" href="/screens">Find your favorite face <ArrowDown size={17}/></a></div>
 <div className="photographic-scene"><div className="photo-composition"><img className="bedside-photo" src="/scene/bedside.webp" alt="Titanium landscape phone on a brushed metal charging stand, beside a warm lamp and olive linen curtains" width="1672" height="941" fetchPriority="high"/><div className={`photo-live-screen ${night?'dim-screen':''}`}><LiveFace frame={current.frame} look={current.look}/></div></div><div className="photo-scene-caption"><span>Already on your time.</span><button className="photo-light-button" onClick={()=>setNight(!night)} aria-pressed={night} aria-label={night?'Switch to day preview':'Switch to dim preview'}>{night?<Sun size={19}/>:<Moon size={19}/>}</button></div></div>
 <div className="live-hero-bar"><div className="live-time-meta"><span className="live-status-dot"/><span>Live in {clock.timeZone.replaceAll('_',' ').split('/').at(-1)||'your timezone'}</span></div><div className="live-featured-selector" role="group" aria-label="Choose a featured clock">{featured.map((f,i)=><button key={f.name} aria-pressed={face===i} onClick={()=>setFace(i)}>{f.name}{face===i&&<Check size={14}/>}</button>)}</div><span className="hero-local-note">Your time. Your side of the world.</span></div>
 </section>;
}
