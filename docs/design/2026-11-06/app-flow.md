# Standby companion app: preview-first design

Design proposal only. No source changes made. Based on current native SettingsActivity, Prefs, Frames and StandbyView. Product is an Android bedside display with a companion app; preserve all existing activation and phone capabilities.

## Core decision

Browsing and previewing never changes the user's running Standby. Selection, variant edits and order live in a draft until the user presses **Apply to Standby**. The app should present the visual catalogue first; permissions and technical setup belong to contextual setup cards. Every frame is previewable before it can be added, including unconfigured Photos, Weather and Music through clearly labelled sample data.

## Information architecture

Three top-level destinations: **Home**, **Explore**, **Settings**. Use native bottom navigation with icon plus text and retained scroll position. A selection tray is a task surface opened from Home or Explore; it is not a fourth destination.

### Home

1. Title Standby, small factual status such as “Ready when charging sideways” or “Finish setup”. Never show Ready solely because a settings flag exists.
2. Wide preview of the currently applied carousel, with visible frame name and variant name below. Accessible Previous frame, Next frame, Previous look and Next look buttons accompany gestures.
3. “Your Standby” shows one to three thumbnail previews in playback order, with frame names and a clear Edit selection button. No miniature live controls that accidentally play music.
4. Primary action **Preview full screen**. Secondary **Explore frames**.
5. Collapsed setup card with completion count. Ask for location only after choosing a location-dependent frame; explain Music access only when Music is selected. Make call handling a separately understandable capability.
6. Settings rows for Display, Activation, Time and region. Reset belongs at the end of Settings, outside the daily home journey.

### Explore: all 23 frames

A two-column portrait gallery, with large consistent landscape thumbnails, 16sp frame names, compact visible look count and an explicit selection check badge. At large font sizes use one column. Landscape/tablet uses a two-pane gallery/detail layout rather than one 560dp column floating in unused space.

Search by frame name and broad groups: All, Clocks, Music, Nature, Utilities. The exact 23 catalogue entries are Digital, Dial, Music, Clock and Music, Photos, World, Flip, Word clock, Dot matrix, Nixie, Alarm LED, Railway, Orbit, Sun path, Moon, Time left, Big date, Weather, Aurora, Breathe, Terminal, Sky and Timer. Keep their existing IDs stable.

Tapping a tile opens detail. It must not add/remove the frame. Every tile shows a real renderer thumbnail from a shared controlled preview model, not a marketing image. Use a single timestamp and consistent fixture inputs across comparison previews. Label “Sample preview” whenever not live device data. Avoid running all gallery renderers at animation frame rate: static thumbnails, animate only the focused detail preview.

A persistent bottom selection tray shows thumbnail slots and **Review 2 of 3**. When selection is full, an Add action opens an explicit replacement sheet showing the three selected frames. Do not make the user navigate away and turn one off first.

### Frame detail

Top: Back, frame name, bookmark/selection state. Main: landscape preview that can expand to full screen. Beneath it, show the named looks as preview thumbnails, not an undiscoverable tap cycle. Show all looks from Frames.COMPONENTS, including the single-look states without a disabled carousel.

For example, Digital exposes Digital and Float; Dial exposes Analog, Widgets and Calendar; Clock and Music exposes its two layouts. Remaining names must map to renderer semantics rather than “Look 1” where a meaningful name exists. The detail preview loads the currently selected variant rather than always index zero.

After looks: colour palette with named swatches, then frame-specific options grouped by purpose. Label shared settings as “Also used by …” or move them into global settings: digital colour currently spans Digital and Clock and Music; music background spans both players. Explain those relationships before Apply.

Bottom actions: **Compare** and **Add to selection** (or **In your Standby**). Changing a look or colour only changes the draft preview. An unsaved-change indicator appears beside its name. Back offers Keep editing / Discard changes only if changes exist; no confirmation for browsing.

### Compare and selection: up to three

Compare opens a dedicated sheet/page with two or three previews at identical aspect, time, weather and playback state. Portrait stacks previews vertically; landscape places them side by side only when readable. Show frame and look labels and selected status. Provide Day / Night toggle; sample-data indicator; Pause motion control. Compare does not change the running device.

Review selection shows three generous ordered rows with thumbnails. Support drag handle plus explicit Move earlier/Move later actions for accessibility. Removing the final frame is unavailable with a visible explanation. Editing a variant returns to the same draft and selection order. Footer: **Apply to Standby** plus Cancel. Apply saves frame IDs, order, chosen look indices and settings together, announces success, then returns Home. Provide brief Undo that restores the previous applied configuration. Preserve the existing 1–3 invariant and sanitization in Frames.parse.

