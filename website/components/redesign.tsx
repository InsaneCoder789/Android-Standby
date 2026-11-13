"use client";
/**
 * The redesigned look of every face, drawn live. This is a port of the Android app's ConceptFrames (same 480 x 222 drawing space,
 * same positions, sizes and colours), so the website shows what the phone shows: a dark screen, one accent, one big element,
 * letter-spaced labels, details in glass pills and 24-hour time.
 * What a browser cannot know is shown the way the app shows it when it does not know either: no alarm, sunrise and sunset at
 * 06:00 and 18:00 without a location, weather only after the visitor allows it. The charging ring is left out, as on the other web faces.
 */
import type { ReactNode } from "react";
import type { LocalClock } from "./local-clock";

/** How many looks each face had before the redesign; the redesign is the look after these (as in the app's Frames.BASE). */
export const BASE = [2, 3, 1, 2, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2];
export const hasRedesign = (frame: number) => frame < BASE.length;

const COND = "BarlowCondensed, Barlow Condensed, sans-serif", INK = "#fff", MUTED = "#8e8e93", GLASS = "#ffffff17", ROUND = "Nunito, ui-rounded, system-ui, sans-serif", UI = "Standby, Inter, system-ui, sans-serif", MONO = 'ui-monospace, "SF Mono", Menlo, "Roboto Mono", monospace';
const two = (v: number) => String(v).padStart(2, "0");
const hash = (i: number) => { const v = Math.sin(i * 12.9898) * 43758.545; return v - Math.floor(v); };
const rad = (deg: number) => deg * Math.PI / 180;
/** Cuts a line to fit a width, with an ellipsis, as the app does (about 0.56 of the type size per letter). */
const ell = (t: string, maxW: number, size: number) => { const n = Math.floor(maxW / (size * 0.56)); return t.length <= n ? t : t.slice(0, Math.max(1, n - 1)).trimEnd() + "…"; };

type TP = { x: number; y: number; size: number; fill?: string; font?: string; weight?: number; anchor?: "start" | "middle" | "end"; children: ReactNode; spacing?: string; glow?: string };
const T = ({ x, y, size, fill = INK, font = ROUND, weight = 800, anchor = "start", children, spacing, glow }: TP) => <text x={x} y={y} fontSize={size} fill={fill} fontFamily={font} fontWeight={weight} textAnchor={anchor} letterSpacing={spacing} style={{ fontVariantNumeric: "tabular-nums lining-nums", filter: glow ? `drop-shadow(0 0 8px ${glow})` : undefined }}>{children}</text>;
const Lab = ({ x, y, fill, anchor = "start", children }: { x: number; y: number; fill: string; anchor?: "start" | "middle" | "end"; children: string }) => <text x={x} y={y} fontSize={10} fontWeight={600} fontFamily={UI} letterSpacing="2.4" fill={fill} textAnchor={anchor}>{children.toUpperCase()}</text>;
const Pill = ({ x, y, w, h, size, fill = INK, children }: { x: number; y: number; w: number; h: number; size: number; fill?: string; children: string }) => <><rect x={x} y={y} width={w} height={h} rx={h / 2} fill={GLASS} /><text x={x + w / 2} y={y + h / 2 + size * 0.36} textAnchor="middle" fontSize={size} fontWeight={600} fontFamily={UI} fill={fill}>{children}</text></>;
const Stars = ({ n, seed, x0, x1, y0, y1, op }: { n: number; seed: number; x0: number; x1: number; y0: number; y1: number; op: number }) => <>{Array.from({ length: n }, (_, i) => <circle key={i} cx={x0 + (x1 - x0) * hash(i + seed)} cy={y0 + (y1 - y0) * hash(i + seed + 30)} r={0.7 + 1.1 * hash(i + seed + 60)} fill="#fff" opacity={op * (0.4 + 0.6 * hash(i + seed + 90))} />)}</>;
const Ticks = ({ cx, cy, r0, len, major, w, wMajor, color, colorMajor }: { cx: number; cy: number; r0: number; len: number; major: number; w: number; wMajor: number; color: string; colorMajor: string }) => <>{Array.from({ length: 60 }, (_, i) => { const a = rad(i * 6), l = i % 5 ? len : major; return <line key={i} x1={cx + Math.sin(a) * (r0 - l)} y1={cy - Math.cos(a) * (r0 - l)} x2={cx + Math.sin(a) * r0} y2={cy - Math.cos(a) * r0} stroke={i % 5 ? color : colorMajor} strokeWidth={i % 5 ? w : wMajor} strokeLinecap="round" />; })}</>;
/** An arc of an ellipse or circle, angles in degrees clockwise from 12 o'clock. */
const arc = (cx: number, cy: number, rx: number, ry: number, from: number, sweep: number) => { const p = (d: number) => `${cx + rx * Math.sin(rad(d))} ${cy - ry * Math.cos(rad(d))}`; const s = Math.min(359.9, Math.max(0.01, sweep)); return `M${p(from)}A${rx} ${ry} 0 ${s > 180 ? 1 : 0} 1 ${p(from + s)}`; };

const SEG = ["abcdef", "bc", "abdeg", "abcdg", "bcfg", "acdfg", "acdefg", "abc", "abcdefg", "abcdfg"];
const DOT = [["01110", "10001", "10011", "10101", "11001", "10001", "01110"], ["00100", "01100", "00100", "00100", "00100", "00100", "01110"], ["01110", "10001", "00001", "00010", "00100", "01000", "11111"], ["11110", "00001", "00001", "01110", "00001", "00001", "11110"], ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
  ["11111", "10000", "11110", "00001", "00001", "10001", "01110"], ["00110", "01000", "10000", "11110", "10001", "10001", "01110"], ["11111", "00001", "00010", "00100", "01000", "01000", "01000"], ["01110", "10001", "10001", "01110", "10001", "10001", "01110"], ["01110", "10001", "10001", "01111", "00001", "00010", "01100"]];
const describe = (code: number) => code === 0 ? "Clear" : code <= 2 ? "Partly cloudy" : code === 3 ? "Cloudy" : code <= 48 ? "Fog" : code <= 67 ? "Rain" : code <= 77 ? "Snow" : code <= 82 ? "Showers" : code <= 86 ? "Snow showers" : "Thunderstorm";
const PHASES = ["New Moon", "Waxing Crescent", "First Quarter", "Waxing Gibbous", "Full Moon", "Waning Gibbous", "Last Quarter", "Waning Crescent"];

type Music = { playing: boolean; seconds: number; length: number; interactive: boolean; toggle: () => void; reset: () => void };
type Extra = { wordGrid: string[]; wordLit: (h: number, m: number) => Set<string>; timer?: { remaining: number; duration: number } | null; music: Music };

