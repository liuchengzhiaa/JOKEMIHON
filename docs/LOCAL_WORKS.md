# Non-destructive local works manager (JOKEMIHON)

## Owner-approved scope (2026-10-25)

- New More > Local works manager, separate from legacy Local source and existing library.
- Root folders grouped by exact leading ASCII digits plus the first subsequent Unicode
  letter (Han/kana/Latin/etc.), ignoring intervening punctuation/spaces. A letter must
  exist. No prefix or no letter means independent. Full author names need NOT match.
  Leading zeros and letter case remain exact. Display name defaults to the naturally
  first member folder with trailing split digits removed. Original folder names stay.
- Opening an author flattens the supported immediate child archives/image directories
  from all member folders. Not another list of split folder cards. Same basename in
  different parents stays distinct; source folder is shown on each card.
- Ordered selection displays ordinals. Separate add creates one virtual manga per
  selected child; combined add creates one manga with chapters in selection order.
  Arrows allow order correction before confirming; automatic name follows the first
  selected item's name (archive extension removed) plus part count, e.g.
  作品D 上（六部曲）. Name editable before saving; default cover is the first item.
- Select existing library categories during add. Existing manga/categories/progress
  stay intact; re-add same ordered selection is idempotent. Untouched tags stay;
  joke.3 additionally supports explicit category removal (see below). Max 1000 selections per operation; use batches.
- Open/preview work, enter reader, long-press an image > Set as cover. Selected page
  reference is private app metadata, not author cover.jpg. Existing bundled work opens
  when a selected child is already in a saved bundle (single entry preferred).

## Design and safety boundaries

- No new production database schema. LocalWorkDefinition lives in namespaced manga
  memo, already serialized by normal manga backups; stable virtual URL hashes ordered
  original parent/name references. Existing source ID0 and local reader load real files.
- LocalSource has a narrow early virtual branch; legacy discovery/metadata code stays
  unchanged. Invalid/missing definition or files fail rather than clearing chapters.
- Cover URL contains original relative path plus page index, so selected reader covers
  regenerate after restore/cache removal with the same media/SAF grant. Normal backups
  do not copy comic files. Gallery-file overrides follow upstream private cover-cache
  behavior and are NOT made portable by this feature; select a reader page for portability.
- Cover streams use the reader's natural image ordering, bounded to two simultaneous
  streams and 64 MiB per image; Coil sizes visible images. No whole-archive extraction
  or eager cover decoding at author scan. Root scans directory metadata only; child
  contents are listed only for the opened author. Existing SAF grant is reused.
- New chapter records inherit read/bookmark/page state once from the original local
  parent manga. Original entries/history/files are never rewritten. Subsequent progress
  of legacy and virtual entries is independent (no silent cross-entry mark-read).
- Different ordered member lists intentionally have different IDs. Same path/order
  re-add does not duplicate; overlapping or differently ordered bundles can coexist.
- Rename/move a physical file and its reference can break. Do not infer identity from
  basename alone or silently remap/delete missing files. No destructive file operations.
- Batch add finishes coherent manga entries sequentially. If interrupted, earlier adds
  remain, later items can be retried; favorite publication follows chapter initialization.
  The operation is not an all-or-nothing multi-manga transaction.

## Delivered version and verification

Published 0.21.2-joke.2 / versionCode40 with unchanged app.jokemihon and dedicated signer.
[Download](https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.2).
151 unit tests pass (26 new), Lint0 errors/149 warnings, both changed module format checks pass.
Exact minified universal Release tested on isolated Android16/API36: code39 upgrade, real
SAF grant, grouping/flattening, six-click order/title/reorder, separate/category/idempotent
adds, ZIP and JPEG-directory reading, page covers/cold restart, actual normal backup and
restore after removing synthetic virtual entries, and source refresh. All34 media file
names/hashes unchanged. See HANDOFF/build manifest for exact evidence and untested areas.
Normal backups preserve virtual references but not media; do not move original files.

## joke.3 follow-up (candidate, 2026-10-25)

- Directory cards/search/grid polished. Long-press a logical directory to select in
  click order. Merge2+ with first selected name by default, or rename a single one.
  Manual groups can be merged again; dissolve restores automatic grouping, NOT a
  nested undo history. Only presentation membership/name changes, not comic paths,
  existing library titles/progress or work identity. New unrecorded folders keep auto grouping.
- Explicit flat mapping records retain original directory names, stable UUID/display
  name; no cascading guessed if/else rules. Temporarily unavailable recorded folders
  remain remembered. Validate uniqueness/path safety/version/10k directories/4MiB JSON
  before persisting. Rules live in app-state preferences, never user media folders.
- Work checkboxes/select-all are hidden until long-press enters selection mode; tap
  then selects additional works. Clear/back/last deselection exits selection mode.
- Import categories prefill all/some/none (tri-state for mixed batches). Explicit
  uncheck removes; untouched mixed tags remain. Removing every named tag leaves the
  default category, not a removed library item. Existing work preview > Edit categories
  edits its actual saved single/collection and works even when media is unavailable.
- Normal Library backup includes manual rule JSON in optional protobuf field107,
  regardless of App settings selection; general app-state backup excludes duplicate
  copies. Restore with Library validates complete record before replacing current
  rules; absent field in older backups leaves current rules alone. Work membership,
  page-cover references and progress use existing manga/chapter fields; include
  Chapters and Categories when creating backups. Media and gallery caches are not included.
- Owner requested simple validation: unit/Lint/format and signed APK static checks,
  no new extended AVD/device test. Previous joke.2 device receipt remains historical.