## Accessibility and interaction contract

- Minimum 48dp targets for Back, reorder controls, swatches, chips and all actions; current 34dp swatches need larger wrappers.
- Native text, switches, buttons and collection semantics around custom Canvas previews. The preview should announce frame/look and state, with explicit actions; it must never expose a wall of decorative drawing descriptions.
- TalkBack can browse every frame, every variant, compare, add, remove, reorder and Apply without gestures. Label switches with their visible setting names and checked state. Selection badge has text/state, not colour alone.
- Visible 4.5:1 contrast for ordinary text, 3:1 large text and meaningful graphics. Improve muted copy rather than relying on very dark grey. Support Android font scale and 200% layout verification, wrapping labels and growing rows.
- Honour system animation disable and the existing in-app animation preference. Paused previews remain visually useful. No auto-scrolling gallery or motion in offscreen cards.
- Focus returns to the originating tile after detail/compare closes. Selection status changes are announced without re-reading the screen. Keep scroll position through edits.
- Large landscape display preview preserves the screen ratio but keeps navigation/controls outside its viewport. Use touch-safe margins for companion controls while leaving the artwork itself full bleed.
- No request for every permission on first launch. Show accurate denied, unavailable and sample states without hiding the visual catalogue.

## Concrete current issues

Source root: /Users/rohanc/Documents/ChatGPT/Standby Mode/app/src/main/java/com/rohan/standby/

1. SettingsActivity.java:57 requests coarse location on initial app creation before a weather-dependent choice. This interrupts visual discovery and gives no frame-specific reason.
2. SettingsActivity.java:117–139 renders all 23 catalogue entries as text rows and toggles. There is no per-entry thumbnail, so users choose before seeing the design. When three frames are selected, a toast tells them to turn one off first instead of offering a replacement.
3. SettingsActivity.java:137 persists selection immediately; :144 persists reordering immediately. There is no draft review, Apply or Undo, so browsing and configuration have immediate consequences.
4. SettingsActivity.java:147 and :260–266 always lock a detail preview to component 0. Users must tap to discover other looks, while their saved current component is not the initial detail state. No named variant rail or compare surface exists.
5. SettingsActivity.java:299 and :323 write option values immediately. Settings that visually look frame-specific can be shared across frames (for example digital/music keys in :149–153), with no visible explanation of scope.
6. SettingsActivity.java:142 provides symbol-only up/down reorder text, and :314–324 builds unnamed 34dp colour Views. Their size and missing content descriptions/state semantics impede TalkBack and precise touch.
7. SettingsActivity.java:287 creates unlabeled Switch children next to separate text. Make the setting label available to the control, and avoid redundant row/control focus stops.
8. SettingsActivity.java:86 Back is a small TextView with padding rather than an explicit minimum-size native navigation target. :269 uses fixed text appearance with no layout strategy shown for large font scale.
9. SettingsActivity.java:40 contentWidth clamps all layouts to 560dp; :260–266 offers a small ratio-locked preview. On landscape phones this wastes space and still gives users a tiny preview. Prefer adaptive two-pane detail and explicit full-screen preview.
10. StandbyView.java:28–30 documents gesture-only discovery and hidden long-press brightness. :105–107 persists frame/component transitions during normal running. In companion preview mode :64 pins a frame but still exposes gesture behavior; previews need explicit variant controls and a draft model so user intent is visible and accessible.
11. StandbyView.java:55–57 initializes real Weather data and active frame state in every view. Preview/comparison needs an injected, reproducible model and should avoid triggering unrelated network/location refresh merely to show catalogue thumbnails.
12. Prefs.java:frames/saveFrames/component define applied persisted state only. Introduce an in-memory draft snapshot at the settings UI boundary; avoid reusing applied preferences as temporary preview storage.

## Acceptance walkthrough

Open app with no permissions -> view all 23 frames -> open Dial -> explicitly preview all three looks -> choose Calendar look -> compare it with Digital and Music -> review exact three-frame order -> Apply once -> verify running Standby receives the chosen order and variants. Repeat with TalkBack, animations disabled, 200% font scale, portrait and landscape. Cancel and Back must leave applied state unchanged. Denied weather/media/photo permissions must still permit clearly marked sample previews and a complete browsing flow.
