# Standby frame design audit, 6 November 2026

Read-only source audit of ClockFrames.java, NewFrames.java, Frames.java; inspected stored previews FRAMES/00_0, 01_2, 02_0, 03_0, 06_0, 16_0, 17_1. These are stored captures, not evidence of the current phone state. No source or device modifications. All 23 catalogue entries were considered, but only the listed images were visually inspected.

## Confirmed design/geometry findings

1. **Digital geometry depends on the current time string.** ClockFrames.java:79-85 measures `t`, calculates horizontal scale from it, and may change font size below sx=.66. It then aligns using that string's ink. It bypasses the fixed-cell helper at :45-51. Thus the renderer does not guarantee fixed glyph size/pitch across time changes. The stored 00_0 preview shows aggressive tall compression and a time occupying almost all height. Empirical 11:11→22:58 screenshots were not generated in this audit. Remedy: design prototypes with 00:00, 11:11, 23:59 and one-digit hours side by side; choose a constant font size, fixed digit cells and fixed colon, reserve a fixed information column.
2. **Generic text fitting changes numeric size.** ClockFrames.java:30-35 shrinks text to the current string and ink-centres that current content. Called for dates and time strings throughout NewFrames.java. A one-digit date and a two-digit date can therefore have different fitted sizes where maxW binds; e.g. Calendar :211-216 and Big date NewFrames :338,345. Remedy: fit representative widest 2-digit date once; hold scale/baselines stable, allow only glyph change.
3. **Calendar hierarchy is a disconnected poster-plus-grid.** Stored FRAMES/01_2 confirms a huge isolated day on the left while the month is repeated above the grid and in the weekday/month header. Current ClockFrames :211-217 intentionally implements this. It is not clipped in that capture, but the date lacks a clear shared baseline/group with the grid. Remedy: one month/year heading, compact contextual full-date line, measured grid; either put today's numeral in the same heading group or omit the duplicate giant numeral.
4. **Flip card pair can rescale as digits change.** NewFrames :95-102 draws `digits` as a whole string through current-string fit, rather than two stable digit cells. Card geometry is fixed but numeral scale/pitch is not guaranteed. Remedy: fixed two-cell type, test 11/28/00/59; animate halves of the same cells.
5. **Sun-path geometry can overlap its information text.** NewFrames :262 sets arc apex at .20h; :273 places a .26h clock at .51h with width up to .396w. Sun near noon lies at .20h, so actual overlap is time-dependent, not proved in a screenshot. Labelled as a code-identified risk, not a visual reproduction.
6. **Bottom-safe-area policy is inconsistent.** ClockFrames class comment claims bottom 8% free, but widget grid can centre a last row at .90h (:199) and Terminal comp1 puts cursor at .96h (NewFrames :421); running timer labels at .925h (:502) and preset explanation at .925h (:464). Remedy: adopt a single content safe rectangle ending at .88h and separate bottom controls zone.
7. **Clock + Music repeats the variable time fit and boxed composition.** MusicFrame.java:106 uses current time through generic fit, stored FRAMES/03_0 shows long title and artist both ellipsized in a large rounded panel, leaving a large gap before progress. Prototype should align artwork/text/actions to explicit rows, reduce redundant panel framing, and test long title/artist and no-art states.

## Per-frame design review / prototype requirements

