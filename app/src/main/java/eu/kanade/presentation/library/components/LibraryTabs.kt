package eu.kanade.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.category.visualName
import eu.kanade.tachiyomi.R
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import tachiyomi.domain.category.model.Category
import tachiyomi.presentation.core.components.material.TabText

@Composable
internal fun LibraryTabs(
    categories: List<Category>,
    pagerState: PagerState,
    getItemCountForCategory: (Category) -> Int?,
    onCategoryClick: (Long) -> Unit,
) {
    if (categories.isEmpty()) return
    val currentPageIndex = pagerState.currentPage.coerceIn(categories.indices)
    // JOKEMIHON: retain the original tabs and offer a saveable, bounded multi-row category wall.
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (expanded) {
                Text(
                    text = stringResource(R.string.jokemihon_all_categories, categories.size),
                    modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleSmall,
                )
            } else {
                PrimaryScrollableTabRow(
                    selectedTabIndex = currentPageIndex,
                    modifier = Modifier.weight(1f),
                    edgePadding = 0.dp,
                ) {
                    categories.forEachIndexed { index, category ->
                        Tab(
                            selected = currentPageIndex == index,
                            onClick = { onCategoryClick(category.id) },
                            text = {
                                TabText(
                                    text = category.visualName,
                                    badgeCount = getItemCountForCategory(category),
                                )
                            },
                            unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
            IconButton(onClick = { expanded = !expanded }) {
                Icon(
                    imageVector = if (expanded) {
                        MaterialSymbols.Rounded.KeyboardArrowUp
                    } else {
                        MaterialSymbols.Rounded.KeyboardArrowDown
                    },
                    contentDescription = stringResource(
                        if (expanded) R.string.jokemihon_collapse_categories else R.string.jokemihon_expand_categories,
                    ),
                )
            }
        }
        if (expanded) {
            CategoryWall(categories, currentPageIndex, getItemCountForCategory, onCategoryClick)
            HorizontalDivider()
        }
    }
}

@Composable
private fun CategoryWall(
    categories: List<Category>,
    selectedIndex: Int,
    getItemCountForCategory: (Category) -> Int?,
    onCategoryClick: (Long) -> Unit,
) {
    val maxHeight = minOf(280.dp, LocalConfiguration.current.screenHeightDp.dp * 0.35f)
    val labels = categories.map { it.visualName }
    val counts = categories.map(getItemCountForCategory)
    val measurer = rememberTextMeasurer(cacheSize = 128)
    val density = LocalDensity.current
    val labelStyle = MaterialTheme.typography.labelLarge
    val countStyle = MaterialTheme.typography.labelSmall
    val scrollState = rememberLazyListState()

    BoxWithConstraints(Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
        val availableWidth = with(density) { (maxWidth - 16.dp).roundToPx().coerceAtLeast(1) }
        val spacing = with(density) { 8.dp.roundToPx() }
        val widths = remember(labels, counts, availableWidth, density, labelStyle, countStyle, measurer) {
            labels.mapIndexed { index, label ->
                val textWidth = measurer.measure(
                    text = label,
                    style = labelStyle,
                    maxLines = 1,
                    softWrap = false,
                    constraints = Constraints(maxWidth = availableWidth),
                ).size.width
                val countWidth = counts[index]?.let {
                    measurer.measure(text = it.toString(), style = countStyle, maxLines = 1).size.width +
                        with(density) { 14.dp.roundToPx() }
                } ?: 0
                (textWidth + countWidth + with(density) { 24.dp.roundToPx() } + 2)
                    .coerceIn(with(density) { 48.dp.roundToPx() }.coerceAtMost(availableWidth), availableWidth)
            }
        }
        val rows = remember(widths, availableWidth, spacing) { categoryWallRows(widths, availableWidth, spacing) }
        LaunchedEffect(rows, selectedIndex) {
            val row = rows.indexOfFirst { selectedIndex in it }
            if (row >= 0 && scrollState.layoutInfo.visibleItemsInfo.none { it.index == row }) {
                scrollState.scrollToItem(row)
            }
        }
        LazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(count = rows.size, key = { categories[rows[it].first].id }) { rowIndex ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (index in rows[rowIndex]) {
                        val category = categories[index]
                        val selected = index == selectedIndex
                        Surface(
                            selected = selected,
                            onClick = { onCategoryClick(category.id) },
                            modifier = Modifier
                                .width(with(density) { widths[index].toDp() })
                                .heightIn(min = 48.dp)
                                .semantics { role = Role.Tab },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = labels[index],
                                    modifier = Modifier.weight(1f),
                                    style = labelStyle,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                counts[index]?.let { count ->
                                    Text(
                                        text = count.toString(),
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                                RoundedCornerShape(8.dp),
                                            )
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        style = countStyle,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
