# JOKEMIHON handoff

## Current work: local works manager (2026-10-25, VERIFIED GATES / RELEASE PENDING)

Owner authorized implementation after approving exact leading digits + first letter
folder grouping. Scope/design in docs/LOCAL_WORKS.md. Candidate joke.2/code40, same
package/signer. The category-wall Release below remains the latest published APK.

Implemented: read-only virtual author catalog, flattened lazy cover grid, ordered
selection/reorder/name/category dialogs, memo-backed work identity, portable reader
page covers and More entry. No original media move/delete/write; legacy path stays.
New entries inherit legacy read/bookmark/page once; later old/new progress is independent.
Re-add is idempotent for the same ordered paths, preserving cover/progress and unioning tags.

Verified against this source before the release build:
- Focused model/service suites: 26 tests (17 + 9); all discovered, none skipped.
- Full testDebugUnitTest, lintDebug, app/source-local spotlessCheck: BUILD SUCCESSFUL
  in 7m37s with isolated D: JDK21/cache, serial/in-process/3GB flags in RELEASING.
- All 11 Debug JUnit XML suites: 151 tests, zero failures/errors/skips. All 15 module
  Lint XMLs inspected: zero Error/Fatal, 149 warnings. Resource key parity: 33.
- Fixed a configuration-awareness Lint error with LocalResources + rememberUpdatedState;
  no suppression and no reliance on upstream abortOnError=false. git diff --check passes.
- Covered grouping/Unicode/10k folders, ordered identity/selection/count naming, normal
  protobuf manga-backup round-trip, legacy state/category/idempotence/partial failure,
  concurrent adds, preview behavior and page-cover metadata preservation.
- Local logs/scripts/synthetic media live in ignored .tools/verification; never upload.

Safest next action: commit reviewed source snapshot; build signed/minified Release,
verify package/code40/signer pin/embedded commit, then test exact universal APK upgrade
from published code39 on guarded JOKEMIHON_CategoryWall_Test (ADB5038, port5588).
Prepared synthetic fixtures: nine local folders, five groups, thirteen works; manually
select six in approved order, verify category/reader/page cover/backup and exact 34-file
SHA256 preservation. No device behavior or second APK publication is claimed yet.
Then push normally, unique v0.21.2-joke.2 Release, verify remote assets + anonymous full
APK download and write receipt. Do not touch signer, physical phones or JOKESONG.

---

## Latest delivery: category wall (2026-10-25, PUBLISHED AND VERIFIED)

The completed slice is delivered to the public owner repository. JOKESONG source,
physical phones and personal reading data were not touched. Notes below the first
separator are historical checkpoints, not current pending-publication statements.

### Delivery receipt

- Release: https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.1
- APK: https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.1/JOKEMIHON.apk
- Source/APK commit b0a183dd8061e85c100e66415df3f9aeec07e721; embedded SHA
  b0a183dd8. Annotated tag v0.21.2-joke.1 resolves to the exact source commit.
- Normal main push and API readback verified before publication. This subsequent
  documentation-only receipt advances main; release tag/APK stay immutable at the
  tested code commit. No identical APK rebuild or tag move is needed.
- Release ID 423083096, published 2026-10-25T01:27:14Z; draft=false, prerelease=false.
  Repository PUBLIC; inherited Actions remain disabled.
- All 3 remote asset sizes/digests matched local files before publishing:
  JOKEMIHON.apk: 67794839 bytes; JOKEMIHON.apk.sha256: 80 bytes; build-manifest.json: 3369 bytes.
- APK SHA256: ececfa895e8f03999dac33c7522207498d68c41c971035d4dad5c16b388e3851.
- Independent urllib opener WITHOUT auth/cookies/token read public API and downloaded
  the whole APK: HTTP 200, exact size and SHA256 match the installed/tested universal
  Release. Local receipt: ignored
  .tools/releases/v0.21.2-joke.1/public-download-check/receipt.json.
- Exact 28-file source upload reviewed: source/resources/tests/docs/public helpers
  and public certificate pin only. Keys/APK/config/tool/auth caches stayed ignored.

### Final verification

- testDebugUnitTest lintDebug :app:spotlessCheck passed (11m 12s): 9 Debug JUnit
  suites, 125 tests, 0 failures/errors. All 15 module Lint XMLs inspected: 0 Error/Fatal,
  146 warnings remain. Narrow category-wall tests 7/7 and formatting passed.
