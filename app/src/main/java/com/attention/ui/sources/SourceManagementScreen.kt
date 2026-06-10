package com.attention.ui.sources

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.attention.data.entity.SourceEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceManagementScreen(
    viewModel: SourceManagementViewModel,
    onBack: () -> Unit = {}
) {
    val sources by viewModel.sources.collectAsState()
    val validationState by viewModel.validationState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Automatically close dialog on success
    LaunchedEffect(validationState) {
        if (validationState is ValidationState.Success) {
            showAddDialog = false
            viewModel.resetValidationState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Sources") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                viewModel.resetValidationState()
                showAddDialog = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Source")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sources, key = { it.id }) { source ->
                SourceItem(
                    source = source,
                    onToggle = { enabled -> viewModel.toggleSource(source.id, enabled) },
                    onDelete = { viewModel.deleteSource(source.id) }
                )
            }
        }

        if (showAddDialog) {
            AddSourceDialog(
                state = validationState,
                onDismiss = { 
                    showAddDialog = false
                    viewModel.resetValidationState()
                },
                onConfirm = { url ->
                    viewModel.addCustomSource(url)
                }
            )
        }
    }
}

@Composable
fun SourceItem(
    source: SourceEntity,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source.feedUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Switch(
                checked = source.isEnabled,
                onCheckedChange = onToggle
            )
            
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun AddSourceDialog(
    state: ValidationState,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    val isLoading = state is ValidationState.Loading

    AlertDialog(
        onDismissRequest = if (isLoading) ({}) else onDismiss,
        title = { Text("Add Custom Source") },
        text = {
            Column {
                Text("Enter the RSS or Atom feed URL:")
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = url,
                    onValueChange = { url = it },
                    placeholder = { Text("https://example.com/feed.xml") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    isError = state is ValidationState.Error
                )
                
                if (state is ValidationState.Error) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url) },
                enabled = url.isNotBlank() && !isLoading
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
