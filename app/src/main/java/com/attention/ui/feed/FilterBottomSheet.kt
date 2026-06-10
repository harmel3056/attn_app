package com.attention.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    currentFilter: FeedFilter,
    onFilterChange: (FeedFilter) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
        ) {
            Text(
                text = "Topic",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val topics = listOf("all", "models", "safety", "research", "products", "policy", "other")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                topics.forEach { topic ->
                    val isSelected = if (topic == "all") currentFilter.topicTag == null else currentFilter.topicTag == topic
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val newTag = if (topic == "all") null else topic
                            onFilterChange(currentFilter.copy(topicTag = newTag))
                        },
                        label = { Text(topic.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Sort by",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val sortOptions = listOf(
                    "Newest" to SortOrder.Newest,
                    "Oldest" to SortOrder.Oldest,
                    "Most Covered" to SortOrder.MostCovered
                )

                sortOptions.forEach { (label, order) ->
                    FilterChip(
                        selected = currentFilter.sortOrder == order,
                        onClick = { onFilterChange(currentFilter.copy(sortOrder = order)) },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}
