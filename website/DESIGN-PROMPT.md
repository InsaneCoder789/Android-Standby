# Android Standby website: the design prompt, the concept, and a reusable approach

This file has three parts.

1. **The prompt**: a complete brief that would reproduce this website's design.
2. **The concept**: what I understood the product to be and why the site looks the way it does.
3. **The approach**: the method to follow when generating a differently styled site for a different product.

---

## 1. The prompt for this website

Copy everything in this section as one prompt.

> **Product.** Android Standby is an open-source Android app. When a phone is charging and resting on its side, the app turns the screen into a bedside clock. It has 24 "faces" (digital, analog dial, flip cards, nixie tubes, word clock, music player, world clocks, moon, aurora and more), each with several looks. The user keeps up to three and swipes between them. It is built from source, runs on Android 9 and newer, and is not affiliated with Apple or Samsung.
>
> **Audience and job of the site.** Android users who have seen iPhone StandBy and want it. The site has one job: let them see the real faces working, then send them to the setup guide.
>
> **Core concept: the site is the screen.** Do not show a photo or a 3D render of a phone. The product is a screen, so the website behaves like that screen. Every clock face on the site is a live redraw running on the visitor's own clock and time zone. The page is black like a display that is off and white like one that is on. Colour comes only from the faces.
>
> **Colour.**
> - Off: `#000000` (page ground, hero, closing section)
> - On: `#FFFFFF` (text on black, primary button)
> - Paper: `#F3F3F0` (the white sections)
> - Ink: `#0B0B0C` (text on paper)
> - Grey: `#8B8B8F` (secondary text on black), `#45454A` (secondary text on paper)
> - Rule: `#26262A` (hairlines on black), `#D6D6D0` (hairlines on paper)
> - No brand accent colour. Each face lends its own colour (green for Digital, magenta for Music, orange for Nixie) only where that face is being shown.
>
> **Type.** One family, Archivo (variable, weight and width axes), used in two voices.
> - Statements: width 125%, weight 800 to 900, letter-spacing about -0.04em, line-height 0.9 to 0.95, sentence case, never all caps.
> - Reading text: width 100%, weight 400 to 600, 16 to 19px, line-height 1.55.
> - Small labels: width 112%, weight 600 to 700, 13 to 15px.
> - The live faces keep their own fonts (Inter, Nunito, Barlow Condensed), exactly as the app draws them.
>
> **Shape and controls.** Screens have rounded corners (10 to 26px) and a 1px ring. Buttons are full pills. Links are underlined text. No cards, no gradients as decoration, no icons used as ornament.
>
> **Page structure, top to bottom.**
> 1. *Top bar.* Solid black strip, fixed. Wordmark "standby" on the left in the wide heavy cut. Two links on the right: "All faces", "Set it up".
> 2. *Hero: a wall of live faces.* One full window, black. Eighteen live faces in framed screens, arranged in three rows of three different heights, the whole wall tilted about 8 degrees. Each row drifts sideways slowly at its own speed and direction. A shade darkens the lower left. Over it, at the bottom left, the headline "Sideways, it's a clock." in the wide heavy cut, sized from both the window width and height so it always fits. Under it, one sentence of explanation on the left and two actions on the right: a white pill "See all 24 faces" and a text link "Set it up". On load, the headline's words rise out of their line one after another.
> 3. *Reel: a scrolling showcase on white.* Paper background, ink text. Two columns. On the right, one large screen (black, rounded, thick ink border, soft shadow) stays pinned in the middle of the window. On the left, six short descriptions scroll past, each one window tall: a large statement and two lines of detail. As each description reaches the middle, the pinned screen changes to the live face it describes with a quick cross-fade and a slight settle in scale, so reading down the page plays like a short film of the app. Under the screen, the face's name in its own colour and a counter "3 / 6". The six: Digital "It runs on your time.", Flip "Read it from across a dark room.", Music "Your music takes the whole screen." (playable demo), World "Four cities. No arithmetic.", Aurora "Something slow to fall asleep to.", Nixie "24 faces. Keep three. Swipe." with the two actions repeated. On narrow screens the screen pins under the top bar and the descriptions scroll beneath it.
> 4. *How it works.* Paper. Heading "Three steps, then leave it alone." Three columns, each with a very large numeral, a short title and one sentence, under a 2px ink rule. A fine-print line about charging and activation.
> 5. *Questions.* Paper. Heading "Good to know." on the left, an accordion of eight questions on the right.
> 6. *Close.* Black. The visitor's current time set as large as the window allows in the widest, heaviest cut. Then "A little less phone. A little more standby." and the two actions.
> 7. *Footer.* Black, one row: wordmark, a one-line disclaimer, four links.
>
> **Other pages.**
> - *All faces:* black. Heading "24 faces. All of them live." A time-zone picker and an optional local-weather button. A three-column contents list. Then three groups (The clocks, The everyday, The atmosphere), each face shown live in a rounded screen with its name, two sentences, and pill buttons to switch looks. Between groups, a paper band with one large statement.
> - *Setup guide and privacy:* black reading pages. Very large heading, a grey lead paragraph, then numbered sections separated by hairlines, with code blocks in a dark rounded box.
>
> **Motion rules.**
> - Only transforms and opacity move. Nothing is drawn per scroll frame.
> - Scrolling is the browser's own. No scroll-smoothing library.
> - Continuous motion exists in one place only: the hero wall's slow drift. It pauses when the hero is off screen.
> - Everything else moves once, in answer to arriving: words rising, the pinned screen changing face, descriptions sliding up as they enter.
> - With reduced motion turned on, nothing animates and all content is still present.
>
> **Writing.** Short, plain, concrete. Statements are things the visitor will notice in a dark room, not feature names. State limits honestly: demos do not reach the phone, activation varies by manufacturer, the app is built from source.
>
> **Quality floor.** Works from 360px phones to 1920px desktops and on a phone held sideways. No sideways scrolling. Visible keyboard focus. Headings in order, one `h1` per page. Type sizes are set with `min()` of a width unit and a height unit wherever a statement must fit the window.
>
> **Build.** Next.js App Router, plain CSS files, no animation or 3D libraries. The live faces are React components that read one shared clock.

