# Standby frame art direction

Scope: design proposals derived from the current 23-entry Frames catalogue, MusicFrame/NewFrames code, FINAL2 music preview and FRAMES orbit preview. No source changes. Preserve frame identities and data semantics.

## Common system

A bedside object seen from 1–3 metres, often in dim light. Backgrounds are immersive; data is calmly anchored. Maintain three deliberate typographic voices: condensed tabular display digits for time, soft sans for controls/data, one restrained serif for editorial date/photo treatments. Each frame chooses one display voice, never all three at once. Use real tabular numerals or fixed per-digit cells; reserve colon width; never independently autosize every timestamp. Measure the widest supported time and reserve its rectangle. Do not change font size at minute rollover.

Layout is a landscape stage, not two giant equal cards. Reserve camera/cutout safety for information while allowing backgrounds full bleed. Keep 5–7% horizontal and 9–12% vertical information margins. Frame family may have one hero and one supporting cluster; avoid an obligatory sidebar. Bottom mode bar overlays temporarily, with a scrim and 200 ms eased slide/fade, matching hit geometry. It disappears on inactivity; no permanent dock. A shared charging glyph is a transient status indicator rather than a large green decorative logo on every page.

Minimum contrast 4.5:1 supporting text, 3:1 large display text; no ultra-thin primary digits. Decorative strokes can be dimmer. Distinguish unavailable/stale data from real readings. Respect locale, 12/24h, RTL and long city/artist labels. Reduced motion freezes ambient effects, keeping all information visible. Night mode suppresses photo luminosity and uses restrained red data. Burn-in shifts must not move data into clipping/cutout bounds. Motion uses monotonic time and a fixed data anchor; no constantly wobbling digits.

## 23 frames

### 0 Digital — Precision
Hero: a single generous line of subtly condensed time, optically centred in the usable canvas. One consistent cap height with a quieter fixed colon. Colour choice applies to the digits, not random labels. Component 0 is pure time; component 1 Float uses soft overlapping colour planes behind individual numeral silhouettes while keeping glyph positions stable. No date sidebar forced onto both components.

### 1 Dial — Instrument
Hero: an open circular dial with fine minute ticks and clear cardinal numbers. Use ivory hour/minute hands and a single amber seconds hand. Component Analog owns the centre; Widgets offsets the dial left with a small weather/date cluster, never another equally dominant clock. Calendar retains one dial plus a compact month grid whose last row is guaranteed visible. A six-row month determines fixed grid metrics. Current day sits optically centred in its highlight, not baseline-aligned to a circle edge.

### 2 Music — Colour field
Artwork is a prominent left anchor (about 0.70h square) and the right panel is a single aligned information stack: title, artist, app, seek rail/times, controls. Keep app badge subordinate. Sample artwork into a dark full-bleed colour field; don't add unrelated rainbow glow. Title can occupy two measured lines, artist one; stable controls cannot jump with title length. Times sit directly below rail edges; elapsed wave is subtle and never masquerades as measured audio amplitude. Playback button visually stronger than skip controls. Correctly label live streams without fictitious duration.

### 3 Clock and Music — Duet
Clock remains large and unboxed on the left; the player on the right is intentionally compact, with a smaller cover, title and three controls. The two components are clock-first and artwork-first, not duplications of Music. The Now Bar can span the lower third but must not collide with time. One shared tonal field visually joins both halves.

### 4 Photos — Gallery
One photo full bleed, with a locally positioned readable time over its low-detail region. Use a broad subtle scrim rather than an opaque top strip. Crop uses focal point when available; no face under a clock by default. Date is a short secondary line. Crossfades only at photo changes; do not pan endlessly. Empty state presents an elegant preview silhouette and a clearly labelled photo selection action.

### 5 World — Meridian
Two to four city clocks in a disciplined horizontal rhythm. Local clock is strongest; other zones are equal peers. City, time and day offset form one cluster, with long names bounded. Day/night indicated by a small disc and text, not a large decorative globe. Component alternatives can be a two-city split and a four-city strip, retaining fixed digit widths.

### 6 Flip — Mechanical
Two tactile hour/minute leaves with restrained seam, hinge and small depth. Digits fill the leaves but leave seam breathing room. Avoid thick black borders and huge drop shadows. First component is the object; second adds a low date line. Flip only changed leaves, from the actual previous value to current; no replay on every redraw. Reduce motion updates instantly.

### 7 Word clock — Quiet language
Keep the original letter-grid identity with precise equal character cells and carefully balanced lit/unlit contrast. Do not make the grid a cramped square with a giant unrelated date beside it. Component 1 can pair a smaller grid with a simple written-time phrase; inactive letters should be subdued yet visible. Minute remainder dots are anchored and explained in customization.

### 8 Dot matrix — Signal
Large modular luminous dot glyphs on deep charcoal. Dot pitch and diameter are derived from a fixed full-time matrix so 11:11 and 23:59 share scale. Glow is soft and limited; hard dot centres carry readability. Components choose pure time or time with a slim bottom date strip. No scattered dots as background decoration.

### 9 Nixie — Ember
Warm amber digits within barely visible glass envelopes, dark inactive cathode traces, understated ceramic bases. Glass shape must not cage the digits in fat cards. Four evenly spaced tubes are a single instrument. Second component adds date/alarm below rather than shrinking every tube. Stable tube bounds and restrained luminance in night mode.

### 10 Alarm LED — Bedside classic
True seven-segment construction with a subtle unlit-segment scaffold. Wide centred time in red or chosen accent, one small AM/PM marker, optional alarm status beneath. Components offer pure clock and utility view. Geometry remains fixed for every numeral and no font substitution approximates segments.

