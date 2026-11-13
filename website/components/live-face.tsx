"use client";

import { useEffect, useState, type CSSProperties } from "react";
import { useLocalClock } from "./local-clock";
import { Sun, Cloud, CloudRain, Microphone, MicrophoneSlash, SpeakerHigh, BluetoothConnected, PhoneDisconnect, Phone } from "@phosphor-icons/react";
import { Redesign, Calls, hasRedesign } from "./redesign";
import "./live-face.css";

type Props = { frame: number; look?: number; compact?: boolean; interactive?: boolean };
type Vars = CSSProperties & Record<`--${string}`, string | number>;
const dotFont = ["01110100011001110101110011000101110", "00100011000010000100001000010001110", "01110100010000100010001000100011111", "11110000010000101110000010000111110", "00010001100101010010111110001000010", "11111100001111000001000011000101110", "00110010001000011110100011000101110", "11111000010001000100010000100001000", "01110100011000101110100011000101110", "01110100011000101111000010001001100"];
const WORD_GRID = ["ITLISASAMPM", "ACQUARTERDC", "TWENTYFIVEX", "HALFSTENFTO", "PASTERUNINE", "ONESIXTHREE", "FOURFIVETWO", "EIGHTELEVEN", "SEVENTWELVE", "TENSEOCLOCK"];
const W = { IT: [0, 0, 2], IS: [0, 3, 2], QUARTER: [1, 2, 7], TWENTY: [2, 0, 6], FIVE: [2, 6, 4], HALF: [3, 0, 4], TEN: [3, 5, 3], TO: [3, 9, 2], PAST: [4, 0, 4], OCLOCK: [9, 5, 6], A: [1, 0, 1] };
const WORD_HOURS = [[8, 5, 6], [5, 0, 3], [6, 8, 3], [5, 6, 5], [6, 0, 4], [6, 4, 4], [5, 3, 3], [8, 0, 5], [7, 0, 5], [4, 7, 4], [9, 0, 3], [7, 5, 6]];
/** The same rule as the Android WordClock: minutes round to five, then these runs of letters light. */
function wordLit(hour24: number, minute: number): Set<string> {
  const total = Math.floor((hour24 * 60 + minute + 2) / 5) % 288, step = total % 12, hour = Math.floor(total / 12);
  const runs = [W.IT, W.IS]; let shown = hour;
  switch (step) {
    case 0: runs.push(W.OCLOCK); break;
    case 1: runs.push(W.FIVE, W.PAST); break;
    case 2: runs.push(W.TEN, W.PAST); break;
    case 3: runs.push(W.A, W.QUARTER, W.PAST); break;
    case 4: runs.push(W.TWENTY, W.PAST); break;
    case 5: runs.push(W.TWENTY, W.FIVE, W.PAST); break;
    case 6: runs.push(W.HALF, W.PAST); break;
    case 7: runs.push(W.TWENTY, W.FIVE, W.TO); shown = hour + 1; break;
    case 8: runs.push(W.TWENTY, W.TO); shown = hour + 1; break;
    case 9: runs.push(W.A, W.QUARTER, W.TO); shown = hour + 1; break;
    case 10: runs.push(W.TEN, W.TO); shown = hour + 1; break;
    default: runs.push(W.FIVE, W.TO); shown = hour + 1;
  }
  runs.push(WORD_HOURS[shown % 12]);
  const lit = new Set<string>(); runs.forEach(([r, c, n]) => { for (let i = 0; i < n; i++) lit.add(`${r}-${c + i}`); }); return lit;
}
const pad2 = (n: number) => String(n).padStart(2, "0");
const words = ["IT IS HALF TEN", "QUARTER TWENTY", "FIVE MINUTES TO", "PAST TWO THREE", "ONE FOUR FIVE", "SIX SEVEN EIGHT", "NINE TEN ELEVEN", "TWELVE O CLOCK"];
const zoneCities = [["London", "Europe/London"], ["New York", "America/New_York"], ["Tokyo", "Asia/Tokyo"]];
function Dial({ hour, minute, second, railway = false, plain = false }: { hour: number; minute: number; second: number; railway?: boolean; plain?: boolean }) {
  return <div className={`lf-dial ${railway ? "lf-railway" : ""} ${plain ? "lf-plain" : ""}`} aria-label={`${hour}:${String(minute).padStart(2, "0")}`}>
    {Array.from({ length: 60 }, (_, i) => <i key={i} className={i % 5 ? "lf-tick" : "lf-tick lf-major"} style={{ "--angle": `${i * 6}deg` } as Vars} />)}
    {!railway && Array.from({ length: 12 }, (_, i) => <span className="lf-number" key={i} style={{ "--angle": `${(i + 1) * 30}deg`, "--reverse": `${-(i + 1) * 30}deg` } as Vars}>{i + 1}</span>)}
    <b className="lf-hand lf-hour" style={{ transform: `rotate(${hour * 30 + minute / 2}deg)` }} /><b className="lf-hand lf-minute" style={{ transform: `rotate(${minute * 6 + second / 10}deg)` }} /><b className="lf-hand lf-second" style={{ transform: `rotate(${second * 6}deg)` }} /><i className="lf-pin" />
  </div>;
}
function SevenSegments({ value }: { value: string }) { const masks = [63,6,91,79,102,109,125,7,127,111]; return <div className="lf-segments">{Array.from(value).map((ch,index)=>ch===":"?<div className="lf-segment-colon" key={index}><i/><i/></div>:<div className="lf-segment-digit" key={index}>{[0,1,2,3,4,5,6].map(segment=><i key={segment} className={`lf-seg lf-seg-${segment} ${ch.trim() === "" || Number.isNaN(Number(ch)) ? "" : masks[Number(ch)] & (1<<segment) ? "is-on" : ""}`}/>)}</div>)}</div>; }
function Matrix({ value }: { value: string }) { return <div className="lf-matrix">{value.split("").map((ch, i) => <div key={i} className={ch === ":" ? "lf-dot-colon" : "lf-dot-digit"}>{ch === ":" ? <><i /><i /></> : (ch.trim() === "" ? "0".repeat(35) : dotFont[Number(ch)] || "0".repeat(35)).split("").map((bit, n) => <i key={n} className={bit === "1" ? "is-on" : ""} />)}</div>)}</div>; }
export function LiveFace({ frame, look: lookIn = 0, compact = false, interactive = false }: Props) {
  // On faces that have one, the redesigned look comes first and the earlier looks follow it.
  const redo = hasRedesign(frame) && lookIn === 0, look = hasRedesign(frame) ? Math.max(0, lookIn - 1) : lookIn;
  const clock = useLocalClock();
  // The drawn looks compute SVG coordinates with Math.sin and Math.cos, whose last digits can differ between the server and the browser,
  // so they are drawn once the page has mounted. Until then an empty screen of the same shape holds the place.
  const [mounted, setMounted] = useState(false);
  useEffect(() => setMounted(true), []);
  const blank = <svg className="rd" viewBox="0 0 480 222" aria-hidden="true" />;
  const { now, locale, timeZone, city: rawCity, weather, locationStatus } = clock;
  const city = rawCity.replace(/ · timezone region$/, "");   // faces show just the place name, like the app
  const [playing, setPlaying] = useState(false);
  const [demoStartedAt, setDemoStartedAt] = useState<number | null>(null);
  const [demoPaused, setDemoPaused] = useState(0);
  const demoSeconds = Math.min(196, demoPaused + (playing && now && demoStartedAt !== null ? Math.max(0, Math.floor((now.getTime() - demoStartedAt) / 1000)) : 0));
  const toggleDemo = () => { if (playing) { setDemoPaused(demoSeconds); setDemoStartedAt(null); } else { setDemoStartedAt(now?.getTime() ?? Date.now()); if (demoSeconds >= 196) setDemoPaused(0); } setPlaying(!playing); };
  const resetDemo = () => { setPlaying(false); setDemoPaused(0); setDemoStartedAt(null); };
  const [deadline, setDeadline] = useState<number | null>(null);
  const [duration, setDuration] = useState(300);
  const fmt = (opts: Intl.DateTimeFormatOptions, zone = timeZone) => now ? new Intl.DateTimeFormat(locale, { calendar: "gregory", ...opts, timeZone: zone }).format(now) : "—";
  const parts = now ? new Intl.DateTimeFormat("en-US-u-nu-latn", { timeZone, calendar: "gregory", year: "numeric", month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23" }).formatToParts(now) : [];
  const get = (type: string, fallback = 0) => Number(parts.find(p => p.type === type)?.value ?? fallback);
  const hour = get("hour"), minute = get("minute"), second = get("second"), year = get("year", 2026), month = get("month", 1), day = get("day", 1);
  const localTimeParts = now ? new Intl.DateTimeFormat(locale, { timeZone, hour: "2-digit", minute: "2-digit", second: "2-digit" }).formatToParts(now) : [];
  const localPart = (kind: string) => localTimeParts.find(p => p.type === kind)?.value ?? "--";
  const hourParts = now ? new Intl.DateTimeFormat(locale, { timeZone, hour: "numeric", minute: "2-digit" }).formatToParts(now) : [];
  const hourText = hourParts.find(p => p.type === "hour")?.value ?? "--";
  const time = `${hourParts.some(p => p.type === "dayPeriod") ? hourText : hourText.padStart(2, "0")}:${localPart("minute")}`;
  const secondsTime = `${time}:${localPart("second")}`;
  const period = localTimeParts.find(p => p.type === "dayPeriod")?.value;
  const localeInfo = new Intl.Locale(locale) as Intl.Locale & { weekInfo?: { firstDay: number }; getWeekInfo?: () => { firstDay: number } };
  const weekStart = (localeInfo.getWeekInfo?.() ?? localeInfo.weekInfo)?.firstDay ?? 7;
  const date = fmt({ weekday: "long", day: "numeric", month: "long" });
  const weekday = fmt({ weekday: "long" });
  const monthName = fmt({ month: "long" });
  const fraction = (hour * 3600 + minute * 60 + second) / 86400;
  const days = new Date(Date.UTC(year, month, 0)).getUTCDate();
  const firstDay = (new Date(Date.UTC(year, month - 1, 1)).getUTCDay() - weekStart % 7 + 7) % 7;
  const dayOfYear = Math.floor((Date.UTC(year, month - 1, day) - Date.UTC(year, 0, 1)) / 86400000) + 1;
  const daysOfYear = new Date(Date.UTC(year, 1, 29)).getUTCMonth() === 1 ? 366 : 365;
  const moonPhase = now ? ((((now.getTime() - Date.UTC(2000, 0, 6, 18, 14)) / 86400000) / 29.530588853) % 1 + 1) % 1 : 0;
  const illumination = Math.round((1 - Math.cos(moonPhase * 2 * Math.PI)) * 50);
  const remaining = deadline && now ? Math.max(0, Math.ceil((deadline - Date.now()) / 1000)) : duration;
  const countdown = remaining >= 3600 ? `${Math.floor(remaining / 3600)}:${String(Math.floor(remaining % 3600 / 60)).padStart(2, "0")}:${String(remaining % 60).padStart(2, "0")}` : `${String(Math.floor(remaining / 60)).padStart(2, "0")}:${String(remaining % 60).padStart(2, "0")}`;
  const number = (value: number) => new Intl.NumberFormat(locale).format(value);
  const calendar = <div className="lf-calendar"><header>{monthName} <small>{now ? year : ""}</small></header><div className="lf-calendar-grid">{Array.from({ length: 7 }, (_, i) => <label key={i}>{new Intl.DateTimeFormat(locale, { weekday: "short", timeZone: "UTC" }).format(new Date(Date.UTC(2023, 0, 1 + weekStart % 7 + i)))}</label>)}{Array.from({ length: firstDay }, (_, i) => <span key={`empty-${i}`} />)}{Array.from({ length: days }, (_, i) => <span className={now && i + 1 === day ? "lf-today" : ""} key={i}>{now ? new Intl.NumberFormat(locale).format(i + 1) : "·"}</span>)}</div></div>;
  const dateBlock = <div className="lf-date-block"><span>{weekday}</span><strong>{now ? new Intl.NumberFormat(locale).format(day) : "—"}</strong><small>{monthName}</small></div>;
  const digital = <div className="lf-digital-time">{time}</div>;
  const music = <div className="lf-player"><div className="lf-album"><span>♫</span><small>Standby</small></div><div className="lf-track"><small>INTERACTIVE DEMO</small><h3>Evening, uninterrupted</h3><p>A moment to slow down</p><div className={`lf-equalizer ${playing && demoSeconds < 196 ? "is-playing" : ""}`}>{Array.from({ length: 7 }, (_, i) => <i key={i} style={{ "--delay": `${i * -.17}s` } as Vars} />)}</div><div className="lf-track-line"><i style={{ width: `${demoSeconds / 196 * 100}%` }} /></div><div className="lf-track-times"><span>{`${Math.floor(demoSeconds / 60)}:${String(demoSeconds % 60).padStart(2, "0")}`}</span><span>3:16</span></div><div className="lf-player-controls"><button disabled={!interactive} aria-label="Restart demo" onClick={resetDemo}>‹</button><button disabled={!interactive} className="lf-play" aria-label={playing ? "Pause demo" : "Play visual demo"} onClick={toggleDemo}>{playing ? "Ⅱ" : "▶"}</button><button disabled={!interactive} aria-label="Next demo" onClick={resetDemo}>›</button></div></div></div>;
  let content;
  if (redo) content = !mounted ? blank : <Redesign frame={frame} clock={clock} extra={{ wordGrid: WORD_GRID, wordLit, timer: deadline ? { remaining, duration } : null, music: { playing: playing && demoSeconds < 196, seconds: demoSeconds, length: 196, interactive, toggle: toggleDemo, reset: resetDemo } }} />;
  else switch (frame) {
    case 0: content = look ? <div className="lf-float">{(() => { let d = 0; return Array.from(time).map((ch, i) => ch === ":" ? <span key={i} className="lf-colon">:</span> : <span key={i} className={`lf-d${d++}`}>{ch}</span>); })()}</div> : <div className="lf-digital-layout">{digital}<aside><b><em>{fmt({ weekday: "short" }).toUpperCase()}</em> {now ? day : "—"}</b>{weather && <strong>{Math.round(weather.temperature)}°</strong>}</aside></div>; break;
    case 1: content = look === 2 ? <div className="lf-calendar-layout">{dateBlock}{calendar}</div> : <div className="lf-dial-layout"><Dial hour={hour} minute={minute} second={second} />{look === 1 ? calendar : <aside><h3>{city || weekday}</h3><p className="lf-redate">{fmt({ day: "numeric", month: "short" }).toUpperCase()}</p>{weather ? <><strong>{Math.round(weather.temperature)}°</strong><p>H:{Math.round(weather.high)}° &nbsp; L:{Math.round(weather.low)}°</p></> : <small>Allow location for live weather</small>}</aside>}</div>; break;
    case 2: content = music; break;
    case 3: content = <div className="lf-clock-player">{look ? digital : <Dial hour={hour} minute={minute} second={second} />}{music}</div>; break;
    case 4: content = <div className="lf-photo"><div>{digital}<p>{date}</p></div><small>Landscape preview</small></div>; break;
    case 5: {
      const dayKey = (zone: string) => now ? now.toLocaleDateString("en-CA", { timeZone: zone }) : "";
      const offsetMin = (zone: string) => now ? Math.round((Date.parse(now.toLocaleString("en-US", { timeZone: zone })) - Date.parse(now.toLocaleString("en-US", { timeZone }))) / 60000) : 0;
      const entries: [string, string][] = ([["New York", "America/New_York"], ["London", "Europe/London"], ["Tokyo", "Asia/Tokyo"]] as [string, string][]).filter(([, z]) => z !== timeZone);
      entries.push([city || timeZone.split("/").pop()!.replaceAll("_", " "), timeZone]); entries.sort((a, b) => offsetMin(a[1]) - offsetMin(b[1]));
      content = <div className="lf-world">{entries.map(([name, zone]) => {
        const ps = now ? new Intl.DateTimeFormat("en-GB", { timeZone: zone, hour: "2-digit", minute: "2-digit", hourCycle: "h23" }).formatToParts(now) : [];
        const off = offsetMin(zone), a = Math.abs(off), day = dayKey(zone) < dayKey(timeZone) ? "Yesterday" : dayKey(zone) > dayKey(timeZone) ? "Tomorrow" : "Today";
        const rel = off === 0 ? `${day}, same time` : `${day}, ${off < 0 ? "-" : "+"}${Math.floor(a / 60)}h${a % 60 ? ` ${a % 60}m` : ""}`;
        return <div key={zone}><Dial hour={Number(ps.find(p => p.type === "hour")?.value ?? 0)} minute={Number(ps.find(p => p.type === "minute")?.value ?? 0)} second={0} plain /><h3>{name}</h3><p>{fmt({ hour: "numeric", minute: "2-digit" }, zone)}</p><small>{rel}</small></div>;
      })}</div>; break;
    }
    case 6: content = <div className={`lf-flip-layout ${look ? "lf-with-date" : ""}`}><div className="lf-flip"><span>{period && <em>{period}</em>}{localPart("hour")}</span><span>{localPart("minute")}</span></div>{look > 0 && <p>{date}</p>}</div>; break;
    case 7: {
      const lit = wordLit(hour, minute);
      const grid = <div className="lf-word-grid" aria-label={`English word clock, approximately ${time}`}>{WORD_GRID.flatMap((row, r) => Array.from(row).map((ch, c) => <span key={`${r}-${c}`} className={now && lit.has(`${r}-${c}`) ? "is-lit" : ""}>{ch}</span>))}</div>;
      content = <div className={`lf-word-layout ${look ? "lf-split" : ""}`}>{grid}{look > 0 && dateBlock}</div>; break;
    }
    case 8: content = <div className="lf-matrix-layout"><Matrix value={now ? `${String(period ? hour % 12 || 12 : hour).padStart(2, period ? " " : "0")}:${String(minute).padStart(2, "0")}` : "00:00"} />{look > 0 && <p>{date}</p>}</div>; break;
    case 9: content = <div className="lf-nixie">{(now ? `${String(period ? hour % 12 || 12 : hour).padStart(2, "0")}${String(minute).padStart(2, "0")}${look ? String(second).padStart(2, "0") : ""}` : look ? "------" : "----").split("").flatMap((digit, i) => [<span key={i}>{digit}<i /></span>, ...(i % 2 === 1 && i < 5 && (look || i < 3) ? [<b key={`c${i}`} className="lf-nx-colon"><i /><i /></b>] : [])])}</div>; break;
    case 10: content = <div className="lf-led"><SevenSegments value={now ? `${String(period ? hour % 12 || 12 : hour).padStart(2, period ? " " : "0")}:${String(minute).padStart(2, "0")}` : "--:--"} />{look > 0 && <p>{date}</p>}</div>; break;
    case 11: content = <div className={`lf-rail-layout ${look ? "lf-split" : ""}`}><Dial hour={hour} minute={minute} second={second} railway />{look > 0 && dateBlock}</div>; break;
    case 12: content = <div className={`lf-orbit-layout ${look ? "lf-split" : ""}`}><div className="lf-orbits">{[hour % 12 / 12, minute / 60, second / 60].map((f, i) => <div key={i} className={`lf-orbit lf-orbit-${i}`} style={{ "--progress": `${f * 360}deg` } as Vars}><i style={{ transform: `rotate(${f * 360}deg)` }} /></div>)}<b>{time}</b></div>{look > 0 && dateBlock}</div>; break;
    case 13: content = <div className="lf-sun"><small>LOCAL DAY PROGRESS</small><div className="lf-sun-arc"><i style={{ transform: `rotate(${-90 + fraction * 180}deg)` }} /></div><div className="lf-sun-time">{look ? time : `${Math.round(fraction * 100)}% of today`}</div><footer><span>00:00</span><span>24:00</span></footer><p>Illustrative day progress · not sunrise or sunset</p></div>; break;
    case 14: content = <div className="lf-moon-layout"><div className="lf-moon" style={{ "--phase": `${illumination}%`, transform: moonPhase < .5 ? "none" : "scaleX(-1)" } as Vars}><i /></div><aside><h3>{look ? time : illumination > 98 ? "Full moon" : illumination < 2 ? "New moon" : moonPhase < .5 ? "Waxing moon" : "Waning moon"}</h3><p>{now ? `${illumination}% illuminated` : "—"}</p><small>Approximate lunar phase</small></aside></div>; break;
    case 15: { const fractions = [fraction, (((new Date(Date.UTC(year, month - 1, day)).getUTCDay() - weekStart % 7 + 7) % 7) + fraction) / 7, (day - 1 + fraction) / days, (dayOfYear - 1 + fraction) / daysOfYear]; content = look ? <div className="lf-year"><header><b>{now ? year : "—"}</b><span>Day {now ? number(dayOfYear) : "—"} of {daysOfYear}</span></header><div>{Array.from({ length: daysOfYear }, (_, i) => <i key={i} className={now && i < dayOfYear ? "is-past" : ""} />)}</div><small>Each dot is a day</small></div> : <div className="lf-progress">{["Today", "This week", "This month", "This year"].map((name, i) => <div key={name}><header><span>{name}</span><b>{now ? Math.round(fractions[i] * 100) : "—"}%</b></header><div><i style={{ width: `${now ? fractions[i] * 100 : 0}%` }} /></div></div>)}</div>; break; }
    case 16: content = <div className={`lf-bigdate ${look ? "lf-bigdate-split" : ""}`}><strong>{look ? fmt({ weekday: "short" }) : now ? day : "—"}</strong><aside>{look ? <b>{now ? number(day) : "—"}</b> : <h3>{weekday}</h3>}<p>{fmt({ month: "long", year: "numeric" })}</p>{!look && <small>Day {now ? number(dayOfYear) : "—"} of {daysOfYear}</small>}</aside></div>; break;
    case 17: content = <div className="lf-weather">{look ? digital : <div className="lf-weather-icon">{weather ? weather.code >= 51 ? <CloudRain weight="fill" /> : weather.code >= 2 ? <Cloud weight="fill" /> : <Sun weight="fill" /> : <Cloud weight="fill" />}</div>}<aside><small>{city || "Your weather"}</small>{weather && <strong>{Math.round(weather.temperature)}°</strong>}<p>{weather ? `H:${Math.round(weather.high)}°  L:${Math.round(weather.low)}°` : "Weather unavailable"}</p><small>{weather ? date : locationStatus === "loading" ? "Finding your location…" : "Enable location for local conditions"}</small></aside></div>; break;
    case 18: content = <div className={`lf-aurora ${look ? "lf-ambient-side" : ""}`}>{digital}{look > 0 && <aside><h3>{weekday}</h3><p>{fmt({ day: "numeric", month: "long" })}</p></aside>}</div>; break;
    case 19: { const total = look ? 16 : 19, t = now ? Math.floor(now.getTime() / 1000) % total : 0; const phase = look ? t < 4 ? "Breathe in" : t < 8 ? "Hold" : t < 12 ? "Breathe out" : "Hold" : t < 4 ? "Breathe in" : t < 11 ? "Hold" : "Breathe out"; const growth = look ? t<4?t/4:t<8?1:t<12?1-(t-8)/4:0 : t<4?t/4:t<11?1:1-(t-11)/8; content = <div className={`lf-breathe ${look ? "lf-breathe-box" : ""}`} style={{"--breath-scale": .65 + growth*.35} as Vars}><h3>{phase}</h3><div><i /><strong>{look ? 4 - t % 4 : t < 4 ? 4 - t : t < 11 ? 11 - t : 19 - t}</strong></div><p>{look ? "Box breathing · 4 · 4 · 4 · 4" : "4 · 7 · 8"}</p></div>; break; }
    case 20: {
      const pad = (n: number) => String(n).padStart(2, "0"), host = "standby@phone", t24 = `${pad(hour)}:${pad(minute)}:${pad(second)}`;
      const statusTime = now ? new Intl.DateTimeFormat(locale, { timeZone, hour: "2-digit", minute: "2-digit", second: "2-digit" }).format(now) : "--:--:--";
      content = look ? <div className="lf-terminal lf-term-status"><h2>{host}</h2><dl>{[["time", statusTime], ["date", fmt({ weekday: "short", day: "numeric", month: "short", year: "numeric" })], ["zone", timeZone], ["weather", weather ? `${Math.round(weather.temperature)}°` : "unavailable"]].map(([k, v]) => <div key={k}><dt>{k}</dt><dd>{v}</dd></div>)}</dl><p>{host} ~ %</p></div>
        : <div className="lf-terminal"><p>{host} ~ % date +%T</p><strong>{t24}</strong><p>{host} ~ % date +"%A, %d %B"</p><h3>{weekday}, {pad(day)} {monthName}</h3><p>{host} ~ % <span className="lf-cursor">▌</span></p></div>; break;
    }
    case 21: content = <div className={`lf-sky ${look ? "lf-ambient-side" : ""}`} style={{ "--sky-top": hour < 6 || hour > 19 ? "#090d27" : "#214770", "--sky-bottom": hour < 6 || hour > 19 ? "#352441" : "#a86c56" } as Vars}>{digital}{look ? <aside><h3>{weekday}</h3><p>{fmt({ day: "numeric", month: "long" })}</p></aside> : <p>{date}</p>}</div>; break;
    case 22: {
      const presets: [number, string, number][] = [[1, "min", 60], [3, "min", 180], [5, "min", 300], [10, "min", 600], [15, "min", 900], [30, "min", 1800], [45, "min", 2700], [1, "hour", 3600]];
      content = deadline ? <div className={`lf-timer ${look ? "lf-timer-digital" : ""}`}><small>{remaining ? "TIMER" : "TIME’S UP"}</small><div className="lf-timer-ring" style={{ "--progress": `${remaining / duration * 360}deg` } as Vars}><strong>{countdown}</strong></div>{interactive ? <div className="lf-timer-actions"><button onClick={() => setDeadline(null)}>Cancel timer</button></div> : <p>Choose a duration in the interactive preview</p>}</div>
        : <div className="lf-timer lf-timer-presets"><em className="lf-timer-clock">{time}</em><small>START A TIMER</small><div className="lf-timer-grid">{presets.map(([n, u, sec]) => <button key={sec} disabled={!interactive} tabIndex={interactive ? 0 : -1} onClick={() => { setDuration(sec); setDeadline(Date.now() + sec * 1000); }}><b>{n}</b><span>{u}</span></button>)}</div><p>Website demo · countdown only, no sound</p></div>; break;
    }
    case 23: content = mounted ? <Calls clock={clock} incoming={look === 0} /> : blank; break;
    default: content = digital;
  }
  return <div className={`lf-root lf-face lf-frame-${frame} lf-look-${look} ${compact ? "lf-compact" : ""} ${interactive ? "lf-interactive" : ""}`} aria-label={`Live ${timeZone} preview`}><div className="lf-content">{content}{period && [10].includes(frame) && !redo && <span className="lf-period">{period}</span>}</div></div>;
}
