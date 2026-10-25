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

## joke.3 follow-up (published preview, 2026-10-25)

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

Published preview: [v0.21.2-joke.3](https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.3),
code41/source c2f1fd287.162 tests pass, Lint0 errors/152 warnings, formatting/resources
and same-signer universal APK static checks pass. Public whole APK hash verified.
New screen/interaction/new backup-field device restore acceptance is owner-pending;
no new device run was performed. See HANDOFF for exact verification boundaries.

## joke.4: optional original-entry removal

Combined import offers a default-off checkbox to remove fully covered originals
from the library after saving the new collection and its selected categories.
Virtual originals require all memo members selected. Legacy directory originals
require all currently listed works and stored chapters selected. Partial or invalid
originals remain. The new target is excluded. Only favorite status changes: no media,
chapters, history or saved categories are deleted. Removed originals disappear from
all visible library tags, not just the destination tag. The new collection does not
automatically inherit tags or virtual-original reading progress. Root logical-directory
merge alone still does not change any library entries. Re-add originals to restore
them to the library; current ordinary library backups omit unfavorited entries, so
make a backup BEFORE replacement to preserve the prior library membership snapshot.

Owner requested no tests for joke.4; compile/package only, phone acceptance pending.

## joke.5: local work directories are separate from the library

At the author level, long-press ungrouped works in order, then Combine into work
directory. Name defaults to first work plus part count; arrows adjust order before
creation. Creation stores metadata only, never favorites, tags or files. Click the
Folder card to see its children; long-press the card or use Directory options inside
for directory import/category edit/reader. No nested work directories or post-creation
member editing offered in this slice. Directories are shown in the author group that
contains their first original physical parent; changing author grouping does not
change directory identity or stored members. Missing files remain referenced.

A directory uses the existing ordered multi-member LocalWorkDefinition identity;
children use their own single-member identities. Removed all child-to-combination
fallbacks. Both can be in different library categories and have different page covers.
Earlier favorited combinations automatically become manager directories using the
same manga IDs/ordered URLs, so their existing tags/progress are not remapped. The
old optional fully-covered-original removal is now on directory import only, default
OFF. It is an explicit opt-out from otherwise independent original favorite status.

Inside a directory, preview a child and choose its first page as directory cover;
this does not create a favorite or change that child cover. For another image, use
Directory options > Open / Choose cover, choose a chapter in the directory manga,
then long-press a reader page > Set as cover. Portable path/page is stored in both
manga metadata and directory records; even an unadded directory keeps its cover in
normal Library backup. Gallery cover overrides remain private cache, not portable.

Optional backup field108 contains all directory names, ordered members and portable
covers, including unfavorited directories. Library must be selected; App settings
is not needed. Use Chapters/Categories too for favorite progress and categories.
Restore with108 validates then replaces current directory records; older backups
without108 retain existing records. Field107 author groups continue independently.
Files are not copied: retain relative local paths and regrant storage on a new phone.
Directory record bounds:10k records,100k total members,8MiB encoded,max1000 per dir.

Label badge means this exact favorite has at least one named category; Check means
in library, Folder means logical work directory. Parent tags never mark a child.
Choose tags opens a searchable lazy grid; filled/partial states show current all/
some membership. Cancel discards picker changes; Done choosing returns the draft
and the outer Confirm/Save persists explicit additions/removals. Untouched mixed
batch tags remain unchanged. No tests run for this slice; owner acceptance pending.

## joke.6: compatibility with original local library books

Legacy LocalSource library books identify a physical parent directory, whereas new
virtual works use ordered-path hashes. Both are now indexed. If a completed current
author scan has exactly one work under a favorite legacy parent AND every stored
chapter URL matches that work, the single card can reuse the original book, categories
and progress. An exact favorited virtual single always wins. There is no title-based
matching, ID migration or automatic copying. Batch prefill and import recheck the same
full-directory coverage against live storage before resolving a legacy favorite.

Editing categories always reads/saves the explicit actual manga ID. Legacy category
edits do not require media availability or create a new virtual manga. If an original
parent has multiple works, or stale/unselected chapter references, it is not treated
as the same individual child: its tags appear separately under Existing related
library tags. Use Edit this entry’s tags in preview to modify that original row.
Parent/collection tags are not automatically preselected as an independent child’s
own tags. A new group similarly shows related tags without falsely being favorited.

Primary Label means the exact resolved item owns named tags. Secondary Label plus
Contains previously tagged entries means related original/collection books own tags.
Author rows aggregate the presence of named tags among original/virtual library books
under their physical directories. All contexts come from library IDs/category data,
not file guesses. Old-directory reader cover/refresh behavior remains upstream when
explicitly opening a reused legacy book. No new automatic media writes are added.

Backup fields107/108 unchanged. Original DB IDs/categories use standard backup fields;
aliases are rebuilt from restored library and unchanged relative local paths. No tests
run for code44; owner phone acceptance pending, including legacy and mixed batch cases.

## joke.7: smaller category status badge

Top-left Check means this resolved entry is in the library, not selected. Selection
checkboxes only appear after long-press. Bottom-right Label means own or related named
categories as above, not an action button. Its overall badge is now16dp instead of28dp
(12dp vector +2dp padding each side),5dp rounded corners and4dp cover-edge inset.
Status colors/descriptions and all identity/category/backup behavior are unchanged.
No tests performed for this visual-only slice; owner phone sizing acceptance pending.