### 11 Railway — Station
Off-white hands and tick marks, clear red seconds paddle, black full-bleed face. Main component is nearly architectural and centred. Secondary component offsets the dial with restrained date typography. Keep the seconds movement smooth only when animation is enabled; display the correct instant after sleep/resume.

### 12 Orbit — Celestial instrument
Three slender tracks with distinct hour/minute/second bodies, not thick activity rings. Main time is a quiet centre anchor. Second component can place date outside the orbit at a carefully sized secondary scale; eliminate the current oversized weekday/date sidebar. Use one warm body, one cool body and neutral seconds; a small legend makes the encoded values legible.

### 13 Sun path — Horizon
An open horizon arc stretches across the lower two thirds, with the sun located from real solar time. Current time sits above the horizon with sunrise/sunset at endpoints. Daylight/night regions are subtle fills. Second component uses a taller portrait-like arc crop with the same data. Missing coordinates show setup; polar day/night gets explicit wording rather than a fabricated arc.

### 14 Moon — Nocturne
A luminous phase disc with restrained physically plausible terminator, positioned with generous negative space. Date/phase name are small companions; time remains easily readable. Component 0 is moon-first, component 1 time-first with a smaller phase disc. No giant decorative stars behind text. Phase labels and illumination use the same calculation.

### 15 Time left — Measure
Four deliberately thin progress tracks for day, week, month and year, grouped with labels and percentages. Main component treats them as a rhythmic four-line composition; alternative uses one large selected period arc. Avoid rainbow dashboard heaviness. Labels clarify elapsed versus remaining consistently; use the frame title's semantics and never reverse meaning between variants.

### 16 Big date — Date poster
One enormous date numeral with a carefully placed month word and weekday. Use asymmetry deliberately: date left-weighted, compact month grid or week strip aligned right. Do not leave a huge isolated numeral low in the corner. The headline and grid share an optical vertical centre. Component 1 may stack month/day as a typographic poster without a second date duplication.

### 17 Weather — Atmosphere
Large current temperature over a controlled tonal sky field, with location and short conditions directly adjacent. High/low and next update remain legible but subordinate. Secondary component exposes a concise forecast strip, not a second giant temperature card. Weather illustration sits outside the type's clear region. Never invent city/weather; stale status visibly accompanies cached readings.

### 18 Aurora — Chromatic space
Slow broad auroral ribbons near the edges with an uncluttered black central area for time. Choose one accent family and a complementary secondary hue, avoiding full-spectrum gradients. Component 1 adds a compact date line. The scene freezes beautifully under reduced motion and dims strongly at night; time never moves with the ribbons.

### 19 Breathe — Stillness
A single soft breathing disc/ring in an open field, with current instruction above and fixed session time below. Phase shape/size is the motion language, not a busy animated background. Component alternatives are 4-7-8 and box breathing, clearly named. Text switches at exact phase boundaries, ring interpolation stays continuous. User can pause/exit with a large predictable target.

### 20 Terminal — Console
A deliberate monochrome instrument with a fixed monospace time line and a short set of labelled readings. Use aligned columns and real data, no simulated hacker logs. Component 0 is clock-dominant; component 1 is a compact system table. Prompt accent and caret are subtle; no jitter, random scrolling or gratuitous boot animation.

### 21 Sky — Open air
A landscape gradient tied to local solar conditions, with a refined sun/moon treatment and a free-standing time anchor. Distinct from Sun path: this is atmosphere, not a chart. Components may place time high or low depending on horizon composition. Use stable contrast scrims as the sky lightens. Unsupported location falls back to explicitly time-based appearance.

### 22 Timer — Focus
One large countdown in fixed tabular cells, with a thin progress arc or rail and an unmistakable pause/resume control. Idle state offers a few useful presets in one measured row. Running state gives remaining time precedence over preset buttons. Second component may be a compact timer with clock above. Expired state has clear status and real stop/reset actions; don't claim control of a third-party timer unless its notification supplies that action.

## Three new photo-backed concepts

### A Shore — Negative-space photography
A full-bleed dawn shoreline where water/sky provide a clean time zone. A large light condensed clock occupies the empty sky; a tiny date anchors the opposite lower edge. Photo and typography form one composition. The user can swap the image; layout uses a selectable upper/lower anchor and contrast preview. No stock watermark, no fake weather over the scene.

### B Botanical — Silhouette and time
A dark macro leaf or botanical silhouette extends beyond the screen edge, leaving a wide field of shadow. Warm ivory time sits in the shadow field, with restrained moss accent for date. Optional foreground mask can overlap only decorative portions of a digit, never make the time ambiguous. Mask is optional and cannot be fabricated for arbitrary user photos.

### C Window — Architecture as grid
A soft architectural interior/window photo provides a natural vertical axis. Time is placed along its calm wall region and a compact date aligns with the window sill. One subtle photo tint unifies the colours. Focal-point adjustment protects faces and subjects. Keep architecture as a photograph, without drawing fake panels around it.

## Implementation acceptance gates

Render every component at Samsung landscape dimensions and a narrower 16:9 test size. Include 00:00, 11:11, 23:59, 12-hour AM/PM, longest translated weekday/month, six-row month, no media, long title/artist, no art, live stream, paused, seeking and disconnected listener. Check every pointer target against actual drawn bounds. Inspect night and reduced-motion captures, plus page-change and mode-bar transitions. Aesthetic completion is a visually reviewed result across this matrix, not a successful build alone.
