# JOKEMIHON repository instructions

This is the owner's independent Mihon customization, based on upstream tag v0.21.2
(commit 9612d955db3165e1578fdf9ff19ca9b7762c4a46), not JOKESONG.

Read docs/HANDOFF.md, docs/TASKS.md and docs/RELEASING.md before continuing.
Preserve upstream LICENSE, attribution and history. See CONTRIBUTING.md#forks.
Do not invoke Claude Code on the owner's behalf unless explicitly authorized.

## Standing delivery requirement (owner request, 2026-10-25)

For each completed material code update:
1. Update CHANGELOG.md, docs/HANDOFF.md and docs/TASKS.md.
2. Run relevant tests, full unit tests and lint; build and validate the APK.
3. Review the exact upload set; commit and push to the owner's JOKEMIHON repository.
4. Publish the verified APK as a versioned GitHub Release attachment and give the
   owner the download link. Verify remote commit and assets before claiming success.

Documentation-only updates require commit/push, not another identical APK.
If authentication, network, tests or builds fail, retain work and record exact
pending steps. Never call an unbuilt APK tested or an unpublished APK published.

## Boundaries and release safety

- origin: https://github.com/liuchengzhiaa/JOKEMIHON.git (owner repository).
- upstream: https://github.com/mihonapp/mihon.git (reference only; never push).
- Work on main or focused local branches; never force-push or rewrite shared history.
- Current repository visibility is PUBLIC, explicitly requested by the owner on
  2026-10-25 for sign-in-free APK downloads. Source, history and Release assets are
  public. Do not change visibility again without explicit owner approval.
- Never commit/upload credentials, tokens, signing private keys, local.properties,
  .env, machine-local settings, tool/auth caches, user media or backups.
- APK/AAB files belong in Release assets, not Git history.
- Before the first custom APK, establish independent app name, icon, applicationId,
  updater/telemetry behavior and stable signing identity. Do not reuse JOKESONG's
  applicationId or signing key. Preserve the custom ID/signer for later updates.
- Disable upstream updater and telemetry for custom builds until intentionally
  replaced. Avoid sending custom usage/crash data to upstream services.
- Increase versionCode for each newly published APK. Never overwrite existing
  release assets, reuse release tags or silently rotate the signing key.
- Inherited upstream Actions are disabled at repository level and archived under
  docs/upstream-workflows (not executable). Review/replace their
  triggers, destinations, permissions and signing before enabling any workflows.
- Keep originals and user reading data safe; migrations need tests and backups.
- Add/update tests for non-trivial state, data and migration behavior.
## Established first release (2026-10-25)

- app.jokemihon / JOKEMIHON, first published version 0.21.2-joke.1, versionCode 39.
  Preserve package ID and dedicated signer; use the latest versionCode floor below.
- Pin: tools/release-signing-certificate.sha256. Private signer is ignored
  .signing/jokemihon-release.p12 plus ignored keystore.properties. NEVER delete or
  regenerate either. Owner offline backup remains pending; tool-cache duplicates
  are not offline backups. Never upload signing material.
- Release/source tag v0.21.2-joke.1 points to b0a183dd8. Receipt-only docs can
  advance main without rebuilding that exact APK or moving its tag.

## Previous device-checked release (2026-10-25)

- Published 0.21.2-joke.2 / code40, same app.jokemihon and pinned signer. Superseded versionCode floor is below.
- Source/tag f4630606ea10b0d89d1cccb5270c2ff35b573740 / v0.21.2-joke.2 immutable.
- Local works are namespaced memo-backed original-path references; never migrate or
  rewrite physical media to make virtual entries. Preserve backup compatibility and
  independent old/new progress semantics documented in docs/LOCAL_WORKS.md.
- See HANDOFF for final 151-test/0-error-Lint/Release/device/public-download receipt.

## Latest preview: joke.6 (2026-10-25)

- PUBLIC prerelease0.21.2-joke.6/code44; next APK code>44, same package/signer config.
- Source/tag7842e5f03797a1b5a5afae3681a74d8c38241b21 / v0.21.2-joke.6 immutable.
- Legacy original-directory URL compatibility: only complete one-work originals can
  alias a single; exact virtual favorites win. Tags load/save actual explicit manga
  ID. Multi-work original/collection tags shown as separate context, not propagated
  as child ownership; explicit original-entry editor and author aggregate markers.
- No schema/memo/backup107/108 migration or automatic file/tag mutation. Preserve
  actual original IDs/progress. Same resolver for batch prefill/add (live coverage).
- Continued no-test slice: Release compile9m53s only; no unit/Lint/format/static-signer/
  device checks and no owner DB inspected.3 remote asset digests/sizes/public API
  delivery readback confirmed. Owner phone acceptance pending; no old tests as proof.
  Resume standing verification for future work unless owner requests otherwise.

## Previous preview: joke.5 (2026-10-25)

- PUBLIC prerelease0.21.2-joke.5 / code43; next APK code>43, same package/signer config.
- Source/tag438de729ced6e71ac5048230cfc1d489050d423a / v0.21.2-joke.5 immutable.
- Logical work-directory creation is separate from import. Exact directory/child
  library identities/categories/covers; no member-to-parent fallback. Old favorited
  combinations adopted without identity migration. Original media never moved.
- Optional108 includes ALL work directories/names/order/page covers with Library,
  including unfavorited directories; independent of App settings. Keep107 unchanged.
  Absent108 preserves existing records; present108 validated before replacement.
- Continued owner no-test exception for this slice: no unit/Lint/format/static-signer/
  emulator/device checks. Final Release compile9m11s only. Remote asset sizes/digests
  and anonymous public API delivery readback confirmed, phone acceptance pending.
  Do not reuse old tests/device evidence as code43 proof. Default future verification
  remains the standing rule unless owner requests otherwise. See LOCAL_WORKS/HANDOFF.

## Previous preview: joke.4 (2026-10-25)

- PUBLIC prerelease 0.21.2-joke.4 / code42; next APK code>42, same package/signer config.
- Source/tag ea772c0fa161da903f917441c3c5e03ae8586e83 / v0.21.2-joke.4 immutable.
- Optional original unfavorite only after combined target saved and full coverage;
  keep partial originals, files and chapter/history/category rows. See LOCAL_WORKS.
- Explicit owner exception THIS SLICE: NO tests/Lint/format/device checks. Build only
  passed9m42s. Remote asset digests/sizes and public API delivery readback confirmed.
  Owner acceptance pending; never reuse older tests as joke.4 evidence. Resume normal
  standing verification for future slices unless owner gives another exception.

## Previous preview: joke.3 (2026-10-25)

- Published PUBLIC prerelease 0.21.2-joke.3 / code41, same package/signer; next APK code>41.
- Source/tag c2f1fd2872178c757da6961af8e06240c514c312 / v0.21.2-joke.3 immutable.
- Manual logical-directory rules are original-name presentation mappings; optional
  backup field107 travels with Library. Do not write them to user media or make them
  depend on App settings. Keep old absent-field backups compatible.
- Categories now support explicit removal; untouched mixed batch tags stay intact.
- 162 tests/0-error Lint/static APK/public download verified. This slice explicitly
  used simple validation by owner request; NO code41 device run. Owner acceptance
  pending. Do not conflate previous joke.2 AVD evidence with this preview.
