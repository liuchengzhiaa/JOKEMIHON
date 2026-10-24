# JOKEMIHON handoff

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
  Source push retry and verification pending.
- Android unit tests/lint/build/device checks NOT RUN: documentation/repository setup
  only, not a material application code update. No custom APK exists or is published.
- See docs/RELEASING.md for exact candidate verification/build commands.

### Safest next action

1. Finish initialization commit/push and verify remote main SHA, visibility and
   disabled Actions. Record actual receipt here.
2. Confirm requested product customization; establish independent name/icon/package,
   disabled or replaced updater/telemetry and a persistent dedicated signing key.
3. Build/verify the first custom APK, then publish to a new versioned Release.

### Constraints and pending setup

- Private downloads require authorized GitHub sign-in; do not make public without
  explicit owner instruction.
- JDK/SDK/NDK/dependency availability has not been tested for this checkout.
- No custom signer/package/version policy or publishing script has been configured.
- Inherited upstream README/download links and workflows are not custom releases.