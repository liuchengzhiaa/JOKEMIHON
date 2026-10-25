# JOKEMIHON

Personal customization of [Mihon](https://github.com/mihonapp/mihon), based on
**v0.21.2**. Independent project; not an official Mihon release.

- [Project status and handoff](docs/HANDOFF.md)
- [Expandable category wall](docs/CATEGORY_WALL.md)
- [Non-destructive local works manager](docs/LOCAL_WORKS.md)
- [Tasks](docs/TASKS.md) | [Source and APK delivery](docs/RELEASING.md)
- [v0.19.0 to v0.21.2 library performance analysis](docs/MIHON_019_TO_0212_PERFORMANCE.md)
- [JOKEMIHON releases](https://github.com/liuchengzhiaa/JOKEMIHON/releases)

**New preview: [0.21.2-joke.6 · existing legacy library tags](https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.6).**
[Download preview APK](https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.6/JOKEMIHON.apk)
without sign-in. Code44, unchanged package/signing config. Fix raw-directory legacy
book vs virtual hash mismatch: complete single-work originals reuse existing IDs,
tags and progress; category picker reads/saves the actual explicit manga ID. Multiple-
work parent/collection tags appear separately with an explicit original-entry editor,
not automatically assigned to every child. Aggregate author markers added. No data/
backup migration or automatic tag copying. **Build only, no tests; owner phone/DB
acceptance pending. Back up before installing.**

**Previous preview: [0.21.2-joke.5 · independent work directories and tag grid](https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.5).**
[Download preview APK](https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.5/JOKEMIHON.apk)
without sign-in. Code43, existing package/signing configuration. Creating a logical
work directory no longer imports it; click to browse children, long-press or Directory
options to import the directory. Directory/children have independent tags/covers.
Existing combinations retain their identities. Vector Label badge shows named tags;
Choose tags opens a searchable, filled/partial-state grid. Library backup includes
unfavorited directory names/members/order/page covers via108, author groups via107;
include Chapters/Categories too, preserve media paths and regrant storage on restore.
**NO tests run this slice; Release build only. Back up before install; owner phone
and new-phone restore acceptance pending.** Older checks below do not validate code43.

**Previous preview: [0.21.2-joke.4 · optional replacement of combined originals](https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.4).**
[Download preview APK](https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.4/JOKEMIHON.apk)
without sign-in. Code42, existing package/signing configuration. Combined import now
has a default-off option to unfavorite fully covered originals after saving the new
collection. Partial originals remain. No media/chapters/history are deleted. Back up
BEFORE replacement; new library backups omit unfavorited entries. **NO tests run by
explicit owner request** (no unit/Lint/format/device tests); Release build only.
Owner phone acceptance pending. Prior version checks below do not validate this build.

**Previous preview: [0.21.2-joke.3 · directory grouping and UI polish](https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.3).**
[Download preview APK](https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.3/JOKEMIHON.apk)
without sign-in. Code41, same signer; back up before upgrading. Long-press directory
cards to merge/rename/ungroup; work checkboxes appear only after long-press. Existing
work preview supports removable categories. Manual groups/names join Library backups
independently of App settings. Include Chapters/Categories; comics are not copied.
162 unit tests/zero Lint errors (152 warnings), formatting and APK static checks pass.
Per owner request no repeated device regression; phone UI/upgrade/restore acceptance
is pending. This is a pre-release; the prior device-checked stable/latest stays below.

**Previous device-checked release: [0.21.2-joke.2 · local works manager](https://github.com/liuchengzhiaa/JOKEMIHON/releases/tag/v0.21.2-joke.2).**
[Download JOKEMIHON.apk](https://github.com/liuchengzhiaa/JOKEMIHON/releases/download/v0.21.2-joke.2/JOKEMIHON.apk)
without GitHub sign-in. Same package/signer as joke.1, versionCode40; back up before upgrading.

More > 本地作品管理 groups existing author folders without reorganizing files. Add works
separately or combine them in selection order, name the bundle and choose category tags.
Long-press a reader page to choose a per-work cover. Original directories/old library stay;
initial progress is inherited once, then old/new progress is independent. Normal backups
retain definitions/page-cover references, not comic files. See [design/limits](docs/LOCAL_WORKS.md).

Expandable category wall remains available: tap the arrow on the right of category tabs.
Independent package app.jokemihon does not overwrite/import official Mihon automatically.
151 unit tests, zero Lint errors (149 warnings), signed Release upgrade/SAF/reader/cover/
actual backup-restore synthetic emulator checks and anonymous APK hash verification pass.
Physical-phone/full extension/large real-library tests are not implied; see HANDOFF.
Repository is public by owner request.
Original upstream introduction/download links below are retained for attribution,
NOT as JOKEMIHON download links.

Modified for JOKEMIHON on 2026-10-25: added this project notice. Original licensing
and credits are retained below.

---

<div align="center">

<a href="https://mihon.app">
    <img src="./.github/assets/logo.png" alt="Mihon logo" title="Mihon logo" width="80"/>
</a>

# Mihon [App](#)

### Full-featured reader
Discover and read manga, webtoons, comics, and more – easier than ever on your Android device.

[![Discord server](https://img.shields.io/discord/1195734228319617024.svg?label=&labelColor=6A7EC2&color=7389D8&logo=discord&logoColor=FFFFFF)](https://discord.gg/mihon)
[![GitHub downloads](https://img.shields.io/github/downloads/mihonapp/mihon/total?label=downloads&labelColor=27303D&color=0D1117&logo=github&logoColor=FFFFFF&style=flat)](https://mihon.app/download)

[![CI](https://img.shields.io/github/actions/workflow/status/mihonapp/mihon/build.yml?labelColor=27303D)](https://github.com/mihonapp/mihon/actions/workflows/build_push.yml)
[![License: Apache-2.0](https://img.shields.io/github/license/mihonapp/mihon?labelColor=27303D&color=0877d2)](/LICENSE)
[![Translation status](https://img.shields.io/weblate/progress/mihon?labelColor=27303D&color=946300)](https://hosted.weblate.org/engage/mihon/)

## Download

[![Mihon Stable](https://img.shields.io/github/release/mihonapp/mihon.svg?maxAge=3600&label=Stable&labelColor=06599d&color=043b69)](https://mihon.app/download)
[![Mihon Beta](https://img.shields.io/github/v/release/mihonapp/mihon-preview.svg?maxAge=3600&label=Beta&labelColor=2c2c47&color=1c1c39)](https://mihon.app/download)

*Requires Android 8.0 or higher.*

## Features

<div align="left">

* Local reading of content.
* A configurable reader with multiple viewers, reading directions and other settings.
* Tracker support: [MangaBaka](https://mangabaka.org), [MyAnimeList](https://myanimelist.net/), [AniList](https://anilist.co/), [Kitsu](https://kitsu.app/), [MangaUpdates](https://mangaupdates.com), [Shikimori](https://shikimori.one), [Bangumi](https://bgm.tv/), and [Hikka](https://hikka.io/) support.
* Categories to organize your library.
* Light and dark themes.
* Schedule updating your library for new chapters.
* Create backups locally to read offline or to your desired cloud service.
* Plus much more...

</div>

## Contributing

[Code of conduct](./CODE_OF_CONDUCT.md) · [Contributing guide](./CONTRIBUTING.md)

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Before reporting a new issue, take a look at the [FAQ](https://mihon.app/docs/faq/general), the [changelog](https://mihon.app/changelogs/) and the already opened [issues](https://github.com/mihonapp/mihon/issues); if you got any questions, join our [Discord server](https://discord.gg/mihon).


### Repositories

[![mihonapp/website - GitHub](https://github-stats-extended.vercel.app/api/pin/?username=mihonapp&repo=website&bg_color=161B22&text_color=c9d1d9&title_color=0877d2&icon_color=0877d2&border_radius=8&hide_border=true&description_lines_count=2)](https://github.com/mihonapp/website/)
[![mihonapp/bitmap.kt - GitHub](https://github-stats-extended.vercel.app/api/pin/?username=mihonapp&repo=bitmap.kt&bg_color=161B22&text_color=c9d1d9&title_color=0877d2&icon_color=0877d2&border_radius=8&hide_border=true&description_lines_count=2)](https://github.com/mihonapp/bitmap.kt/)

### Credits

Thank you to all the people who have contributed!

<a href="https://github.com/mihonapp/mihon/graphs/contributors">
    <img src="https://contrib.rocks/image?repo=mihonapp/mihon" alt="Mihon app contributors" title="Mihon app contributors" width="800"/>
</a>

### Disclaimer

The developer(s) of this application does not have any affiliation with the content providers available, and this application hosts zero content.

### License

<pre>
Copyright © 2015 Javier Tomás
Copyright © 2024 Mihon Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>

</div>
