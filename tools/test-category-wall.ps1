param([string] $GradleLib)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to a working JDK first.' }
if (-not $GradleLib) {
    $gradleCacheHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path $HOME '.gradle' }
    $compiler = Get-ChildItem -LiteralPath (Join-Path $gradleCacheHome 'wrapper/dists/gradle-9.8.1-bin') -Recurse -Filter 'kotlin-compiler-embeddable-*.jar' | Select-Object -First 1
    if (-not $compiler) { throw 'Run gradlew --version first, or pass -GradleLib.' }
    $GradleLib = $compiler.DirectoryName
}
$stdlib = Get-ChildItem -LiteralPath $GradleLib -Filter 'kotlin-stdlib-*.jar' | Select-Object -First 1
$out = Join-Path $root '.tools/category-wall-smoke'
New-Item -ItemType Directory -Path $out -Force | Out-Null
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
& $java -cp "$GradleLib/*" org.jetbrains.kotlin.cli.jvm.K2JVMCompiler -no-stdlib -no-reflect -jvm-target 17 -classpath $stdlib.FullName -d $out (Join-Path $root 'app/src/main/java/eu/kanade/presentation/library/components/CategoryWallRows.kt') (Join-Path $PSScriptRoot 'CategoryWallRowsSmoke.kt')
if ($LASTEXITCODE -ne 0) { throw 'Standalone Kotlin compilation failed.' }
& $java -cp "$out;$($stdlib.FullName)" eu.kanade.presentation.library.components.CategoryWallRowsSmokeKt
if ($LASTEXITCODE -ne 0) { throw 'Standalone category wall checks failed.' }
