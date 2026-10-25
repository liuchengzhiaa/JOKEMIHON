# Expandable library category wall

Owner decision, 2026-10-25: use a multi-row tag wall, not a searchable category picker.

## Behavior

- Original horizontally scrollable tabs remain the collapsed view. An always-visible
  right-hand arrow expands/collapses the category navigation.
- Expanded chips wrap to multiple rows with variable widths, preserving the same
  category ordering and optional/filter-aware counts as the existing tabs.
- Selected category is highlighted. Names can wrap to two lines; screen-reader text
  retains the full label. Chips have a minimum 48dp height.
- Height is capped at the smaller of 280dp and 35% of screen height. Additional rows
  scroll vertically. Lazy rows avoid composing every offscreen category chip.
- Clicking a chip resolves its stable category ID against the latest displayed list
  and jumps directly to that pager page, without animating through unrelated pages.
  It stays expanded. The existing horizontal page gestures still work.
- Expansion uses rememberSaveable for activity state restoration. It is not a new
  always-expanded preference across fresh launches.
- Closing the wall restores original horizontal navigation around the active page.
- No new cover fetches, library scans, database queries, sorting rules or production
  database migration are added. Existing library StateFlow/query gating stays intact.

## Implementation

- LibraryTabs.kt: fixed toggle, measured chip widths, lazy rows and selection styling.
- CategoryWallRows.kt: linear, pure-Kotlin ordered row packing with capped long labels.
- LibraryContent.kt: stable-ID lookup and direct pager jump.
- Static accessibility/action strings are in Android resources (English/zh-CN).
- JUnit covers wrapping, exact fits, oversize labels, overflow, invalid dimensions
  and 10000-category order/completeness. tools/test-category-wall.ps1 independently
  compiles the actual production packer and checks seeded boundary/property cases.

## Initial custom application identity

- Name: JOKEMIHON. Release package: app.jokemihon (debug: app.jokemihon.dev).
- Published: 0.21.2-joke.1, versionCode 39. See HANDOFF for verified delivery receipt.
- Independent open-book/J vector launcher/splash icon. Internal upstream namespaces
  are kept for compatibility; existing official Mihon installations are not replaced.
- Upstream updater/telemetry are disabled; release links target the owner repository.
- Dedicated private key is local and ignored under .signing, with ignored signing
  properties. Only the public certificate fingerprint is checked into tools/.
- Existing official-app libraries are not silently copied into this separate package;
  users should retain backups and use the app's existing restore flow when needed.

## Verification checklist

- Expand/collapse, current highlight and count visibility.
- Long names/mixed scripts, empty categories and preserved category order.
- Vertical wall scrolling without scrolling the book grid underneath.
- Far category selection and synchronization back to collapsed tabs.
- Rotation/activity restoration and larger system font.
- Actual release startup/signature/package/version checks, not just test helpers.

See HANDOFF.md for exact completed checks, failures, limits and delivery receipt.
Physical-phone acceptance and complete third-party extension/tracker regression are
not implied by the synthetic layout and emulator checks.

## First Release verification and known limits

The exact universal minified Release APK passed 14 saved emulator XML/log checks
with 40 synthetic categories / 30 fake books, in English and Simplified Chinese.
Includes far empty-category jump, vertical wall versus horizontal book gestures,
count preference, selected highlight, collapse synchronization, rotation, cold
restart and long names at 150% font. 125 Debug unit tests and formatting passed;
15 module Lint reports contain 0 errors and 146 warnings. This is not a physical-phone,
full reader/extension/tracker or backup/restore regression test.

In landscape the expanded wall crowds/clips the upstream empty-category illustration;
collapse the wall for more book area. A first-boot emulator System UI timeout recovered
with Wait; no application crash/ANR was found in the captured app exits/logs.