| ID / frame | Evidence and next prototype requirement |
|---|---|
| 0 Digital + Float | Digital instability mechanism confirmed above. Float ClockFrames :112-131 does use fixed digit advances and fixed `00:00` sizing; do not claim it has the same defect. One-digit-hour width changes by design at :121-123. Prototype deliberate blank first cell versus re-centred 12h format. |
| 1 Dial (Analog/Widgets/Calendar) | Calendar visual hierarchy finding above. Dial geometry is constrained by min(h*.44,w*.20) (:136), so no proven dial/sidebar collision. Prototype one shared dial box treatment and aligned information column; six-row months, long cities and missing weather. |
| 2 Music | Stored 02_0: strong artwork, but large vertical gap between metadata and progress and ring visually competes in top-right. Need live/paused/long-title/no-art composition variants; current implementation belongs MusicFrame/NowBar outside this source ownership. |
| 3 Clock and Music | Confirmed stored truncation and variable time fit above. Test both analog and digital components. |
| 4 Photos | Renderer outside audited files. Prototype light/busy image, portrait crop, no-photo state; text/ring must get a deliberate contrast zone. No new defect confirmed. |
| 5 World | ClockFrames :221-229 sizes dials by city count; labels independently shrink. Risk of inconsistent city type scale for long names; test 1/2/4 cities, DST offsets, next-day label. No observed clipping claimed. |
| 6 Flip | Current-string fit mechanism above; stored 06_0 shows strong card mass and a seam cutting through glyphs as intended, not a clipping bug. Prototype thinner typography and stable turn geometry. |
| 7 Word | NewFrames :111-119 grid fits from h, then information column starts .60w. At narrow aspect ratios the grid can approach that column; prototype 16:9 and 21:9 plus phrase changes. English-only content is an explicit product choice to document. |
| 8 Dot matrix | :135 pads first cell, fixed 27-column grid; numeric pitch stable by construction. Prototype readable dim dots/glow at low brightness, reserve date/alarm space rather than shrink whole strings. |
| 9 Nixie | :153-160 tube cells fixed; stable pitch. Individual digit rendering through T at :175 risks glyph-dependent fitting if width binds. Prototype all digits within one fixed tube aperture and seconds variant. |
| 10 Alarm LED | :185-196 fixed seven-segment cells and blank first cell: stable numeric geometry. Prototype AM/PM and alarm/date footer with consistent margins. |
| 11 Railway | :219 fixed dial; date side column at :229 uses huge date and independent fit, same hierarchy concern as Calendar. Prototype compact purposeful date block. |
| 12 Orbit | :248 centre time uses generic fitting; :250 giant date independent shrink. Risk of changing centre text width/scale; prototype 11:11 vs 23:59 inside the inner ring. |
| 13 Sun path | Time/arc risk noted above. Prototype sunrise/noon/sunset, polar and missing-location states before styling. |
| 14 Moon | :282-289 constant disc geometry; phase name/date can independently shrink. Prototype longest phase name and time alternative, maintain equal text hierarchy. No confirmed overflow. |
| 15 Time left | :315-.318 final progress bar centre=.93h, lower edge=.947h, violating stated .92 content boundary. Prototype four equal rhythm rows inside safe zone; percentages fixed-width. |
| 16 Big date | Stored 16_0 confirms oversized numeral competes with the weekday; generic date fitting differs 1 vs 28. Prototype fixed numeral area with full-date editorial heading; avoid duplicating Calendar. |
| 17 Weather | Stored 17_1 shows a very large time and isolated weather icon/temp. :366 time fits current string. Prototype fixed-width clock and shared temp/condition alignment; handle 3-digit and negative temperatures. |
| 18 Aurora | :381-382 current-string centred/left time fit; background geometry uses float epoch seconds (:374), a precision/jitter risk that needs motion testing. Prototype calm typography, fixed time cells and contrast at brightest background. |
| 19 Breathe | :395 remaining count changes via generic centred text; no max-width squeeze at normal 1..8 values. Avoid claiming digit layout unstable without testing. Prototype clear phase/count hierarchy, active/paused and reduced-motion states. |
| 20 Terminal | :408,417 time uses generic string fit; monospace advances help, but ink-left alignment can still vary with leading glyph ink. Footer at .96h confirmed. Prototype fixed content inset and stable monospace time origin. |
| 21 Sky | :433-434 generic current time sizing and alignment; brightness/contrast depends on time-of-day background. Prototype daylight/twilight/night with same fixed clock skeleton. |
| 22 Timer | :439-442 explicit cells stabilize within a fixed digit-count string; timer switches h*.19→.15 at one hour (:496) and changes length/total centring when formatting switches. Prototype 1:00:00→59:59 and 10:00→9:59 transitions intentionally; footer and status need safe zone. |

## Priorities for the image prototypes

1. Show fixed time geometry across numeric test states, not only attractive 9:41.
2. Establish a common content inset, bottom controls zone, hierarchy scale and visual charging-ring size; allow each face its own art direction inside that system.
3. Produce Digital, Calendar/Dial and Music first with real aspect ratio and long-data variants. User review precedes implementation.
4. Then extend to the remaining catalogue in coherent families (typographic, instrument, ambient, information). Do not imply 23 independently drawn styles require 23 unrelated design systems.

No claim of exhaustive defect detection, live-device reproduction or current screenshot parity is made.