- Actual production packer smoke: 8 boundaries, 10000-category case, 500 seeded
  property cases PASS. git diff --check and resource XML parsing passed.
- :app:assembleRelease passed (15m 14s), R8/resource shrinking enabled. Exact universal
  APK (not Debug/ABI-only substitute) verified: app.jokemihon, 0.21.2-joke.1, code 39,
  minSdk 26, targetSdk 36, 4 ABIs. Signature v2, RSA4096, one signer matches public pin.
  Generated COMMIT_SHA and updater=false/telemetry=false inspected.
- Installed exact published Release on isolated Android 16 / API 36 Google APIs x86_64
  AVD JOKEMIHON_CategoryWall_Test. Blank DB exported before inserting 40 synthetic
  categories / 30 fake local books; no media/chapters/covers. Onboarding skipped only
  for fixture setup, not fully tested. AVD stopped after tests; no phone install.
- 14 saved XML/log assertions PASS: English/Chinese controls; expansion; independent
  vertical scroll retaining book 01; direct category 40 empty-page jump; selected
  highlight and expansion retained; collapse synchronization; rotation; 150% font;
  two-line long-label selection; cold restart; existing count switch hide/restore
  via UI; horizontal page swipe to book 08 and synchronized highlight; no FATAL/own
  ANR/SQLite/OOM in final captured log; only deliberate force-stop exit reasons.
- Screenshots for normal wall, category 40, landscape, 150% font inspected. Evidence and
  scripts remain ignored .tools/verification; public build manifest gives test scope.

### Limits and safest next action

- First emulator boot showed one System UI not responding dialog; observed Wait
  recovered. No app.jokemihon crash/ANR found. Do not claim global crash-free behavior.
- Landscape expanded wall crowds/clips upstream empty-category illustration;
  collapse for book area. Selection/scrolling/state restoration still work.
- Not a physical-phone test, performance benchmark or complete reader/extension,
  tracking-login, backup/restore or migration regression. Inherited Comick
  app.mihon callback unchanged; co-installed OAuth flows not validated.
- Independent app.jokemihon coexists with official Mihon; does not automatically
  import its library. Back up original before restore; confirm download directories
  and permissions separately. Upstream updater and telemetry stay disabled.
- Persistent private signer: ignored .signing/jokemihon-release.p12 AND
  keystore.properties. Public certificate SHA256:
  f31ab9d9683c6e60fc92083a2493d754969ab10ae4b765871642a946bf5330a5.
  Owner OFFLINE backup of BOTH remains pending. Local cached duplicate is NOT an
  offline backup. NEVER upload, delete, regenerate or silently rotate the signer.
- Owner should try the APK on a phone and report density/height preferences.
  Future code slices preserve package/signer, increment versionCode above 39, verify,
  and publish NEW tags/assets. No release overwrite. Receipt-only docs need no APK.

---

## Pre-release source checkpoint (historical, superseded by receipt above)

Owner selected a multi-row category wall, not a searchable picker. Work is in this
independent Mihon checkout; JOKESONG source is untouched.

### Implemented

- Fixed right-side expand/collapse arrow; original horizontal tabs remain collapsed.
- Expanded variable-width chips retain order/count preference, selected highlight,
  two-line names and 48dp minimum height. Height is min(280dp, 35% screen height),
  with vertical lazy rows. It stays expanded after a direct stable-ID pager jump.
- rememberSaveable retains expansion through activity recreation; selected row is
  brought into view only when offscreen. No new cover fetch/query/schema migration.
- Independent name/icon/package: JOKEMIHON / app.jokemihon (debug .dev).
  Candidate 0.21.2-joke.1 / versionCode 39. Upstream updater/telemetry disabled;
  release links point to liuchengzhiaa/JOKEMIHON.
- Dedicated persistent signer is ignored .signing/jokemihon-release.p12, configured
  by ignored keystore.properties. Public certificate SHA256 is pinned in
  tools/release-signing-certificate.sha256. NEVER regenerate or upload the key.
  Owner offline backup of BOTH files remains pending; no offline backup claimed.
