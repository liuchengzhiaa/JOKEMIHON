# JOKEMIHON tasks

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

## Category wall slice (2026-10-25, in progress)

- [x] Owner selected multi-row category wall; fixed toggle, bounded scrolling,
      original order, counts/highlight and direct category jumps implemented.
- [x] Add pure row packer/JUnit cases and pass SDK-free smoke verification.
- [x] Configure independent identity and persistent dedicated local signer; pin
      public certificate only. Disable upstream updater and telemetry.
- [x] Work around full C: and broken bundled JDK with isolated D: cache and verified JDK 21.
- [x] Android narrow tests (7), full Debug tests (125), formatting and Lint passed;
      parsed 15 Lint reports: 0 errors, 146 warnings. Inherited errors fixed.
- [ ] Build/inspect APK and test actual UI on available emulator/device or explicitly
      record lack of device verification.
- [ ] Review/commit/push completed source and publish verified first APK.
- [ ] Owner offline backup of local keystore + keystore.properties (not yet done).

## Before first custom APK

- [x] Confirm first customization scope with the owner: multi-row category wall.
- [x] Set independent app name/icon/applicationId; disable or replace upstream
      updater and telemetry. Preserve licensing/attribution.
- [ ] Establish dedicated persistent signing identity and offline backup; keep keys
      and passwords out of Git. Record public certificate fingerprint only.
- [ ] Verify JDK/SDK/NDK and Gradle/dependencies; run focused tests, full unit tests,
      lint, build and actual APK checks. Document any device-test limitations.
- [ ] Review/commit/push source, verify remote commit, publish a unique Release with
      verified JOKEMIHON.apk and SHA256, then provide the owner its download link.
- [ ] Optionally implement a reviewed release script/CI; do not enable inherited
      upstream Actions as-is.

## Standing requirement

Every completed material code update includes source/documentation synchronization
and verified APK delivery. Docs-only updates require commit/push but no identical
APK rebuild. Track exact blockers; never claim pending publication is complete.