export function Redesign({ frame, clock, extra }: { frame: number; clock: LocalClock; extra: Extra }) {
  const { now, locale, timeZone, weather, city: rawCity } = clock;
  const ms = now ? now.getTime() : 0, city = rawCity.replace(/ · timezone region$/, "");
  const zoned = (zone: string) => { const ps = now ? new Intl.DateTimeFormat("en-GB-u-nu-latn", { timeZone: zone, calendar: "gregory", year: "numeric", month: "numeric", day: "numeric", hour: "2-digit", minute: "2-digit", second: "2-digit", hourCycle: "h23" }).formatToParts(now) : []; const g = (t: string) => Number(ps.find(p => p.type === t)?.value ?? 0); return { h: g("hour"), m: g("minute"), s: g("second"), y: g("year") || 2026, mo: g("month") || 1, d: g("day") || 1 }; };
  const z = zoned(timeZone), hm = now ? `${two(z.h)}:${two(z.m)}` : "--:--", digits = now ? two(z.h) + two(z.m) : "0000";
  const fmt = (o: Intl.DateTimeFormatOptions) => now ? new Intl.DateTimeFormat(locale, { calendar: "gregory", ...o, timeZone }).format(now) : "";
  const part = (o: Intl.DateTimeFormatOptions) => now ? new Intl.DateTimeFormat(locale, { calendar: "gregory", ...o, timeZone }).formatToParts(now).find(p => p.type === Object.keys(o)[0])?.value ?? "" : "";
  const wd = part({ weekday: "short" }), wdLong = part({ weekday: "long" }), mon = part({ month: "short" }), monLong = part({ month: "long" });
  const dayShort = `${wd} ${z.d}`, dateShort = `${wd} ${z.d} ${mon}`;
  const temp = weather ? `${Math.round(weather.temperature)}°` : "--°";
  const ha = rad(((z.h % 12) + z.m / 60) * 30), ma = rad((z.m + z.s / 60) * 6), sa = rad(z.s * 6);
  const hand = (cx: number, cy: number, a: number, len: number, w: number, color: string, back = 0) => <line x1={cx - Math.sin(a) * back} y1={cy + Math.cos(a) * back} x2={cx + Math.sin(a) * len} y2={cy - Math.cos(a) * len} stroke={color} strokeWidth={w} strokeLinecap="round" />;
  const dayFrac = (z.h * 60 + z.m) / 1440, utc = Date.UTC(z.y, z.mo - 1, z.d), doy = Math.floor((utc - Date.UTC(z.y, 0, 1)) / 86400000) + 1;
  const daysInMonth = new Date(Date.UTC(z.y, z.mo, 0)).getUTCDate(), daysInYear = new Date(Date.UTC(z.y, 1, 29)).getUTCMonth() === 1 ? 366 : 365;
  let body: ReactNode = null, bg = "#000";

  switch (frame) {
    case 0: { const a = "#6cf55c"; body = <>
      <T x={190} y={148} size={112} anchor="middle">{hm}</T>
      <rect x={40} y={168} width={300} height={5} rx={2.5} fill="#ffffff18" /><rect x={40} y={168} width={Math.max(5, 300 * z.s / 60)} height={5} rx={2.5} fill={a} style={{ transition: z.s ? "width 1s linear" : "none" }} />
      <Lab x={40} y={58} fill={a}>{dateShort}</Lab><Pill x={372} y={84} w={84} h={34} size={15}>{dayShort.toUpperCase()}</Pill><Pill x={372} y={128} w={84} h={34} size={15}>{temp}</Pill></>; break; }
    case 1: { const a = "#ff9f0a", cx = 130, cy = 113, r = 86; body = <>
      <circle cx={cx} cy={cy} r={r + 8} fill="none" stroke="#1d2a4a" strokeWidth={5} /><path d={arc(cx, cy, r + 8, r + 8, 6 * 15, 12 * 15)} fill="none" stroke="#ffd36b" strokeOpacity={0.5} strokeWidth={5} strokeLinecap="round" />
      <Ticks cx={cx} cy={cy} r0={r} len={4} major={9} w={1} wMajor={2} color="#ffffff44" colorMajor="#fff" />
      {hand(cx, cy, ha, 44, 6, "#f2f2f7")}{hand(cx, cy, ma, 66, 4, "#f2f2f7")}{hand(cx, cy, sa, 66 * 0.96, 2, a, 14)}<circle cx={cx} cy={cy} r={5} fill={a} />
      <Lab x={270} y={62} fill={a}>{weather ? describe(weather.code) : "Today"}</Lab><T x={270} y={108} size={44}>{dayShort}</T>
      <Pill x={270} y={128} w={70} h={32} size={13}>{temp}</Pill><Pill x={348} y={128} w={96} h={32} size={12}>No alarm</Pill></>; break; }
    case 2: case 3: { const withClock = frame === 3, m = extra.music, a = withClock ? "#64d2ff" : "#ff5e8a", frac = m.seconds / m.length, mmss = (v: number) => `${Math.floor(v / 60)}:${two(v % 60)}`;
      const TITLE = "Evening, uninterrupted", ARTIST = "A moment to slow down";
      const art = (x: number, y: number, sz: number, id: string) => { const r = sz * (withClock ? 0.12 : 0.065); return <g><clipPath id={id}><rect x={x} y={y} width={sz} height={sz} rx={r} /></clipPath><g clipPath={`url(#${id})`}><rect x={x} y={y} width={sz} height={sz} fill="url(#rd-art)" /><circle cx={x + sz * 0.707} cy={y + sz * 0.293} r={sz * 0.176} fill="#ffc83d" /><circle cx={x + sz * 0.31} cy={y + sz * 0.76} r={sz * 0.32} fill="#1b1130" /></g></g>; };
      const btn = (cx: number, cy: number, r: number, fill: string, label: string, onClick: () => void, icon: ReactNode) => <g role="button" aria-label={label} onClick={m.interactive ? onClick : undefined} style={{ cursor: m.interactive ? "pointer" : "default" }}><circle cx={cx} cy={cy} r={r} fill={fill} />{icon}</g>;
      const skip = (x: number, y: number, r: number, left: boolean) => { const sg = left ? -1 : 1; return <g fill="#fff" stroke="#fff" strokeWidth={r * 0.26} strokeLinejoin="round"><path d={`M${x - sg * r * 0.75} ${y - r * 0.72}L${x + sg * r * 0.42} ${y}L${x - sg * r * 0.75} ${y + r * 0.72}z`} /><rect x={x + sg * r * 0.56 - r * 0.13} y={y - r * 0.72} width={r * 0.26} height={r * 1.44} rx={r * 0.1} strokeWidth={0} /></g>; };
      const play = (x: number, y: number, r: number, fill: string) => m.playing ? <g fill={fill}><rect x={x - r * 0.62} y={y - r * 0.75} width={r * 0.34} height={r * 1.5} rx={r * 0.14} /><rect x={x + r * 0.28} y={y - r * 0.75} width={r * 0.34} height={r * 1.5} rx={r * 0.14} /></g> : <path d={`M${x - r * 0.55 + r * 0.1} ${y - r * 0.75}L${x + r * 0.8} ${y}L${x - r * 0.55 + r * 0.1} ${y + r * 0.75}z`} fill={fill} stroke={fill} strokeWidth={r * 0.28} strokeLinejoin="round" />;
      const wavy = (sl: number, sr: number, sy: number, amp: number, wl: number, sw: number, kr: number, f: number) => { const kx = sl + (sr - sl) * f; let d = `M${sl} ${sy}`; for (let x = sl + 2; x < kx; x += 2) d += `L${x} ${sy + (m.playing ? amp * Math.sin(((x - sl) / wl) * 2 * Math.PI - m.seconds * 2.9) : 0)}`; return <><line x1={kx} y1={sy} x2={sr} y2={sy} stroke="#ffffff59" strokeWidth={sw} strokeLinecap="round" /><path d={d} fill="none" stroke="#fff" strokeWidth={sw} strokeLinecap="round" /><circle cx={kx} cy={sy} r={kr} fill="#fff" /></>; };
      if (withClock) {
        const ax = 240, ay = 35.5, aw = 218.4, H = 164.3, pad = H * 0.07, sz = H * 0.38, ax0 = ax + pad, ay0 = ay + pad, left = ax0 + sz + pad, right = ax + aw - pad, tw = right - left, sl = ax + pad, sr = ax + aw - pad, sy = ay + H * 0.62, cy = ay + H * 0.875, cx = (sl + sr) / 2, step = Math.min((sr - sl) * 0.27, H * 0.3), big = H * 0.078;
        body = <><defs><linearGradient id="rd-art" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stopColor="#d9395a" /><stop offset="1" stopColor="#2a4bd7" /></linearGradient><linearGradient id="rd-card" gradientUnits="userSpaceOnUse" x1={ax} y1={ay} x2={ax + aw} y2={ay + H}><stop offset="0" stopColor="#56468c" /><stop offset="1" stopColor="#6b5a22" /></linearGradient></defs>
          <T x={24} y={141} size={75.5} anchor="start">{hm}</T><Lab x={24} y={62} fill={a}>{dateShort}</Lab>
          <rect x={ax} y={ay} width={aw} height={H} rx={H * 0.09} fill="url(#rd-card)" stroke="#ffffff22" strokeWidth={H * 0.004} />
          {art(ax0, ay0, sz, "rd-artclip")}
          <T x={left} y={ay0 + sz * 0.3} size={H * 0.072} font={UI} weight={700}>{ell(TITLE, tw, H * 0.072)}</T><T x={left} y={ay0 + sz * 0.6} size={H * 0.052} fill="#ffffffb8" font={UI} weight={500}>{ell(ARTIST, tw, H * 0.052)}</T><T x={left} y={ay0 + sz * 0.9} size={H * 0.04} fill="#ffffff99" font={UI} weight={600}>Standby demo</T>
          {wavy(sl, sr, sy, H * 0.0105, H * 0.075, H * 0.0095, H * 0.0165, frac)}
          <T x={sl} y={sy + H * 0.075} size={H * 0.038} fill="#ffffffcc" font={UI} weight={500}>{mmss(m.seconds)}</T><T x={sr} y={sy + H * 0.075} size={H * 0.038} fill="#ffffffcc" font={UI} weight={500} anchor="end">{mmss(m.length)}</T>
          {btn(cx - step, cy, H * 0.06, "#ffffff00", "Restart demo", m.reset, skip(cx - step, cy, H * 0.034, true))}{btn(cx, cy, big, "#ffffff29", m.playing ? "Pause demo" : "Play demo", m.toggle, play(cx, cy, H * 0.044, "#fff"))}{btn(cx + step, cy, H * 0.06, "#ffffff00", "Next demo", m.reset, skip(cx + step, cy, H * 0.034, false))}</>;
      } else {
        const w = 480, h = 222, sz = Math.min(h * 0.7, w * 0.34), ax0 = w * 0.07, ay0 = (h - sz) / 2, left = ax0 + sz + w * 0.07, right = w - w * 0.07, pw = right - left, sy = h * 0.575, cy = h * 0.79, cx = (left + right) / 2, step = Math.min(pw * 0.27, h * 0.24), big = h * 0.078, sm = h * 0.062, pillW = step + sm + h * 0.035, pillH = big + h * 0.02;
        body = <><defs><linearGradient id="rd-art" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stopColor="#d9395a" /><stop offset="1" stopColor="#2a4bd7" /></linearGradient><linearGradient id="rd-mbg" gradientUnits="userSpaceOnUse" x1={0} y1={0} x2={w} y2={h}><stop offset="0" stopColor="#3d3470" /><stop offset="1" stopColor="#2b2210" /></linearGradient><radialGradient id="rd-mglow" gradientUnits="userSpaceOnUse" cx={w * 0.26} cy={h * 0.5} r={h * 0.95}><stop offset="0" stopColor="#fff" stopOpacity={0.09} /><stop offset="1" stopColor="#fff" stopOpacity={0} /></radialGradient><filter id="rd-shadow" x="-20%" y="-20%" width="140%" height="150%"><feDropShadow dx="0" dy={h * 0.015} stdDeviation={h * 0.02} floodColor="#000" floodOpacity={0.4} /></filter></defs>
          <rect width={w} height={h} fill="url(#rd-mbg)" /><rect width={w} height={h} fill="url(#rd-mglow)" />
          <g filter="url(#rd-shadow)">{art(ax0, ay0, sz, "rd-artclip")}</g>
          <T x={left} y={h * 0.15} size={h * 0.036} fill="#ffffffe6" font={UI} weight={600} spacing={`${h * 0.036 * 0.24}`}>{m.playing ? "NOW PLAYING  ·  DEMO" : "PAUSED  ·  DEMO"}</T>
          <T x={left} y={h * 0.3} size={h * 0.068} font={UI} weight={700}>{ell(TITLE, pw, h * 0.068)}</T><T x={left} y={h * 0.422} size={h * 0.045} fill="#ffffffc9" font={UI} weight={500}>{ell(ARTIST, pw, h * 0.045)}</T>
          {wavy(left, right, sy, h * 0.0105, h * 0.075, h * 0.0095, h * 0.0165, frac)}
          <T x={left} y={h * 0.638} size={h * 0.036} fill="#ffffffcc" font={UI} weight={500}>{mmss(m.seconds)}</T><T x={right} y={h * 0.638} size={h * 0.036} fill="#ffffffcc" font={UI} weight={500} anchor="end">{mmss(m.length)}</T>
          <rect x={cx - pillW} y={cy - pillH} width={pillW * 2} height={pillH * 2} rx={pillH} fill="#ffffff2e" />
          {btn(cx - step, cy, sm, "#ffffff00", "Restart demo", m.reset, skip(cx - step, cy, h * 0.036, true))}{btn(cx, cy, big, "#ffffffef", m.playing ? "Pause demo" : "Play demo", m.toggle, play(cx + (m.playing ? 0 : h * 0.004), cy, h * 0.046, "#24202a"))}{btn(cx + step, cy, sm, "#ffffff00", "Next demo", m.reset, skip(cx + step, cy, h * 0.036, false))}</>;
      }
      break; }
    case 4: body = <><defs><linearGradient id="rd-ph" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stopColor="#3a4a8a" /><stop offset=".6" stopColor="#e08a6a" /><stop offset="1" stopColor="#2a1a2a" /></linearGradient></defs>
      <rect width={480} height={222} fill="url(#rd-ph)" /><path d="M0 170l70-52 60 40 80-70 90 80 70-44 110 60v38H0z" fill="#1b1424" /><circle cx={360} cy={70} r={22} fill="#ffe0a8" />
      <rect x={26} y={150} width={236} height={48} rx={24} fill="#00000066" /><T x={48} y={184} size={30}>{hm}</T><T x={152} y={180} size={13} fill="#ffffffcc" font={UI} weight={500}>{`${dayShort} ${mon}`}</T>
      <T x={240} y={20} size={10} fill="#ffffffaa" font={UI} weight={500} anchor="middle">Sample photo. Yours show here in the app.</T></>; break;
    case 5: { const a = "#5ac8fa";
      const off = (zone: string) => now ? Math.round((Date.parse(now.toLocaleString("en-US", { timeZone: zone })) - Date.parse(now.toLocaleString("en-US", { timeZone }))) / 60000) : 0;
      const list: [string, string][] = ([["New York", "America/New_York"], ["London", "Europe/London"], ["Tokyo", "Asia/Tokyo"]] as [string, string][]).filter(([, zn]) => zn !== timeZone);
      list.push([city || timeZone.split("/").pop()!.replaceAll("_", " "), timeZone]); list.sort((p, q) => off(p[1]) - off(q[1]));
      body = <><Lab x={36} y={38} fill={MUTED}>{`Local ${hm}`}</Lab>{list.slice(0, 4).map(([name, zone], i) => { const x = 34 + (i % 2) * 212, y = 60 + Math.floor(i / 2) * 78, c = zoned(zone), hr = c.h + c.m / 60; return <g key={zone}>
        <rect x={x} y={y} width={200} height={66} rx={16} fill="#ffffff12" /><Lab x={x + 16} y={y + 22} fill={a}>{name}</Lab><T x={x + 16} y={y + 52} size={28}>{now ? `${two(c.h)}:${two(c.m)}` : "--:--"}</T>
        <rect x={x + 120} y={y + 40} width={64} height={6} rx={3} fill="#1d2a4a" /><circle cx={x + 120 + 64 * hr / 24} cy={y + 43} r={6} fill={hr >= 6 && hr < 18 ? "#ffd36b" : "#9fb4ff"} /></g>; })}</>; break; }
    case 6: { const a = "#ff9f0a"; body = <>{[0, 1, 2, 3].map(i => { const x = 42 + i * 98 + (i > 1 ? 14 : 0); return <g key={i}>
      <rect x={x} y={42} width={86} height={138} rx={14} fill="#1c1c20" /><path d={`M${x} 111V56a14 14 0 0 1 14-14h58a14 14 0 0 1 14 14v55z`} fill="#25252a" /><T x={x + 43} y={169} size={165} fill="#f2f2f7" font={COND} anchor="middle">{digits[i]}</T>
      <rect x={x} y={109.5} width={86} height={3} fill="#000" /><circle cx={x + 6} cy={111} r={3} fill="#000" /><circle cx={x + 80} cy={111} r={3} fill="#000" /></g>; })}
      <circle cx={232} cy={95} r={4} fill={a} /><circle cx={232} cy={125} r={4} fill={a} /></>; break; }
    case 7: { const a = "#ffd60a", lit = extra.wordLit(z.h, z.m), cols = extra.wordGrid[0].length, dx = 36, dy = 18.2, x0 = 240 - dx * (cols - 1) / 2;
      body = <>{extra.wordGrid.flatMap((row, r) => Array.from(row).map((ch, k) => { const on = !!now && lit.has(`${r}-${k}`); return <T key={`${r}-${k}`} x={x0 + k * dx} y={40 + r * dy + 6} size={17} fill={on ? a : "#2c2c30"} font={on ? ROUND : UI} weight={on ? 800 : 600} anchor="middle">{ch}</T>; }))}</>; break; }
    case 8: { const a = "#ffa30a", pitch = 15; body = <>{[0, 1, 2, 3].flatMap(k => { const ox = 52 + k * 96 + (k > 1 ? 24 : 0), g = DOT[Number(digits[k])]; return g.flatMap((row, r) => Array.from(row).map((b, col) => <circle key={`${k}-${r}-${col}`} cx={ox + col * pitch} cy={62 + r * pitch} r={b === "1" ? 6 : 1.8} fill={b === "1" ? a : "#ffffff2a"} />)); })}
      <circle cx={238} cy={92} r={5} fill={a} /><circle cx={238} cy={122} r={5} fill={a} /></>; break; }
    case 9: { const a = "#ff8a3c"; body = <>{[0, 1, 2, 3].map(i => { const x = 40 + i * 100 + (i > 1 ? 20 : 0); return <g key={i}>
      <rect x={x} y={34} width={74} height={150} rx={37} fill="#2a1408" /><rect x={x + 3} y={37} width={68} height={144} rx={34} fill="#120803" /><T x={x + 37} y={138} size={92} fill={a} anchor="middle" glow="#ff6a1a">{digits[i]}</T><rect x={x + 20} y={184} width={34} height={10} fill="#5a3418" /></g>; })}
      <circle cx={238} cy={96} r={5} fill={a} /><circle cx={238} cy={124} r={5} fill={a} /></>; break; }
    case 10: { const a = "#ff3b30", xs = [34, 126, 240, 332], w = 76, h = 128, t = 14, y = 40; body = <>{xs.map((x, i) => { const m = [[x + t, y, w - 2 * t, t], [x + w - t, y + t, t, h / 2 - t * 1.5], [x + w - t, y + h / 2 + t / 2, t, h / 2 - t * 1.5], [x + t, y + h - t, w - 2 * t, t], [x, y + h / 2 + t / 2, t, h / 2 - t * 1.5], [x, y + t, t, h / 2 - t * 1.5], [x + t, y + h / 2 - t / 2, w - 2 * t, t]];
      return m.map((v, k) => <rect key={`${i}-${k}`} x={v[0]} y={v[1]} width={v[2]} height={v[3]} rx={t / 2.2} fill={SEG[Number(digits[i])].includes("abcdefg"[k]) ? a : "#1d0707"} />); })}
      <circle cx={224} cy={76} r={7} fill={a} /><circle cx={224} cy={132} r={7} fill={a} /><Pill x={34} y={184} w={150} h={28} size={12} fill="#ff6b60">NO ALARM SET</Pill></>; break; }
    case 11: { const red = "#e5252a", cx = 150, cy = 113, r = 92; body = <>
      <circle cx={cx} cy={cy} r={r} fill="#f4f1ea" /><Ticks cx={cx} cy={cy} r0={r - 2} len={3} major={12} w={1.5} wMajor={4} color="#111" colorMajor="#111" />
      {hand(cx, cy, ha, 52, 8, "#111")}{hand(cx, cy, ma, 76, 5, "#111")}{hand(cx, cy, sa, 72, 2.5, red, 18)}<circle cx={cx + Math.sin(sa) * 72} cy={cy - Math.cos(sa) * 72} r={7} fill={red} /><circle cx={cx} cy={cy} r={4} fill={red} />
      <Lab x={290} y={86} fill={red}>Platform</Lab><T x={290} y={130} size={46} fill="#f4f1ea">{hm}</T><T x={290} y={156} size={14} fill={MUTED} font={UI} weight={500}>{fmt({ weekday: "long", day: "numeric", month: "long" })}</T></>; break; }
    case 12: { const a = "#5e5ce6", cx = 130, cy = 113, mo = rad(z.m * 6); body = <>
      {[94, 64, 34].map(r => <circle key={r} cx={cx} cy={cy} r={r} fill="none" stroke="#ffffff1a" />)}
      <circle cx={cx + Math.sin(ha) * 94} cy={cy - Math.cos(ha) * 94} r={9} fill={a} /><circle cx={cx + Math.sin(mo) * 64} cy={cy - Math.cos(mo) * 64} r={7} fill="#64d2ff" /><circle cx={cx + Math.sin(sa) * 34} cy={cy - Math.cos(sa) * 34} r={5} fill={INK} /><circle cx={cx} cy={cy} r={10} fill="#ff9f0a" />
      <Lab x={270} y={70} fill={a}>Orbit</Lab><T x={350} y={124} size={58} anchor="middle">{hm}</T><T x={270} y={152} size={14} fill={MUTED} font={UI} weight={500}>{`:${two(z.s)} seconds`}</T></>; break; }
    case 13: { const a = "#ff9f0a", rise = 6, set = 18, hr = z.h + z.m / 60, f = Math.max(0, Math.min(1, (hr - rise) / (set - rise))), up = hr >= rise && hr <= set, an = Math.PI + Math.PI * f, left = set - hr; body = <>
      <path d="M50 170A190 150 0 0 1 430 170" fill="none" stroke="#ffffff22" strokeWidth={3} />{up && <path d={arc(240, 170, 190, 150, -90, 180 * f)} fill="none" stroke={a} strokeWidth={4} strokeLinecap="round" />}
      <line x1={30} y1={170} x2={450} y2={170} stroke="#ffffff30" />{up && <circle cx={240 + 190 * Math.cos(an)} cy={170 + 150 * Math.sin(an)} r={14} fill="#ffd36b" style={{ filter: `drop-shadow(0 0 10px ${a})` }} />}
      <Lab x={50} y={194} fill={MUTED}>Sunrise</Lab><T x={50} y={212} size={16} font={UI} weight={600}>06:00</T><Lab x={430} y={194} fill={MUTED} anchor="end">Sunset</Lab><T x={430} y={212} size={16} font={UI} weight={600} anchor="end">18:00</T>
      <T x={240} y={132} size={44} anchor="middle">{hm}</T><T x={240} y={154} size={13} fill={MUTED} font={UI} weight={500} anchor="middle">{!up ? "The sun is down" : `${Math.floor(left)} h ${two(Math.floor((left % 1) * 60))} min of daylight left`}</T></>; break; }
    case 14: { const a = "#b4c4ff", ph = now ? ((((ms - Date.UTC(2000, 0, 6, 18, 14)) / 86400000) / 29.530588853) % 1 + 1) % 1 : 0, il = (1 - Math.cos(ph * 2 * Math.PI)) / 2, cx = 130, cy = 111, r = 76, waxing = ph < 0.5, rx = r * Math.abs(1 - 2 * il);
      const full = Math.round((((0.5 - ph) % 1 + 1) % 1) * 29.530588853); body = <>
      <circle cx={cx} cy={cy} r={r} fill="#1a1d2b" stroke="#3a4060" strokeWidth={1.5} /><path d={`M${cx} ${cy - r}A${r} ${r} 0 0 ${waxing ? 1 : 0} ${cx} ${cy + r}z`} fill="#e8ecf8" /><ellipse cx={cx} cy={cy} rx={rx} ry={r} fill={il > 0.5 ? "#e8ecf8" : "#1a1d2b"} />
      <Lab x={250} y={76} fill={a}>Tonight</Lab><T x={250} y={124} size={54}>{`${Math.round(il * 100)}%`}</T><T x={250} y={152} size={15} fill={MUTED} font={UI} weight={500}>{PHASES[Math.round(ph * 8) % 8]}</T>
      <Pill x={250} y={164} w={150} h={30} size={12}>{full <= 0 ? "Full moon tonight" : `Full moon in ${full} days`}</Pill><Stars n={16} seed={7} x0={200} x1={460} y0={20} y1={200} op={0.5} /></>; break; }
    case 15: { const col = ["#ff9f0a", "#30d158", "#64d2ff", "#bf5af2"], nm = ["DAY", "WEEK", "MONTH", "YEAR"], dow = (new Date(utc).getUTCDay() + 6) % 7;
      const v = [dayFrac, (dow + dayFrac) / 7, (z.d - 1 + dayFrac) / daysInMonth, (doy - 1 + dayFrac) / daysInYear]; body = <>{v.map((f, i) => { const y = 70 + i * 36; return <g key={i}>
        <Lab x={40} y={y + 4} fill={MUTED}>{nm[i]}</Lab><rect x={112} y={y - 8} width={260} height={16} rx={8} fill="#ffffff14" /><rect x={112} y={y - 8} width={Math.max(16, 260 * f)} height={16} rx={8} fill={col[i]} /><T x={392} y={y + 6} size={18}>{now ? `${Math.round(f * 100)}%` : "--%"}</T></g>; })}</>; break; }
    case 16: { const a = "#ff453a", week = Math.ceil((doy + ((new Date(Date.UTC(z.y, 0, 1)).getUTCDay() + 6) % 7)) / 7); body = <>
      <T x={34} y={186} size={178} weight={900} spacing="-5">{two(z.d)}</T><Lab x={290} y={92} fill={a}>{wdLong}</Lab><T x={290} y={130} size={30}>{monLong}</T>
      <T x={290} y={156} size={14} fill={MUTED} font={UI} weight={500}>{`Week ${week}  ·  Day ${doy}`}</T><Pill x={290} y={170} w={96} h={30} size={14}>{hm}</Pill></>; break; }
    case 17: { const a = "#5ac8fa"; if (!weather) { body = <><Lab x={44} y={36} fill={a}>Weather</Lab><T x={36} y={122} size={88}>--°</T><T x={40} y={160} size={14} fill={MUTED} font={UI} weight={500}>{clock.locationStatus === "loading" ? "Fetching the weather" : "Allow location to see local weather"}</T></>; break; }
      const hi = Math.round(weather.high), lo = Math.round(weather.low), t = Math.round(weather.temperature), f = hi === lo ? 0.5 : Math.max(0, Math.min(1, (t - lo) / (hi - lo))), wet = weather.code >= 51, cloud = weather.code >= 2; body = <>
      <defs><linearGradient id="rd-wx" x1="0" x2="1" y1="0" y2="0"><stop offset="0" stopColor="#5ac8fa" /><stop offset="1" stopColor="#ffb340" /></linearGradient></defs>
      <Lab x={44} y={36} fill={a}>{describe(weather.code)}</Lab><T x={36} y={122} size={88}>{`${t}°`}</T>
      {cloud ? <g fill={wet ? "#9fb4d8" : "#e6ebf5"}><circle cx={388} cy={96} r={16} /><circle cx={408} cy={88} r={20} /><circle cx={424} cy={100} r={13} /><rect x={376} y={96} width={60} height={16} rx={8} /></g>
        : <g><circle cx={400} cy={92} r={15} fill="#ffd36b" />{Array.from({ length: 8 }, (_, i) => <line key={i} x1={400 + Math.sin(rad(i * 45)) * 21} y1={92 - Math.cos(rad(i * 45)) * 21} x2={400 + Math.sin(rad(i * 45)) * 28} y2={92 - Math.cos(rad(i * 45)) * 28} stroke="#ffd36b" strokeWidth={3} strokeLinecap="round" />)}</g>}
      <rect x={40} y={172} width={400} height={8} rx={4} fill="url(#rd-wx)" /><circle cx={40 + 400 * f} cy={176} r={8} fill={INK} />
      <T x={40} y={204} size={15} fill={MUTED} font={UI} weight={600}>{`L ${lo}°`}</T><T x={440} y={204} size={15} fill={MUTED} font={UI} weight={600} anchor="end">{`H ${hi}°`}</T></>; break; }
    case 18: { const cols = ["#30d158", "#5ac8fa", "#bf5af2"], period = 2 * Math.PI * 60; body = <>
      <defs>{[0, 1, 2].map(k => <linearGradient key={k} id={`rd-au${k}`} gradientUnits="userSpaceOnUse" x1={0} x2={period} y1={0} y2={0} spreadMethod="repeat"><stop offset="0" stopColor={cols[k]} /><stop offset=".333" stopColor={cols[(k + 1) % 3]} /><stop offset=".666" stopColor={cols[(k + 2) % 3]} /><stop offset="1" stopColor={cols[k]} /></linearGradient>)}</defs>
      {/* Each ribbon is drawn one wavelength wider than the screen and slid left by exactly one wavelength, so it loops without a seam. */}
      <g className="rd-aurora">{[0, 1, 2].map(k => { const base = 100 + k * 36, amp = 34 - k * 6; let d = `M0 ${base}`; for (let x = 0; x <= 480 + period; x += 12) d += `L${x} ${base + amp * Math.sin(x / 60 + k * 1.4)}`; return <path key={k} d={`${d}L${480 + period} 222L0 222z`} fill={`url(#rd-au${k})`} opacity={(80 + k * 40) / 255} />; })}</g>
      <Stars n={20} seed={3} x0={0} x1={480} y0={10} y1={100} op={0.6} /><T x={240} y={108} size={68} anchor="middle">{hm}</T></>; break; }
    case 19: { const a = "#64d2ff", t = now ? Math.floor(ms / 1000) % 19 : 0, scale = t < 4 ? (t + 1) / 4 : t < 11 ? 1 : 1 - (t + 1 - 11) / 8, count = t < 4 ? 4 - t : t < 11 ? 11 - t : 19 - t, word = t < 4 ? "Breathe in" : t < 11 ? "Hold" : "Breathe out", e = scale * scale * (3 - 2 * scale);
      const disc = (r: number, k: number, fill: string) => <circle cx={240} cy={116} r={r} fill={fill} style={{ transform: `scale(${0.6 + k * e})`, transformOrigin: "240px 116px", transition: "transform 1s linear" }} />; body = <>
      {disc(84, 0.4, "#64d2ff14")}{disc(64, 0.4, "#64d2ff29")}{disc(44, 0.5, "#64d2ffe6")}<T x={240} y={124} size={26} fill="#032030" anchor="middle">{String(count)}</T>
      <Lab x={240} y={24} fill={a} anchor="middle">{word}</Lab><T x={240} y={214} size={13} fill={MUTED} font={UI} weight={500} anchor="middle">{`4 in  ·  7 hold  ·  8 out  ·  ${hm}`}</T></>; break; }
    case 20: { const g = "#30d158", offMin = now ? Math.round((Date.parse(now.toLocaleString("en-US", { timeZone })) - Date.parse(now.toLocaleString("en-US", { timeZone: "UTC" }))) / 60000) : 0, ao = Math.abs(offMin); body = <g style={{ whiteSpace: "pre" }}>
      <T x={36} y={62} size={14} fill={g} font={MONO} weight={400}>standby@phone ~ $ date</T><T x={36} y={106} size={44} fill={g} font={MONO} weight={700}>{`${hm}:${two(z.s)}`}</T>
      <T x={36} y={132} size={14} fill="#2a9d47" font={MONO} weight={400}>{`${wd} ${z.d} ${mon} ${z.y}  UTC${offMin < 0 ? "-" : "+"}${two(Math.floor(ao / 60))}:${two(ao % 60)}`}</T>
      <T x={36} y={166} size={14} fill={g} font={MONO} weight={400}>battery  --</T><T x={36} y={188} size={14} fill={g} font={MONO} weight={400}>{`weather  ${weather ? `${Math.round(weather.temperature)}  ${describe(weather.code).toLowerCase()}` : "--"}`}</T>
      {z.s % 2 === 0 && <rect x={36} y={198} width={9} height={16} fill={g} />}</g>; break; }
    case 21: { const hr = z.h + z.m / 60, night = hr < 5.5 || hr >= 20;
      const [top, bot, name] = hr < 5.5 ? ["#03060f", "#0a1730", "Night"] : hr < 8 ? ["#2a3a6a", "#e8a070", "Dawn"] : hr < 16.5 ? ["#3a7bd5", "#9fd0f5", "Day"] : hr < 19 ? ["#2a2a5a", "#e0705a", "Golden hour"] : hr < 20.5 ? ["#0a0f2e", "#8a3a4a", "Dusk"] : ["#03060f", "#0a1730", "Night"]; body = <>
      <defs><linearGradient id="rd-sky" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor={top} /><stop offset="1" stopColor={bot} /></linearGradient><mask id="rd-moon"><rect width={480} height={222} fill="#fff" /><circle cx={348} cy={52} r={17} fill="#000" /></mask></defs>
      <rect width={480} height={222} fill="url(#rd-sky)" />{(night || hr >= 19) && <Stars n={30} seed={11} x0={0} x1={480} y0={6} y1={130} op={0.8} />}
      {night ? <circle cx={340} cy={56} r={20} fill="#e8e0ff" mask="url(#rd-moon)" /> : <circle cx={340} cy={hr < 12 ? 86 - (hr - 6) * 8 : 40 + (hr - 12) * 8} r={20} fill="#ffe0a8" />}
      <path d="M0 200L60 174L130 194L220 164L300 188L390 170L480 192V222H0z" fill="#0d0a18" /><T x={130} y={128} size={72} anchor="middle">{hm}</T><Lab x={44} y={60} fill="#ffd0d8">{name}</Lab></>; break; }
    case 22: { const a = "#64d2ff", cx = 130, cy = 113, r = 78, tm = extra.timer, frac = tm ? tm.remaining / tm.duration : 0, clk = (s: number) => s >= 3600 ? `${Math.floor(s / 3600)}:${two(Math.floor(s / 60) % 60)}:${two(s % 60)}` : `${two(Math.floor(s / 60))}:${two(s % 60)}`; body = <>
      {Array.from({ length: 60 }, (_, i) => { const an = rad(i * 6), l = i % 5 ? 4 : 8; return <line key={i} x1={cx + Math.sin(an) * (r + 12)} y1={cy - Math.cos(an) * (r + 12)} x2={cx + Math.sin(an) * (r + 12 + l)} y2={cy - Math.cos(an) * (r + 12 + l)} stroke="#ffffff40" />; })}
      <circle cx={cx} cy={cy} r={r} fill="none" stroke="#ffffff14" strokeWidth={14} />{tm && frac > 0 && <path d={arc(cx, cy, r, r, 0, 360 * frac)} fill="none" stroke={a} strokeWidth={14} strokeLinecap="round" />}
      <T x={cx} y={cy + 10} size={36} anchor="middle">{tm ? clk(tm.remaining) : "--:--"}</T>{tm && <T x={cx} y={cy + 30} size={12} fill={MUTED} font={UI} weight={500} anchor="middle">{`of ${clk(tm.duration)}`}</T>}
      <Lab x={250} y={78} fill={a}>{tm && tm.remaining === 0 ? "Time's up" : "Timer"}</Lab>
      {tm ? <><T x={250} y={118} size={22} font={UI} weight={600}>{tm.remaining ? "Counting down" : "Finished"}</T><T x={250} y={142} size={13} fill={MUTED} font={UI} weight={500}>{`${Math.round(100 * frac)}% left`}</T></>
        : <><T x={250} y={112} size={22} font={UI} weight={600}>No timer running</T><T x={250} y={136} size={13} fill={MUTED} font={UI} weight={500}>Start one in the Clock app</T>{["1 min", "5 min", "10 min"].map((s, i) => <Pill key={s} x={250 + i * 70} y={156} w={64} h={30} size={12}>{s}</Pill>)}</>}</>; break; }
  }
  return <svg className="rd" viewBox="0 0 480 222" preserveAspectRatio="xMidYMid slice" role="img" aria-label={`Redesigned look, ${hm}`} style={{ background: bg }}>{body}</svg>;
}