---

## 2. The concept and how I understood it

**What the product actually is.** Standby is not a clock app in the usual sense. It is a second state for a phone: the moment the phone stops asking for attention and becomes an object in the room. Its whole value is a screen you glance at from a distance.

**What that means for the site.** Three earlier directions were built and dropped, and each one taught something.

- A paper-coloured editorial site with a 3D bedside scene looked pleasant but was heavy and jittered on scroll.
- A scroll-played "film" of the rendered bedside was smoother but leaned on the same rendered room, which was the weakest asset. The app's real screens were the strongest.
- A sequence of full-window statements and faces was sleek but flat: one thing on screen at a time, nothing moving.

The surviving idea is the simplest one: **show the real thing, live, and get out of its way.** The faces are already designed objects with their own colour and type. A site that adds its own palette, illustration and texture competes with them. So the site contributes only black, white, one typeface and motion, and lets 24 faces supply everything else.

**Why each main decision was made.**

| Decision | Reason |
| --- | --- |
| Live faces, not screenshots | Proof. A clock showing the visitor's own time is believed immediately. |
| Black and white only | The faces carry strong colours. Any brand colour would clash with at least one. |
| One wide, heavy typeface | A bedside clock is about reading large shapes at a distance. The type does the same. |
| A wall of faces as the hero | The product's real selling point is variety. One face says "clock"; eighteen say "collection". |
| A white section for the walkthrough | Contrast marks a change of mode: the hero is to look at, the reel is to read. |
| A pinned screen that changes | It keeps the object still and lets attention move, the way the phone sits still on a nightstand. |
| No scroll-linked drawing | Smoothness was the repeated complaint. The safest fix is to give the browser nothing to compute per frame. |

**What I would still call unproven.** Eighteen live faces on the first screen is the most expensive thing on the site, and it has not been measured on a slow phone. Motion was reviewed only as still frames.

---

## 3. A reusable approach for other products and aesthetics

Use this as the standing instruction when automating site generation. It is a method, not a style: the style must come out of the product each time.

### Step 1. Find the one true thing

Before any visual decision, answer in a sentence each:

1. What is the product, physically or literally? (a screen, a bottle, a service call, a dataset)
2. Who is looking, and in what mood?
3. What is the single piece of evidence that would make them believe it?
4. What is the one action the page should end in?

