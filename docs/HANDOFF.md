# JOKEMIHON handoff

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
  No app/Gradle/signing/workflow source modifications. Remote sync pending.
- No device traces or controlled performance/crash-rate benchmark: conclusions
  describe observed source mechanisms, not measured speedups or proof of zero bugs.

### Safest next action

Finish documentation commit/push and verify remote SHA/settings. Then confirm the
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