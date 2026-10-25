# JOKEMIHON tasks

## Compact tag marker (2026-10-25)

- [x] Explain Check=in library; remove redundant top-left Check per owner follow-up. Keep status text and multi-selection checkboxes.
- [x] Shrink bottom-right tag badge28dp->16dp without new assets or logic changes.
- [x] Continue no-test workflow; compile/package only.
- [ ] code45 Release compile, source/docs push and public preview APK delivery.
- [ ] Owner phone visual acceptance.

## Existing legacy library tags (2026-10-25)

- [x] Source-confirm legacy raw-parent URL vs virtual hash mismatch and wrong picker lookup.
- [x] Complete-coverage one-to-one legacy resolution shared by batch prefill/add.
- [x] Explicit existing-ID category load/save; no duplicate creation for matched originals.
- [x] Own vs related tag markers, original-entry tag context/editor and author aggregates.
- [x] Continue no-test workflow; compile/package only.
- [x] Release compile9m53s; reviewed10-file source7842e5f03 pushed; v0.21.2-joke.6 PUBLIC preview,3 asset sizes/digests and anonymous API URL readback confirmed.
- [ ] Owner phone acceptance; no owner DB or device test this slice.

## Independent work directories and tags (2026-10-25)

- [x] Separate persistent directory creation from import; browse ordered children.
- [x] Exact independent child/directory identities; adopt old combined favorites.
- [x] Portable directory covers even without import; backup Library field108.
- [x] Lightweight named-tag vector badges and searchable tri-state grid picker.
- [x] Continue explicit no-test workflow; no unit/Lint/format/emulator/device checks.
- [x] Release compile9m11s; source438de729c and immutable v0.21.2-joke.5 PUBLIC preview;3 asset sizes/digests and anonymous public API delivery readback confirmed.
- [ ] Owner phone acceptance, including new-phone ordinary backup/restore.

## Combined-import replacement option (2026-10-25)

- [x] Default-off remove-originals checkbox, fully covered originals only.
- [x] Preserve media/chapters/history/category rows; never remove new target.
- [x] Skip ALL tests/Lint/device/format checks by explicit owner request this slice.
- [x] Signed code42 Release build9m42s; reviewed10-file source ea772c0fa pushed; v0.21.2-joke.4 PUBLIC preview,3 remote asset digests/sizes and anonymous API URL readback confirmed.
- [ ] Owner phone acceptance (no automated/device tests this slice).

## Local manager polish (2026-10-25, preview delivered)

- [x] Compact card/grid UI and long-press-only multi-selection.
- [x] Persist logical directory merge/rename/dissolve, original paths unchanged.
- [x] Current-category prefill, explicit removal, mixed batch preservation and direct saved-work category editing.
- [x] Manual rules included with Library backups independently of App settings; backup help added.
- [x] 11 new tests; focused37 pass.
- [x] Full162 tests/12 suites pass;15 Lint reports0 errors/152 warnings; format/resource49-key parity pass.
- [x] Same-signer code41 minified universal Release build/static APK check passed.
- [x] Reviewed18-file source c2f1fd287 pushed; v0.21.2-joke.3 PUBLIC pre-release, all3 asset digests and anonymous whole APK hash verified.
- [ ] Owner phone acceptance; no extended emulator regression this slice by explicit request.

## Local works manager (2026-10-25, delivered)

- [x] Record approved author grouping, flattened works, ordered bundles/category/cover design.
- [x] Implement source/model/library/UI paths; joke.2/code40 published and verified.
- [x] Compile/narrow tests, 26 model/service tests, media safety review.
- [x] Full tests: 151/11 suites, no failures/errors/skips; 15 Lint XMLs: 0 errors/149 warnings; app/source-local formatting and 33 resource keys pass.
- [x] Same-signer minified universal Release; actual code39 upgrade/SAF/grouping/ordered import/reader/cover/backup restore/refresh tests pass; 34 file hashes unchanged.
- [x] Reviewed 25-file source commit f4630606e pushed; v0.21.2-joke.2 published; all3 asset digests and anonymous whole APK download/hash verified.
- [ ] Optional owner physical-phone acceptance / large real-library feedback.

## Initialization (2026-10-25)

- [x] Download and verify upstream v0.21.2 source in an independent local checkout.
- [x] Expand shallow checkout to retain upstream commit history.
- [x] Create the owner's PRIVATE JOKEMIHON repository and disable inherited Actions.
- [x] Archive original upstream workflows under docs/upstream-workflows, avoiding
      executable workflow uploads and any need to expand OAuth permissions.
- [x] Document standing source/APK delivery and continuity requirements.
- [x] Commit/push reviewed initialization and verify remote SHA and settings.
      Receipt: main 1999e94e9b263a7ac256f2fb9b48946ab86fb082; PRIVATE, Actions off,
      0 runs and 0 Releases. Subsequent receipt-only docs commit needs no APK.

## Public access and performance research (2026-10-25)

- [x] Make JOKEMIHON PUBLIC with explicit owner approval; anonymous API reports
      private=false, visibility=public and the repository page returns HTTP 200.
- [x] Update current delivery instructions: future public APK downloads need no login.
- [x] Compare v0.19.0 / e4de208 with v0.21.2 / 9612d95 using release notes and
      actual Git diffs; preserve findings in MIHON_019_TO_0212_PERFORMANCE.md.
- [x] Push this documentation-only slice and verify remote commit/settings.
      Verified main=4147715d30595861627d492951f3d17330522814 (before receipt-only docs).

## Category wall slice (2026-10-25, delivered)

- [x] Owner selected multi-row category wall; fixed toggle, bounded scrolling,
      original order, counts/highlight and direct category jumps implemented.
- [x] Add pure row packer/JUnit cases and pass SDK-free smoke verification.
- [x] Configure independent identity and persistent dedicated local signer; pin
      public certificate only. Disable upstream updater and telemetry.
- [x] Work around full C: and broken bundled JDK with isolated D: cache and verified JDK 21.
- [x] Android narrow tests (7), full Debug tests (125), formatting and Lint passed;
      parsed 15 Lint reports: 0 errors, 146 warnings. Inherited errors fixed.
- [x] Build and verify minified signed universal Release; 14 emulator UI evidence
      checks passed. Physical-phone/full regression not performed; limits documented.
- [x] Reviewed 28-file source pushed at b0a183dd8; unique v0.21.2-joke.1 published.
      All 3 asset digests/sizes verified; anonymous full APK download/hash matched.
- [ ] Owner offline backup of local keystore + keystore.properties (not yet done).

## Before first custom APK

- [x] Confirm first customization scope with the owner: multi-row category wall.
- [x] Set independent app name/icon/applicationId; disable or replace upstream
      updater and telemetry. Preserve licensing/attribution.
- [x] Establish persistent dedicated signing identity; public certificate pin only.
- [ ] Owner OFFLINE backup of .signing/jokemihon-release.p12 + keystore.properties.
- [x] Toolchain/Gradle, 125 unit tests, 0-error Lint, formatting, Release and emulator
      checks passed. Existing prebuilt native dependencies used; limits documented.
- [x] Source/tag/Release/assets/public-download verified. See HANDOFF receipt.
- [ ] Optional owner physical-phone acceptance and density/height feedback.
- [ ] Optionally implement a reviewed release script/CI; do not enable inherited
      upstream Actions as-is.

## Standing requirement

Every completed material code update includes source/documentation synchronization
and verified APK delivery. Docs-only updates require commit/push but no identical
APK rebuild. Track exact blockers; never claim pending publication is complete.