The answer to question 3 becomes the hero. For Standby it was "a clock showing my own time". For a coffee roaster it might be the roast date on the bag; for a developer tool, a working command.

### Step 2. Take the look from the subject, not from a trend

Draw every token from the product's own world.

- **Colour:** sample it from the real object, its materials, its packaging or its interface. Name 4 to 6 colours by what they are ("lamp glass", "paper", "ink"), not by role. If the product already has strong colours, the site should have almost none.
- **Type:** pick for the product's voice and reading distance. One family is the default; add a second only if it is clearly different and has a defined job.
- **Shape:** borrow corner radius, line weight and spacing rhythm from the product itself.
- **Texture and imagery:** use only what the product can truthfully supply. If the best asset is the live product, use that and generate nothing.

Then check the plan against the common defaults: cream background with a serif and a terracotta accent, near-black with one neon accent, newspaper columns with hairlines, identical rounded cards with soft shadows, tracked-out capital labels above every heading. If the plan matches one of these and the brief did not ask for it, change it.

### Step 3. Choose one concept sentence

Write a single sentence that a section can be tested against, in the form "the site is ___" or "the site behaves like ___".

- Standby: "The site is the screen."
- A notebook brand might be: "The site is a page you can write on."
- A logistics API might be: "The site is a live shipment you are tracking."

Any section that does not serve the sentence is cut or made plain.

### Step 4. Spend the boldness once

Pick one memorable element, usually the hero, and make it as strong as the product allows. Everything else is quiet and disciplined: plain sections, clear headings, generous spacing. A page with five loud ideas reads as none.

### Step 5. Structure the page as an argument

A reliable order:

1. **Show** the evidence (hero).
2. **Explain** it in a few concrete beats (the walkthrough).
3. **Tell them how** (steps, only numbered if they truly are a sequence).
4. **Answer doubts** (questions, limits stated honestly).
5. **Ask** (one closing action).

Change background or mode only when the reader's task changes, for example from looking to reading.

### Step 6. Motion rules that keep it smooth

- Decide what moves continuously. The answer should be one thing or nothing.
- Everything else moves once, in response to arriving or to a click.
- Animate only transforms and opacity.
- Do no work per scroll frame. Use sticky positioning, intersection observers and CSS scroll-driven animation before any scripted scroll effect.
- Avoid scroll-smoothing libraries and per-frame canvas drawing unless the concept truly requires them, and measure on a real device if you use them.
- Reduced motion must leave a complete, static page.

### Step 7. Make it fit every screen on purpose

- Size large type from both width and height: `min(10vw, calc((100svh - chrome) / lines))`.
- Give tall and wide screens different compositions, not just smaller ones.
- Check at least: 360×640, 375×812, 812×375, 820×1100, 1280×640, 1440×860, 1920×1000.
- Test for sideways overflow by comparing the document's scroll width with the window width.
- Elements that share a visual group must share edges.

### Step 8. Write like the product

Copy is design material. Name things as the user would. Prefer what they will notice over what the feature is called. State limits plainly. No filler, no slogans that could belong to any product.

### Step 9. Review before showing

Look at real screenshots at several sizes and ask:

- Does the first screen prove the one true thing?
- Is there exactly one loud element?
- Could this palette and type belong to a different product? If yes, it is not specific enough.
- Is anything decorative that carries no information? Remove it.
- What is unverified? Say so.

### A template to fill for each new product

```
PRODUCT: what it is, literally, in one sentence.
AUDIENCE AND MOOD: who is looking, and how they feel when they arrive.
EVIDENCE: the single thing that makes them believe it.
ACTION: the one thing the page ends in.

CONCEPT SENTENCE: "The site is ___."

COLOUR (4 to 6, named after things in the product's world, with hex):
TYPE (family, the voices it is used in, sizes, why it suits this product):
SHAPE (corner radius, line weight, button style, taken from the product):
IMAGERY (what real assets exist; what will not be generated):

THE ONE LOUD ELEMENT: what it is and where it sits.

SECTIONS, in order, each with its job (show / explain / tell how / answer / ask):

MOTION: the one continuous movement (or none); what moves once and when.

WRITING: voice in three adjectives, two example lines, limits that must be stated.

SCREEN SIZES TO CHECK and what changes between tall and wide.

NOT ALLOWED: the defaults this design must avoid.
```
