package com.example.visiting_cardbusiness_card.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.visiting_cardbusiness_card.MainViewModel
import com.example.visiting_cardbusiness_card.ScannedCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, onScanClick: () -> Unit, onCardClick: (ScannedCard) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Business Card Scanner") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onScanClick) {
                Icon(Icons.Default.Add, contentDescription = "Scan New Card")
            }
        }
    ) { padding ->
        if (viewModel.scannedCards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No cards scanned yet. Tap + to start.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.scannedCards) { card ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onCardClick(card) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            val bestResult = card.results.lastOrNull() // Pipeline 4
                            Text(text = bestResult?.company ?: "Unknown Company", style = MaterialTheme.typography.titleMedium)
                            Text(text = bestResult?.name ?: "Unknown Name", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Scanned on ${java.util.Date(card.timestamp)}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
