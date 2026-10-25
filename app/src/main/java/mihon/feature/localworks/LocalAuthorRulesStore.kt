package mihon.feature.localworks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.source.local.work.LocalAuthorGroup
import tachiyomi.source.local.work.LocalAuthorRules
import java.util.UUID

@Inject
@SingleIn(AppScope::class)
class LocalAuthorRulesStore(preferences: PreferenceStore) {
    // Backed up explicitly with library entries, not dependent on the App settings checkbox.
    private val preference = preferences.getString(Preference.appStateKey("jokemihon_author_rules_v1"))
    val changes get() = preference.changes()

    @Synchronized
    fun get(): LocalAuthorRules = LocalAuthorRules.decode(preference.get())

    @Synchronized
    fun save(selected: List<LocalAuthorGroup>, name: String) {
        val id = selected.singleOrNull()?.key?.takeIf { it.startsWith("manual:") }?.removePrefix("manual:")
            ?: UUID.randomUUID().toString()
        preference.set(get().save(selected, name, id).encode())
    }

    @Synchronized
    fun dissolve(keys: Set<String>) = preference.set(get().dissolve(keys).encode())

    @Synchronized
    fun backup(): String = get().encode()

    @Synchronized
    fun restore(value: String) {
        // Validate the complete record before replacing anything; old backups without the field do not call this.
        preference.set(LocalAuthorRules.decode(value).encode())
    }
}
