# JOKEMIHON tasks

## Initialization (2026-10-25)

- [x] Download and verify upstream v0.21.2 source in an independent local checkout.
- [x] Expand shallow checkout to retain upstream commit history.
- [x] Create the owner's PRIVATE JOKEMIHON repository and disable inherited Actions.
- [x] Document standing source/APK delivery and continuity requirements.
- [ ] Commit/push reviewed initialization and verify remote SHA and settings.

## Before first custom APK

- [ ] Confirm first customization scope with the owner.
- [ ] Set independent app name/icon/applicationId; disable or replace upstream
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