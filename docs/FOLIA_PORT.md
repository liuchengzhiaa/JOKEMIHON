# Music Folia rendering port

Owner request2026-10-27: replace the approximate joke.11 wall with the music app's
actual visual behaviour. Reference project JOKESONG (called JOKEMUSIC by the owner),
commit `0c015384a005f042c6bf2eb30370999ed3cd66a6`. Reference checkout is READ ONLY.

## Reused owner implementation

- `feature/focuswall/FoliaWallLayout.kt`:12x8 templates, shape/aspect hero choice,
  exact complementary partitions and nearest-neighbour assignment. In Mihon under
  `app/src/main/java/eu/kanade/presentation/library/components/folia/`.
- `FocusWallLayout.kt`, `FocusWallSpatialIndex.kt`: geometry/decode bounds and bounded
  two-axis visibility, unchanged mathematics apart from package/constants placement.
- `core/artwork/CoverAccentLoader.kt`: pure bounded histogram/palette subset only;
  Mihon facts loader uses the existing Coil MangaCover chain rather than a raw URI.
- `FocusCoverWall.kt`: centring/inertia, animated rectangle transitions, poster chrome,
  distance/lights-out alpha and ambient gradient mathematics adapted to book actions.

These are the owner's independently authored Kotlin sources, not copied or mechanically
translated sources from upstream Folia's AGPL checkout. Existing Mihon licence/history
retained. No music files are modified, built, committed or published by this task.

## Intentional differences

- User explicitly asks no corner radius or spacing: default rectangular posters have
  **zero corner radius/gutter** (music uses tiny2dp); optional heart/circle remain shapes.
- Default is **FullCover** with dynamic decoded aspect, not the music default Square.
  Ordinary cards remain the mixed mosaic/cropped previews, as in music; the focused
  FullCover hero chooses a matching footprint then shows the complete image. The cell
  grid approximates reasonable aspect ratios; extreme panoramas may still letterbox.
- Book title/author/status replace audio/lyrics/transport; existing book open/selection/
  continue-reading stay. No fake playback state or lyrics in a comic reader.
- Global corner tool uses Mihon's adaptive settings sheet. Same applicable shapes,
  sizes/density/zoom/ambient/tint/motion/lights-out, not music-specific playback-follow.
- Focused image metadata comes from bounded decoded thumbnails and cached visible
  decode dimensions, not a direct provider bounds scan. No new media writes.
- Density previews only its number and commits on release, avoiding40k-item rebuilds
  every slider frame. Added cancellation checks between engine blocks; geometry maths
  unchanged. Imported constants use this repository's uppercase naming convention.
- Zoom/appearance global, pan/focus category-local. Old joke.11 transient coordinates
  reset through a new saved-UI namespace. Library IDs/tags/progress/backup107/108 intact.

## Verification boundary

See HANDOFF for exact outcomes. Geometry/palette parity tests do not prove phone FPS,
RAM usage, source-byte aspect precision or the owner's media-provider performance.