/* ───────── Calls: a port of the app's CallFrame (ringing, and on a call) in the same 480 x 222 space ───────── */
const mix = (a: string, b: string, t: number) => { const p = (s: string) => [1, 3, 5].map(i => parseInt(s.slice(i, i + 2), 16)); const x = p(a), y = p(b); return "#" + x.map((v, i) => Math.round(v + (y[i] - v) * t).toString(16).padStart(2, "0")).join(""); };
// The app picks one of five scenes from the time of day (Morning, Sunset, Evening, Night, Midnight); this follows the same idea.
const SCENES: [string, string, string][] = [["#1c1207", "#080604", "#ff9f0a"], ["#1b0a14", "#4a1a10", "#ff7a45"], ["#0c0a1f", "#2b1745", "#b18cff"], ["#03060f", "#0a1730", "#7fb2ff"], ["#1a050b", "#040102", "#b3261e"]];
const sceneFor = (h: number) => h < 5 ? 4 : h < 11 ? 0 : h < 17 ? 1 : h < 21 ? 2 : h < 23 ? 3 : 4;
const ICON = {
  call: "M6.62 10.79c1.44 2.83 3.76 5.14 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z",
  end: "M12 9c-1.6 0-3.15.25-4.6.72v3.1c0 .39-.23.74-.56.9-.98.49-1.87 1.12-2.66 1.85-.18.18-.43.28-.7.28-.28 0-.53-.11-.71-.29L.29 13.08c-.18-.17-.29-.42-.29-.7 0-.28.11-.53.29-.71C3.34 8.78 7.46 7 12 7s8.66 1.78 11.71 4.67c.18.18.29.43.29.71 0 .28-.11.53-.29.71l-2.48 2.48c-.18.18-.43.29-.71.29-.27 0-.52-.11-.7-.28-.79-.73-1.68-1.36-2.66-1.85-.33-.16-.56-.51-.56-.9v-3.1C15.15 9.25 13.6 9 12 9z",
  mic: "M12 14c1.66 0 3-1.34 3-3V5c0-1.66-1.34-3-3-3S9 3.34 9 5v6c0 1.66 1.34 3 3 3zm5.91-3c-.49 0-.9.36-.98.85C16.52 14.2 14.47 16 12 16s-4.52-1.8-4.93-4.15c-.08-.49-.49-.85-.98-.85-.61 0-1.09.54-1 1.14.49 3 2.89 5.35 5.91 5.78V20c0 .55.45 1 1 1s1-.45 1-1v-2.08c3.02-.43 5.42-2.78 5.91-5.78.1-.6-.39-1.14-1-1.14z",
  vol: "M3 9v6h4l5 5V4L7 9H3zm13.5 3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-.73 2.5-2.25 2.5-4.02zM14 3.23v2.06c2.89.86 5 3.54 5 6.71s-2.11 5.85-5 6.71v2.06c4.01-.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z",
  bt: "M17.71 7.71L12 2h-1v7.59L6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 11 14.41V22h1l5.71-5.71-4.3-4.29 4.3-4.29zM13 5.83l1.88 1.88L13 9.59V5.83zm1.88 10.46L13 18.17v-3.76l1.88 1.88z",
};
const Glyph = ({ d, x, y, size, fill }: { d: string; x: number; y: number; size: number; fill: string }) => <path d={d} fill={fill} transform={`translate(${x - size / 2} ${y - size / 2}) scale(${size / 24})`} />;

