# Clavinmanager: Colour Guide

Four colours only. Sleek, modern, minimal and friendly. The UI uses the same colours as the logo.

## Palette

| Role | Name | Hex | RGB |
|---|---|---|---|
| **Brand** | Blue | `#2563EB` | 37, 99, 235 |
| **Neutral (dark)** | Black | `#0F172A` | 15, 23, 42 |
| **Neutral (light)** | White | `#FFFFFF` | 255, 255, 255 |
| **Accent** | Aqua | `#2DD4BF` | 45, 212, 191 |

Black is a very dark blue-black (not pure `#000000`) so it sits well beside the blue.

## Where each colour is used

**Blue `#2563EB` (brand)**
- Logo, primary buttons, links, selected and active states, key icons.

**Black `#0F172A` (neutral, dark)**
- Body text, headings, dark mode background, the "Clavin" part of the wordmark.

**White `#FFFFFF` (neutral, light)**
- Main background, cards, text and icons placed on blue.

**Aqua `#2DD4BF` (accent)**
- Small highlights only: badges, progress indicators, the dot in the logo.
- Keep it rare. It should never be a large fill or the main button colour.

## Suggested balance

- About 80% white or black (depending on light or dark mode)
- About 15% blue
- 5% or less aqua

## Accessibility notes

- White text on Blue: passes (about 5.2:1).
- Black text on White: passes (about 17:1).
- Black text on Aqua: passes (about 10:1).
- Do not put aqua text or icons on a white background; contrast is too low (about 1.9:1). Use blue or black there instead.
- Do not put blue text on a black background for body copy; use white.

## Dark mode

- Background: Black `#0F172A`
- Text: White `#FFFFFF`
- Buttons and links: Blue `#2563EB`
- Accent: Aqua `#2DD4BF`

## Logo files

- `clavinmanager-logo.svg`: icon plus wordmark. Wordmark is "Clavin" in Black and "manager" in Blue (text converted to outlines, no font needed).
- `clavinmanager-icon.svg`: icon only, for the app icon, favicon and small spaces.
- Both are SVG (vector), so they scale to any size without losing quality.

On a dark background, use white for the "Clavin" part of the wordmark and keep "manager" in a lighter blue that stays readable.

## Logo icon construction

- Rounded square in Blue, corner radius about 28% of the width.
- White "C" with a round stroke, open on the right.
- Aqua dot inside the opening of the C.
- Keep clear space around the logo equal to the height of the aqua dot's diameter or more.

## Typography (suggestion)

- Inter, with Bold for headings and Regular for body text. This matches the wordmark.
