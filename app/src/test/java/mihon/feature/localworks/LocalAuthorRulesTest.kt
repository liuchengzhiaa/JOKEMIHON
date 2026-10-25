package mihon.feature.localworks

import eu.kanade.tachiyomi.data.backup.models.Backup
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.protobuf.ProtoBuf
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.source.local.work.LocalAuthorRule
import tachiyomi.source.local.work.LocalAuthorRules
import tachiyomi.source.local.work.LocalWorkDefinition
import tachiyomi.source.local.work.LocalWorkPath
import tachiyomi.source.local.work.groupLocalAuthors

class LocalAuthorRulesTest {
    private val auto = groupLocalAuthors(listOf("12作者01", "12作者02", "55別作者", "无编号"))
    private val first = auto.first { "55別作者" in it.directories }
    private val second = auto.first { "12作者01" in it.directories }

    @Test
    fun `manual grouping overrides auto groups and retains click order`() {
        val rules = LocalAuthorRules().save(listOf(first, second), first.name, "test-group")
        val applied = rules.applyTo(auto)
        applied.size shouldBe 2
        val merged = applied.single { it.key == "manual:test-group" }
        merged.name shouldBe first.name
        merged.directories shouldBe first.directories + second.directories
        applied.flatMap { it.directories }.toSet() shouldBe auto.flatMap { it.directories }.toSet()
    }

    @Test
    fun `partial override leaves remaining automatic folders and new folders visible`() {
        val rules = LocalAuthorRules(listOf(LocalAuthorRule("one", "自定义", listOf("12作者01"))))
        val applied = rules.applyTo(groupLocalAuthors(listOf("12作者01", "12作者02", "99新增")))
        applied.size shouldBe 3
        applied.single { it.key == "author:12:作" }.directories shouldBe listOf("12作者02")
        applied.flatMap { it.directories }.size shouldBe 3
    }

    @Test
    fun `repeated merging flattens records and remembers temporarily missing folders`() {
        val rules = LocalAuthorRules(listOf(LocalAuthorRule("old", "旧组", listOf("12作者01", "暂时离线"))))
        val existing = rules.applyTo(auto).single { it.key == "manual:old" }
        val merged = rules.save(listOf(existing, first), "新组", "new")
        merged.rules.size shouldBe 1
        merged.rules.single().directories shouldBe listOf("12作者01", "暂时离线", "55別作者")
        merged.applyTo(auto).flatMap { it.directories }.distinct().size shouldBe 4
        merged.applyTo(groupLocalAuthors(listOf("暂时离线"))).single().name shouldBe "新组"
    }

    @Test
    fun `rename and dissolve never change work identity`() {
        val path = LocalWorkPath("12作者01", "作品.zip")
        val before = LocalWorkDefinition(listOf(path)).url
        val rules = LocalAuthorRules().save(listOf(second), "别名", "alias")
        rules.applyTo(auto).single { it.key == "manual:alias" }.name shouldBe "别名"
        LocalWorkDefinition(listOf(path)).url shouldBe before
        rules.dissolve(setOf("manual:alias")).applyTo(auto) shouldBe auto
    }

    @Test
    fun `invalid rule or overlapping physical paths is rejected`() {
        assertThrows(IllegalArgumentException::class.java) { LocalAuthorRule("x", "title", listOf("../escape")) }
        assertThrows(IllegalArgumentException::class.java) { LocalAuthorRule("x", " ", listOf("safe")) }
        assertThrows(IllegalArgumentException::class.java) {
            LocalAuthorRules(
                listOf(LocalAuthorRule("x", "one", listOf("same")), LocalAuthorRule("y", "two", listOf("same"))),
            )
        }
        assertThrows(IllegalArgumentException::class.java) { LocalAuthorRules(version = 2) }
    }

    @Test
    fun `ordinary backup field preserves manual rules without app preferences`() {
        val rules = LocalAuthorRules().save(listOf(first, second), "合并名称", "backup")
        val backup = Backup(backupManga = emptyList(), localAuthorRules = rules.encode())
        val bytes = ProtoBuf.encodeToByteArray(Backup.serializer(), backup)
        val restored = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
        restored.backupPreferences shouldBe emptyList()
        LocalAuthorRules.decode(restored.localAuthorRules!!) shouldBe rules
        val oldBackup = Backup(emptyList())
        val oldBytes = ProtoBuf.encodeToByteArray(Backup.serializer(), oldBackup)
        ProtoBuf.decodeFromByteArray(Backup.serializer(), oldBytes).localAuthorRules shouldBe null
    }

    @Test
    fun `store persists rename and invalid restore leaves previous rules intact`() {
        var stored = ""
        val preference = mockk<Preference<String>>()
        val preferences = mockk<PreferenceStore>()
        every { preferences.getString(any(), any()) } returns preference
        every { preference.get() } answers { stored }
        every { preference.set(any()) } answers { stored = firstArg() }
        val store = LocalAuthorRulesStore(preferences)
        store.save(listOf(first, second), "名称")
        val original = store.backup()
        LocalAuthorRules.decode(original).rules.single().name shouldBe "名称"
        assertThrows(IllegalArgumentException::class.java) { store.restore("{invalid}") }
        store.backup() shouldBe original
        val renamed = store.get().applyTo(auto).single { it.key.startsWith("manual:") }
        store.save(listOf(renamed), "新名")
        store.get().rules.single().id shouldBe LocalAuthorRules.decode(original).rules.single().id
        store.get().rules.single().name shouldBe "新名"
    }

    @Test
    fun `mixed batch category checkboxes preserve untouched state`() {
        val memberships = listOf(setOf(1L, 2L), setOf(2L))
        localCategoryChecked(memberships, emptyMap(), 1) shouldBe null
        localCategoryChecked(memberships, emptyMap(), 2) shouldBe true
        localCategoryChecked(memberships, emptyMap(), 3) shouldBe false
        localCategoryChecked(memberships, mapOf(1L to false), 1) shouldBe false
        localCategoryChecked(memberships, mapOf(1L to true), 1) shouldBe true
        localCategoryChecked(emptyList(), emptyMap(), 1) shouldBe false
    }
}
