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
- Current repository visibility is PRIVATE. Do not change visibility without the
  owner's explicit approval. Private downloads require authorized GitHub sign-in.
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