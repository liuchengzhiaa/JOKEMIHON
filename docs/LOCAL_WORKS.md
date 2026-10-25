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
  stay intact; re-add same ordered selection is idempotent and adds categories rather
  than silently removing prior ones. Max 1000 selections per operation; use batches.
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

## Candidate and verification

Candidate 0.21.2-joke.2 / versionCode40; preserve app.jokemihon and existing dedicated
signer. Implementation/checks are IN PROGRESS; no second Release is published yet.
See HANDOFF for current test/build/device/push state, not the previous category-wall
receipt. Tests must include grouping, ordered identity/selection, count naming, source
branches, backup round-trip, safety/idempotence and actual SAF/reader/cover UI behavior.
