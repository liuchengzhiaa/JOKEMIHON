# JOKEMIHON source and APK delivery

## Destination and owner requirement

- Repository: https://github.com/liuchengzhiaa/JOKEMIHON
- Source branch: main. Base: Mihon v0.21.2 / 9612d955db3165e1578fdf9ff19ca9b7762c4a46.
- Visibility: PUBLIC, explicitly requested by the owner on 2026-10-25 for downloads
  without GitHub sign-in. Source, history and Release assets are public. Changing
  visibility again requires explicit owner approval; review uploads accordingly.
- Every completed material code slice must sync reviewed source/documentation and
  deliver a verified APK via a versioned GitHub Release. Docs-only work needs no APK.
- Keep APK/AAB binaries outside Git history. The constant future asset name is
  JOKEMIHON.apk; use a new versioned tag for every release.

## Current initialization

First custom candidate: 0.21.2-joke.1 (39), package app.jokemihon. Category wall,
name/icon and disabled upstream updater/telemetry are implemented. Persistent local
signing configured; public certificate pinned in tools/release-signing-certificate.sha256.
No verified APK is published yet; see latest HANDOFF for exact build/test state. Do not mistake upstream README download badges
or upstream version tags for JOKEMIHON releases.

The initial shallow checkout was expanded to preserve original commit history.
origin is the owner's repository; upstream is for reference only, with local push
URL disabled. Do not push upstream tags/releases wholesale.

Repository Actions are disabled before the initial source push. The inherited workflows are preserved unchanged under docs/upstream-workflows,
outside the executable .github/workflows directory. They target upstream behavior
and are not a configured JOKEMIHON publishing pipeline. Publication is manual until reviewed automation is
implemented. Never restore/enable inherited workflows blindly. The current official CLI token
has repo but not workflow scope; the first push with executable upstream workflows
was rejected. Archiving them avoids extra authorization and accidental execution.

## Before the first APK

1. Follow CONTRIBUTING.md#forks: independent name, icon and applicationId; change or
   disable upstream update checking and avoid upstream analytics/crash reporting.
2. Establish a dedicated persistent local signing key, ignored keystore.properties
   and offline backup. Never use a fresh ephemeral debug key for each published APK.
   Never copy JOKESONG's signing key or upload a private key. Record only the public
   signing certificate SHA256 and selected package ID in repository documentation.
3. Confirm Android tooling from this checkout: JDK 21 is selected by upstream CI;
   Gradle wrapper is 9.8.1, compile SDK 37.2 and NDK 29.0.14206865. Confirm actual
   availability before changing dependency versions; see HANDOFF for completed checks.
4. Keep private SDK paths in ignored local.properties and tool/auth caches out of Git.

## Verification and publication

Run focused tests first. Standard verification commands:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug -Pinclude-telemetry=false -Penable-updater=false
.\gradlew.bat assembleRelease -Pinclude-telemetry=false -Penable-updater=false
```

Use the dedicated custom signer established above. Verify lint results, not just
its exit code (the upstream app sets abortOnError=false). Test the actual resulting
APK's startup and changed behavior on a device/emulator when available; report any
unperformed device checks explicitly. Never copy JOKESONG's Gradle init script
without assessing compatibility with this multi-module project.

1. Update versionCode/versionName, CHANGELOG, HANDOFF and TASKS. Preserve upstream
   history and licensing. Review tracked files and git diff before staging.
2. Check APK applicationId, versionCode/versionName and signer using Android SDK
   tools. Calculate file size and SHA256. Record exact verification outcomes and
   build/source commit, including limitations.
3. Commit and push main normally to origin. Read back refs/heads/main and compare
   its SHA with the intended local commit. Resolve conflicts without force-pushing.
4. Using the normal authenticated GitHub CLI, create a unique release targeting
   that verified source commit. Attach JOKEMIHON.apk, its SHA256 and release notes.
   Never put tokens in scripts, source, remote URLs, notes or chat. Never overwrite
   an existing asset or tag. Inspect partial publication before resuming it.
5. Read back release target, assets, sizes and available digests (or download and
   hash the remote APK) before reporting success. Give the owner the exact release
   and APK URLs. Published public Release assets do not require GitHub sign-in.
6. Record the verified publication receipt in HANDOFF. A receipt-only documentation
   commit does not require rebuilding/releasing an identical APK.

If blocked, record the exact failed command and safest next action instead of
claiming delivery. No automated publishing script or CI signer is configured yet.
## This host's isolated build environment (2026-10-25)

The bundled Android Studio JDK 21 crashes; C: is nearly full. Do not delete user
files, alter other projects or downgrade this project's dependencies to mask it.
Use the checksum-verified, project-local JDK and D: cache created for this task:

```powershell
$root = (Get-Location).Path
$env:JAVA_HOME = (Resolve-Path '.tools/jdk/jdk-21.0.12.1+1').Path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$env:GRADLE_USER_HOME = Join-Path $root '.tools/gradle-user-home'
$env:TEMP = Join-Path $root 'tmp'
$env:TMP = $env:TEMP
# If Git ownership differs for this terminal, scope safe.directory to this process;
# use the current checkout path, never a global wildcard exception.
$env:GIT_CONFIG_COUNT = '1'
$env:GIT_CONFIG_KEY_0 = 'safe.directory'
$env:GIT_CONFIG_VALUE_0 = $root.Replace('\', '/')
.\tools\test-category-wall.ps1
.\gradlew.bat :app:testDebugUnitTest --tests '*CategoryWallRowsTest' --max-workers=2 --console=plain
.\gradlew.bat testDebugUnitTest --max-workers=2 --console=plain
.\gradlew.bat lintDebug --max-workers=2 --console=plain
.\gradlew.bat assembleRelease --max-workers=2 --console=plain
```

SDK location is only in ignored local.properties. No JOKESONG ASCII init script is
needed for this ASCII checkout. Missing SDK components may require setup on a new host. Use HANDOFF for actual
verification results; the isolated JVM smoke alone does not establish Android success.

Never overwrite/regenerate .signing/jokemihon-release.p12 or local signing
properties. The owner must back up BOTH files offline to retain update capability;
the key has been generated locally but an offline backup is not yet confirmed.
For memory-constrained full checks on this host, use a string argument array in
PowerShell (quote dotted -P/-D properties; do not let PowerShell split them):

```powershell
$flags = @('--max-workers=1', '-Pkotlin.compiler.execution.strategy=in-process',
           '-Dorg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8', '--console=plain')
.\gradlew.bat testDebugUnitTest lintDebug :app:spotlessCheck @flags
.\gradlew.bat assembleRelease @flags
```

Stop only this project's dedicated Gradle daemon before emulator UI verification to
release memory. The isolated test AVD is JOKEMIHON_CategoryWall_Test, port 5588;
ADB server 5038 connects to localhost:5589. It contains only synthetic test data.
Do not install to a physical phone or reuse another project's emulator implicitly.