export function Calls({ clock, incoming }: { clock: LocalClock; incoming: boolean }) {
  const { now, timeZone } = clock, W = 480, H = 222, ms = now ? now.getTime() : 0;
  const hour = now ? Number(new Intl.DateTimeFormat("en-GB", { timeZone, hour: "2-digit", hourCycle: "h23" }).format(now)) : 20;
  const sc = SCENES[sceneFor(hour)], th = sceneFor(hour), accent = incoming ? "#64d2ff" : "#30d158", tint = incoming ? "#10233a" : "#0b2418", glow = mix(accent, "#ffffff", 0.3);
  const ax = W * 0.235, ay = H * 0.5, x = W * 0.49, maxW = W * 0.44, secs = Math.floor(ms / 1000) % 3600, mmss = `${two(Math.floor(secs / 60))}:${two(secs % 60)}`;
  const bg = <>
    <defs>
      <linearGradient id="rd-cb" x1="0" x2="0" y1="0" y2="1"><stop offset="0" stopColor={mix(sc[0], tint, 0.5)} /><stop offset="1" stopColor={mix(sc[1], tint, 0.2)} /></linearGradient>
      <radialGradient id="rd-cg" gradientUnits="userSpaceOnUse" cx={ax} cy={ay} r={H * 0.85}><stop offset="0" stopColor={glow} stopOpacity={0.4} /><stop offset="1" stopColor={glow} stopOpacity={0} /></radialGradient>
      <mask id="rd-cm"><rect width={W} height={H} fill="#fff" /><circle cx={W * 0.86 + H * 0.045 * 0.5} cy={H * 0.13 - H * 0.045 * 0.2} r={H * 0.045 * 0.92} fill="#000" /></mask>
    </defs>
    <rect width={W} height={H} fill="url(#rd-cb)" /><rect width={W} height={H} fill="url(#rd-cg)" />
    {th <= 1 ? [[0.62, 0.03], [0.48, 0.05], [0.36, 0.08]].map(([r, a]) => <circle key={r} cx={W * 0.84} cy={H * 1.32} r={H * r} fill={sc[2]} opacity={a} />)
      : <>{th === 2 && <circle cx={W * 0.86} cy={H * 0.13} r={H * 0.045} fill="#e8e0ff" mask="url(#rd-cm)" />}
        {th === 3 && <><circle cx={W * 0.86} cy={H * 0.13} r={H * 0.045 * 1.9} fill="#dfe8ff" opacity={0.08} /><circle cx={W * 0.86} cy={H * 0.13} r={H * 0.045} fill="#dfe8ff" /></>}
        {th === 4 && <><circle cx={W * 0.86} cy={H * 0.13} r={H * 0.045 * 1.6} fill="#b3261e" opacity={0.14} /><circle cx={W * 0.86} cy={H * 0.13} r={H * 0.045 * 0.8} fill="#3a1216" /></>}
        {Array.from({ length: 26 }, (_, i) => { const fx = 0.02 + 0.96 * hash(i + 3), fy = 0.03 + 0.8 * hash(i + 31); return fx > 0.46 && fx < 0.97 && fy > 0.08 && fy < 0.92 ? null : <circle key={i} cx={W * fx} cy={H * fy} r={H * (0.0035 + 0.004 * hash(i + 50))} fill="#fff" opacity={0.15 + 0.45 * hash(i + 70)} />; })}</>}
  </>;
  const disc = (r: number) => <><circle cx={ax} cy={ay} r={r} fill="#30d158" /><text x={ax} y={ay + r * 0.3} textAnchor="middle" fontSize={r * 0.85} fontWeight={600} fontFamily={UI} fill="#fff">AS</text></>;
  const eyebrow = (s: string, y: number, size: number) => <text x={x} y={y} fontSize={size} fontWeight={600} fontFamily={UI} letterSpacing={`${size * 0.14}`} fill={accent}>{s}</text>;
  if (incoming) {
    const ar = H * 0.22, cy = H * 0.7, r = H * 0.078, tl = x + 2 * r + W * 0.014, tr = x + maxW, kr = r * 0.82, kx = tl + r;
    return <svg className="rd" viewBox="0 0 480 222" preserveAspectRatio="xMidYMid slice" role="img" aria-label="Incoming call from Aarav Sharma">{bg}
      {[[1.26, 0.5], [1.52, 0.26], [1.8, 0.12]].map(([k, a]) => <circle key={k} cx={ax} cy={ay} r={ar * k} fill="none" stroke={accent} strokeOpacity={a} strokeWidth={H * 0.004} />)}
      <circle cx={ax} cy={ay} fill="none" stroke={accent} strokeWidth={H * 0.004}><animate attributeName="r" values={`${ar * 1.05};${ar * 1.8}`} dur="2.4s" repeatCount="indefinite" /><animate attributeName="stroke-opacity" values="0.6;0" dur="2.4s" repeatCount="indefinite" /></circle>
      <g><circle cx={ax + ar * 1.52} cy={ay} r={H * 0.011} fill={accent} /><animateTransform attributeName="transform" type="rotate" from={`0 ${ax} ${ay}`} to={`360 ${ax} ${ay}`} dur="20.1s" repeatCount="indefinite" /></g>
      <g><circle cx={ax + ar * 1.8} cy={ay} r={H * 0.008} fill={accent} opacity={0.6} /><animateTransform attributeName="transform" type="rotate" from={`114 ${ax} ${ay}`} to={`-246 ${ax} ${ay}`} dur="32.7s" repeatCount="indefinite" /></g>
      {disc(ar)}{eyebrow("INCOMING  ·  MOBILE", H * 0.17, H * 0.04)}
      <T x={x} y={H * 0.3} size={H * 0.105} font={UI} weight={600}>Aarav Sharma</T><T x={x} y={H * 0.41} size={H * 0.048} fill="#ffffffcc" font={UI} weight={500}>+91 98765 43210  ·  SIM 1  ·  Airtel</T>
      <circle cx={x + r} cy={cy} r={r} fill="#ff3b30" /><Glyph d={ICON.end} x={x + r} y={cy} size={r * 1.05} fill="#fff" /><T x={x + r} y={cy + r + H * 0.05} size={H * 0.036} fill="#ffffffd9" font={UI} weight={500} anchor="middle">Decline</T>
      <rect x={tl} y={cy - r} width={tr - tl} height={2 * r} rx={r} fill="#30d158" fillOpacity={0.15} /><T x={(tl + tr) / 2 + kr * 0.5} y={cy + H * 0.016} size={H * 0.045} fill="#30d158" font={UI} weight={500} anchor="middle">Slide to answer</T>
      {[0, 1, 2].map(i => { const cx = tr - r * 1.05 - i * H * 0.03; return <path key={i} d={`M${cx - H * 0.007} ${cy - H * 0.016}L${cx + H * 0.01} ${cy}L${cx - H * 0.007} ${cy + H * 0.016}`} fill="none" stroke="#30d158" strokeOpacity={0.9 - i * 0.28} strokeWidth={H * 0.006} strokeLinecap="round" strokeLinejoin="round" />; })}
      <circle cx={kx} cy={cy} r={kr} fill="#30d158" /><Glyph d={ICON.call} x={kx} y={cy} size={kr * 1.05} fill="#fff" /></svg>;
  }
  const ar = H * 0.27, cy = H * 0.745, rr = H * 0.062, pad = H * 0.012, gap = H * 0.008, ty = H * 0.545, gl = x, gr = gl + 2 * pad + 6 * rr + 2 * gap, gt = cy - rr - pad, gb = cy + rr + pad, el = gr + W * 0.012, er = x + maxW, mid = (el + er) / 2;
  const wx = x + maxW * 0.36 + W * 0.014, step = (x + maxW - wx) / 20, bw = step * 0.55;
  return <svg className="rd" viewBox="0 0 480 222" preserveAspectRatio="xMidYMid slice" role="img" aria-label="On a call with Aarav Sharma">{bg}
    <circle cx={ax} cy={ay} r={ar * 1.1} fill="none" stroke={accent} strokeOpacity={0.9} strokeWidth={H * 0.008}><animate attributeName="r" values={`${ar * 1.098};${ar * 1.11};${ar * 1.098};${ar * 1.086};${ar * 1.098}`} dur="4s" repeatCount="indefinite" /></circle>
    {disc(ar)}{eyebrow("ON CALL", H * 0.165, H * 0.042)}
    <T x={x} y={H * 0.285} size={H * 0.11} font={UI} weight={600}>Aarav Sharma</T><T x={x} y={H * 0.385} size={H * 0.048} fill="#ffffffd9" font={UI} weight={500}>Mobile  ·  +91 98765 43210</T><T x={x} y={H * 0.385 + H * 0.065} size={H * 0.038} fill="#ffffff8c" font={UI} weight={500}>SIM 1  ·  Airtel</T>
    <T x={x} y={ty + H * 0.037} size={H * 0.105} fill="#fffffff2" weight={800}>{mmss}</T>
    {Array.from({ length: 20 }, (_, i) => { const amp = H * 0.01 + H * 0.07; return <rect key={i} className="rd-bar" x={wx + i * step} y={ty - amp} width={bw} height={amp * 2} rx={bw / 2} fill={accent} fillOpacity={0.9} style={{ animationDuration: `${0.7 + 0.5 * hash(i + 5)}s`, animationDelay: `${-hash(i + 9)}s` }} />; })}
    <rect x={gl} y={gt} width={gr - gl} height={gb - gt} rx={(gb - gt) / 2} fill={GLASS} />
    {[ICON.mic, ICON.vol, ICON.bt].map((d, i) => { const bx = gl + pad + rr + i * (2 * rr + gap); return <g key={i}><circle cx={bx} cy={cy} r={rr} fill={i === 2 ? "#ffffff0c" : "#ffffff14"} /><Glyph d={d} x={bx} y={cy} size={rr} fill={i === 2 ? "#ffffff59" : "#fff"} /></g>; })}
    <rect x={el} y={gt} width={er - el} height={gb - gt} rx={(gb - gt) / 2} fill="#ff3b30" /><Glyph d={ICON.end} x={mid - H * 0.075} y={cy} size={H * 0.062} fill="#fff" /><T x={mid + H * 0.035} y={cy + H * 0.018} size={H * 0.05} font={UI} weight={600} anchor="middle">End</T></svg>;
}
