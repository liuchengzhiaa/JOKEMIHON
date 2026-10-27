package mihon.feature.localworks

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.category.interactor.CreateCategoryWithName
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.source.local.work.LocalWorkCatalog
import tachiyomi.source.local.work.LocalWorkDefinition
import kotlin.coroutines.coroutineContext

@Inject
@SingleIn(AppScope::class)
class LocalAuthorSync(
    private val catalog: LocalWorkCatalog,
    private val authorRules: LocalAuthorRulesStore,
    private val library: LocalWorkLibrary,
    private val getCategories: GetCategories,
    private val createCategory: CreateCategoryWithName,
) {
    private val mutation = Mutex()

    /* Re-scan every root group, ignoring UI search/selection. Reports bounded failures and completed groups;
       cancellation leaves completed entries intact so the owner can safely retry the additive operation. */
    suspend fun sync(onProgress: (LocalAuthorSyncProgress) -> Unit): LocalAuthorSyncResult =
        withContext(Dispatchers.IO) {
            mutation.withLock {
                val authors = authorRules.get().applyTo(catalog.authors())
                var categoryId: Long? = null
                var synced = 0
                var empty = 0
                var failed = 0
                val failedNames = mutableListOf<String>()
                onProgress(LocalAuthorSyncProgress(0, authors.size))
                for ((index, author) in authors.withIndex()) {
                    coroutineContext.ensureActive()
                    onProgress(LocalAuthorSyncProgress(index, authors.size, author.name))
                    try {
                        val works = catalog.works(author)
                        if (works.isEmpty()) {
                            empty++
                        } else {
                            LocalWorkDefinition(works, authorKey = author.key) // Validate before creating a tag.
                            val id = categoryId ?: destination().also { categoryId = it }
                            library.syncAuthor(author, works, id)
                            synced++
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        coroutineContext.ensureActive()
                        logcat(LogPriority.ERROR, e)
                        failed++
                        if (failedNames.size < 5) failedNames.add(author.name)
                    }
                    onProgress(LocalAuthorSyncProgress(index + 1, authors.size))
                }
                LocalAuthorSyncResult(authors.size, synced, empty, failed, failedNames.toList())
            }
        }

    private suspend fun destination(): Long {
        getCategories.await().filter { it.id > 0 && it.name == CATEGORY_NAME }.minByOrNull { it.id }
            ?.let { return it.id }
        val result = createCategory.await(CATEGORY_NAME)
        if (result is CreateCategoryWithName.Result.InternalError) throw result.error
        coroutineContext.ensureActive()
        return getCategories.await().filter { it.id > 0 && it.name == CATEGORY_NAME }.minBy { it.id }.id
    }

    companion object {
        // Fixed destination name is a data identity, not a translated UI title.
        const val CATEGORY_NAME = "大家之室"
    }
}

data class LocalAuthorSyncProgress(val completed: Int, val total: Int, val name: String = "")

data class LocalAuthorSyncResult(
    val total: Int,
    val synced: Int,
    val empty: Int,
    val failed: Int,
    val failedNames: List<String>,
)
