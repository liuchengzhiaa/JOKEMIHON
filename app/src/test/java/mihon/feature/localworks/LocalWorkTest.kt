package mihon.feature.localworks

import eu.kanade.tachiyomi.data.backup.models.BackupManga
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tachiyomi.source.local.work.LocalWorkCover
import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkPath
import tachiyomi.source.local.work.groupLocalAuthors
import tachiyomi.source.local.work.localAuthorKey

class LocalWorkTest {
    private val first = LocalWorkPath("423黄金紳士倶楽部01", "作品D 上.zip")
    private val second = LocalWorkPath("423黄金紳士倶楽部05", "作品sjwnD 下.zip")

    @Test
    fun `groups by digits and first letter not complete author name`() {
        val groups = groupLocalAuthors(listOf("423黄金紳士倶楽部01", "423黄其他名称05", "423白其他01", "432長い草", "432長い草2"))
        groups.size shouldBe 3
        groups.first { it.key == "author:423:黄" }.directories.size shouldBe 2
        groups.first { it.key == "author:432:長" }.directories.size shouldBe 2
    }

    @Test
    fun `punctuation and spaces are skipped and unicode letters retained`() {
        localAuthorKey("423（01） 黄金") shouldBe "author:423:黄"
        localAuthorKey("12 [かな]") shouldBe "author:12:か"
        localAuthorKey("12 Alpha") shouldBe "author:12:A"
        localAuthorKey("12𠮷野") shouldBe "author:12:𠮷"
    }

    @Test
    fun `digit prefix is exact including leading zeros and letter case`() {
        (localAuthorKey("0423黄") == localAuthorKey("423黄")) shouldBe false
        (localAuthorKey("12Alpha") == localAuthorKey("12alpha")) shouldBe false
    }

    @Test
    fun `no leading digits or no letters remains an independent folder`() {
        localAuthorKey("作者423") shouldBe null
        localAuthorKey("123456 ()") shouldBe null
        groupLocalAuthors(listOf("作者", "作者2", "123", "1234")).size shouldBe 4
    }

    @Test
    fun `natural ordering gives deterministic display name and drops only trailing split digits`() {
        val group = groupLocalAuthors(listOf("423黄10", "423黄02", "423黄01")).single()
        group.name shouldBe "423黄"
        group.directories shouldBe listOf("423黄01", "423黄02", "423黄10")
    }

    @Test
    fun `hidden and duplicated names do not add phantom groups`() {
        groupLocalAuthors(listOf(".private", "", "432長い草", "432長い草")).size shouldBe 1
    }

    @Test
    fun `large root snapshot keeps every original directory exactly once`() {
        val folders = (1..10000).map { "$it 作者01" }
        val result = groupLocalAuthors(folders)
        result.size shouldBe 10000
        result.flatMap { it.directories }.toSet() shouldBe folders.toSet()
    }

    @Test
    fun `file extension is stripped while image-directory name is retained`() {
        first.displayName shouldBe "作品D 上"
        LocalWorkPath("作者", "作品Dh 下").displayName shouldBe "作品Dh 下"
        LocalWorkPath("作者", "Book.CBZ").displayName shouldBe "Book"
    }

    @Test
    fun `same basename in different source directories has different identity`() {
        LocalWorkDefinition(listOf(first)).url shouldBe LocalWorkDefinition(listOf(first)).url
        val other = LocalWorkPath("423黄金紳士倶楽部02", first.entry)
        (LocalWorkDefinition(listOf(first)).url == LocalWorkDefinition(listOf(other)).url) shouldBe false
    }

    @Test
    fun `ordered collection is stable and a different order is intentionally distinct`() {
        val definition = LocalWorkDefinition(listOf(first, second))
        definition.url.length shouldBe LocalWorkDefinition.URL_PREFIX.length + 64
        (definition.url == LocalWorkDefinition(listOf(second, first)).url) shouldBe false
        LocalWorkDefinition.fromMemo(definition.url, definition.toMemo()).members shouldBe listOf(first, second)
    }

    @Test
    fun `memo keeps other namespaces and rejects mismatched identity`() {
        val definition = LocalWorkDefinition(listOf(first))
        val memo = definition.toMemo(JsonObject(mapOf("other" to JsonPrimitive("untouched"))))
        memo["other"] shouldBe JsonPrimitive("untouched")
        assertThrows(IllegalArgumentException::class.java) {
            LocalWorkDefinition.fromMemo(LocalWorkDefinition(listOf(second)).url, memo)
        }
    }

    @Test
    fun `invalid membership and traversal references are refused`() {
        assertThrows(IllegalArgumentException::class.java) { LocalWorkDefinition(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { LocalWorkDefinition(listOf(first, first)) }
        assertThrows(IllegalArgumentException::class.java) {
            LocalWorkDefinition(List(1001) { LocalWorkPath("author", "$it.zip") })
        }
        for (value in listOf("..", ".", "a/b", "a\\b", "", ".hidden")) {
            assertThrows(IllegalArgumentException::class.java) { LocalWorkPath(value, "book.zip") }
            assertThrows(IllegalArgumentException::class.java) { LocalWorkPath("author", value) }
        }
        assertThrows(IllegalArgumentException::class.java) { LocalWorkPath.fromRelativeUrl("a/b/c") }
    }

    @Test
    fun `cover descriptor preserves original file and exact page including unicode`() {
        val cover = LocalWorkCover(first, 12)
        LocalWorkCover.fromUrl(cover.toUrl()) shouldBe cover
        assertThrows(IllegalArgumentException::class.java) { LocalWorkCover(first, -1) }
        assertThrows(IllegalArgumentException::class.java) { LocalWorkCover.fromUrl("file:///outside") }
    }

    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @Test
    fun `ordinary protobuf manga backup round trip preserves membership order and chosen page cover`() {
        val definition = LocalWorkDefinition(listOf(first, second))
        val cover = LocalWorkCover(second, 4)
        val backup =
            BackupManga(
                source = 0,
                url = definition.url,
                title = "作品D 上（六部曲）",
                thumbnailUrl = cover.toUrl(),
                memo = Json.encodeToString(JsonObject.serializer(), definition.toMemo()).toByteArray(),
            )
        val restored = ProtoBuf.decodeFromByteArray(
            BackupManga.serializer(),
            ProtoBuf.encodeToByteArray(BackupManga.serializer(), backup),
        ).getMangaImpl()
        LocalWorkDefinition.fromMemo(restored.url, restored.memo) shouldBe definition
        LocalWorkCover.fromUrl(restored.thumbnailUrl!!) shouldBe cover
        restored.title shouldBe backup.title
    }

    @Test
    fun `selection preserves click order and reselect appends`() {
        var selected = toggleLocalWorkSelection(emptyList(), second)
        selected = toggleLocalWorkSelection(selected, first)
        selected shouldBe listOf(second, first)
        selected = toggleLocalWorkSelection(selected, second)
        toggleLocalWorkSelection(selected, second) shouldBe listOf(first, second)
    }

    @Test
    fun `selection arrows change first work without losing members`() {
        moveLocalWorkSelection(listOf(first, second), 1, -1) shouldBe listOf(second, first)
        assertThrows(IllegalArgumentException::class.java) { moveLocalWorkSelection(listOf(first), 0, -1) }
    }

    @Test
    fun `collection numerals include six part example and decimal gaps`() {
        listOf(1, 6, 10, 11, 20, 21, 100, 101, 110, 1000).map(::localCollectionNumeral) shouldBe
            listOf("一", "六", "十", "十一", "二十", "二十一", "一百", "一百零一", "一百一十", "一千")
    }
}