- Fixed inherited Lint issues without disabling checks: presentation-core default
  splash color and observable calendar locale/weekday cache invalidation.

### Verified source/build checks

- git diff --check and resource XML parsing passed; app:spotlessCheck passed.
- tools/test-category-wall.ps1: 8 boundary cases, 10000-category case and 500
  deterministic property cases PASS against the actual production Kotlin packer.
- Narrow :app:testDebugUnitTest --tests '*CategoryWallRowsTest': 7 tests PASS.
- Final testDebugUnitTest lintDebug :app:spotlessCheck: BUILD SUCCESSFUL in 11m12s.
  Parsed all 9 Debug JUnit suites: 125 tests, 0 failures, 0 errors.
  Inspected all 15 module Lint XMLs: 0 Error/Fatal, 146 warnings remain (including
  upstream/dependency warnings and a screen-height heuristic in the wall).
  Log: ignored .tools/verification/final-checks.log. Do not rely on exit code alone:
  upstream app lint abortOnError=false previously hid 3 locale errors.
- Earlier Debug APK assembly passed; metadata and dedicated certificate inspected.
  That Debug APK predates the calendar Lint fix and is NOT the release deliverable.
- Minified Release build, actual Release UI/startup, remote push and publication
  are still pending. No custom Release/APK is published yet.

### Host build environment / reproducibility

- Bundled Android Studio JDK21 crashes in jimage.dll; left untouched. System JDK17
  cannot run Metro1.4.5. Verified official Adoptium21 lives in ignored
  .tools/jdk/jdk-21.0.12.1+1. Archive SHA256:
  f9d6e191ab098c0d416e7d588a24420a8621cd2f4720dab2459b8b7b2d2d8b4e.
- C: filled during initial wrapper extraction. Removed ONLY this task's failed
  gradle-9.8.1-bin extraction; unrelated caches/files untouched. Keep Gradle home and
  temp on D: via .tools/Set-BuildEnv.ps1 / docs/RELEASING.md. SDK path is ignored.
- Memory-safe flags (PowerShell argument array): --max-workers=1,
  '-Pkotlin.compiler.execution.strategy=in-process',
  '-Dorg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8', --console=plain.
- Isolated empty AVD JOKEMIHON_CategoryWall_Test (Android36 x86_64) is currently
  stopped for build memory. Port5588, separate ADB server5038, localhost:5589.
  Start helper .tools/emulator/Start-TestEmulator.ps1; guard exact AVD name first.
  .tools/verification/ui_probe.py prepares only a blank test library with 40 fake
  categories / 30 fake books, preserving raw baseline separately. No personal media.
  Release fixture injection requires root on this guarded userdebug AVD only.

### Safest next action

Review/stage exact source+docs (never .tools/.signing/keystore.properties), make a
local source commit, then build minified assembleRelease against that commit.
Inspect package/version/signer and test the actual universal Release APK on the
isolated AVD: expansion, vertical scrolling, far empty category, collapse sync,
rotation and large font. If successful, push main normally, publish unique
v0.21.2-joke.1 with JOKEMIHON.apk + SHA256/build manifest, read back remote refs/assets
and anonymously verify the download. Record a documentation-only receipt afterward.
Physical-phone and complete extension/tracker regression remain outside these checks.

---

## Latest continuation: public access and library research (2026-10-25)

This section supersedes the PRIVATE visibility statements in the historical
initialization entry below. The owner explicitly requested PUBLIC visibility so
future APK downloads do not need GitHub sign-in.

### Completed

- Changed only liuchengzhiaa/JOKEMIHON to PUBLIC through the authenticated CLI.
- Verified API visibility=PUBLIC, Actions still disabled and no Releases present.
- Independently verified without authentication: repository API private=false,
  visibility=public, default_branch=main and repository page HTTP 200.
- Updated AGENTS, README, release policy, TASKS and CHANGELOG for public delivery.
- Added docs/MIHON_019_TO_0212_PERFORMANCE.md: source/release-note comparison with
  exact baseline e4de208cf75e273b425bf4b1fe4bd7bc3d9cc459 (v0.19.0) and target
  9612d955db3165e1578fdf9ff19ca9b7762c4a46 (v0.21.2), real data-flow trace,
  relevant commits and explicit limits. Most core changes land in v0.21.0; later
  patches address crashes/migration/Cloudflare resource storms.
- No app/Gradle/signing changes, Android tests/builds, user media reads or device
  installation. No custom APK exists. This is a docs/configuration slice, not an
  APK release. Android build commands remain in docs/RELEASING.md.

### Verification and delivery

- Source baseline/target commits confirmed locally; selected diffs read directly.
- LazyLibraryGrid.kt and LibraryPager.kt are identical between the two tags
  (git diff --quiet exit 0); these are not new lazy-loading changes.
- git diff --check passed; reviewed upload set contains only seven Markdown files.
  No app/Gradle/signing/workflow source modifications. Commit/push succeeded;
  GitHub API verified remote main=4147715d30595861627d492951f3d17330522814.
  This receipt is a documentation-only follow-up; no APK rebuild/release is needed.
- No device traces or controlled performance/crash-rate benchmark: conclusions
  describe observed source mechanisms, not measured speedups or proof of zero bugs.

### Safest next action

Public visibility and research documentation are delivered. Next confirm the
first customization scope, establish independent app identity and dedicated stable
signer, run verification and publish the first tested APK. Keep public uploads free
of secrets and retain archived upstream workflows until a reviewed pipeline exists.

---

## 2026-10-25: repository initialization

Owner requests an independent JOKEMIHON GitHub repository. Future completed code
updates must be committed/pushed there and accompanied by a downloadable APK.
This requirement is persistent in AGENTS.md and docs/RELEASING.md.

### Implementation

- Local project directory: mihon, a sibling of the existing JOKESONG checkout.
- Base: Mihon v0.21.2, commit 9612d955db3165e1578fdf9ff19ca9b7762c4a46.
- Restored upstream history from the original shallow checkout.
- Owner repository: https://github.com/liuchengzhiaa/JOKEMIHON (PRIVATE).
- Source branch main; original local custom/v0.21.2 branch remains at upstream base.
- origin targets the owner; upstream remains reference-only with its push disabled.
- Inherited Actions disabled at repository level before any source push; original
  workflow files archived unchanged under docs/upstream-workflows (not executable).
- Added delivery/continuity documentation, README notice and local-secret/binary
  ignore rules. No application/Gradle source changes in this slice.
- No JOKESONG files modified; no Claude invocation and no device install.

### Verification and delivery status

- Authenticated owner verified through official GitHub CLI: liuchengzhiaa (keyring).
- Repository creation confirmed; initially empty and PRIVATE.
- Upstream Git integrity check passed before initialization; exact v0.21.2 base
  verified. Full-history git fsck --no-reflogs and git diff --check passed.
- Reviewed upload set: AGENTS.md, three docs, README.md, CHANGELOG.md and .gitignore;
  plus three unchanged upstream workflows moved to docs/upstream-workflows;
  no app/Gradle modifications. Ignore probes cover APK/AAB, signing key,
  local.properties, keystore.properties, .env and local tool/auth cache paths.
- Remote settings rechecked: PRIVATE and actions.enabled=false. First source push
  was rejected because OAuth scope lacks workflow permission. Archived inherited
  workflows outside .github/workflows rather than expanding account permissions.
  Retry succeeded without adding OAuth scopes. GitHub API read-back confirmed
  remote main equals local 1999e94e9b263a7ac256f2fb9b48946ab86fb082.
- Delivery receipt: repository PRIVATE, default branch main, Actions disabled,
  0 workflow runs and 0 Releases verified remotely. This receipt is a docs-only
  follow-up commit; no identical APK rebuild/release is required.
- Android unit tests/lint/build/device checks NOT RUN: documentation/repository setup
  only, not a material application code update. No custom APK exists or is published.
- See docs/RELEASING.md for exact candidate verification/build commands.

### Safest next action

1. Repository initialization is delivered; confirm requested product customization.
2. Establish independent name/icon/package, disabled or replaced updater/telemetry
   and a persistent dedicated signing key.
3. Build/verify the first custom APK, then publish to a new versioned Release.

### Constraints and pending setup

- Private downloads require authorized GitHub sign-in; do not make public without
  explicit owner instruction.
- JDK/SDK/NDK/dependency availability has not been tested for this checkout.
- No custom signer/package/version policy or publishing script has been configured.
- Inherited upstream README/download links and workflows are not custom releases.