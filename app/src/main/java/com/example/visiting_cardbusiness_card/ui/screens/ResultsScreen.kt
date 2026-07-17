package com.example.visiting_cardbusiness_card.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.visiting_cardbusiness_card.MainViewModel
import com.example.visiting_cardbusiness_card.model.CardResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val results = viewModel.currentResults.value
    val imageUri = viewModel.currentImageUri.value
    val isProcessing = viewModel.isProcessing.value
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Results", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { padding ->
        if (isProcessing) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(strokeWidth = 4.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing Image...", style = MaterialTheme.typography.bodyMedium)
                }
            }
        } else if (results.isNotEmpty()) {
            val result = results.last() // Use the most advanced result (usually Pipeline 3 or 4)

            var company by remember { mutableStateOf("") }
            var name by remember { mutableStateOf("") }
            var designation by remember { mutableStateOf("") }
            var address by remember { mutableStateOf(result.address) }
            var fax by remember { mutableStateOf(result.fax) }
            var gstin by remember { mutableStateOf(result.gstin) }
            var otherInfo by remember { mutableStateOf("") }
            
            var selectedPhone by remember { mutableStateOf(result.phone.firstOrNull() ?: "") }
            var selectedEmail by remember { mutableStateOf(result.email.firstOrNull() ?: "") }
            var selectedWebsite by remember { mutableStateOf(result.website.firstOrNull() ?: "") }

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Scanned Card",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Suggestions for Designation, Name & Company
                item {
                    SectionHeader("Smart Suggestions")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        if (result.name.isNotEmpty()) {
                            item {
                                SuggestionChip(
                                    onClick = { name = result.name },
                                    label = { Text("Name: ${result.name}") },
                                    icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        if (result.designation.isNotEmpty()) {
                            item {
                                SuggestionChip(
                                    onClick = { designation = result.designation },
                                    label = { Text("Designation: ${result.designation}") },
                                    icon = { Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                        if (result.company.isNotEmpty()) {
                            item {
                                SuggestionChip(
                                    onClick = { company = result.company },
                                    label = { Text("Company: ${result.company}") },
                                    icon = { Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }

                item {
                    SectionHeader("Identify")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ResultField("Name", name, Icons.Default.Person) { name = it }
                            ResultField("Designation", designation, Icons.Default.Work) { designation = it }
                            ResultField("Company", company, Icons.Default.Business) { company = it }
                        }
                    }
                }

                item {
                    SectionHeader("Contact Info")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            SingleChipField("Phone", selectedPhone, Icons.Default.Phone, { selectedPhone = it }, result.phone)
                            SingleChipField("Email", selectedEmail, Icons.Default.Email, { selectedEmail = it }, result.email)
                            SingleChipField("Website", selectedWebsite, Icons.Default.Public, { selectedWebsite = it }, result.website)
                        }
                    }
                }

                item {
                    SectionHeader("Additional Details")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            ResultField("Address", address, Icons.Default.LocationOn) { address = it }
                            ResultField("Fax", fax, Icons.Default.Print) { fax = it }
                            ResultField("GSTIN", gstin, Icons.Default.Receipt) { gstin = it }
                            ResultField("Other Info", otherInfo, Icons.Default.Info) { otherInfo = it }
                        }
                    }
                }

                item {
                    SectionHeader("Complete Text from Card")
                    OutlinedTextField(
                        value = result.extras,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        label = { Text("OCR Raw Text") }
                    )
                }

                item {
                    SectionHeader("Text Segments (Tap to Copy)")
                }

                items(result.rawOcrBlocks) { block ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { copyToClipboard(context, "Text Block", block) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = block, 
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SingleChipField(label: String, currentValue: String, icon: ImageVector, onValueChange: (String) -> Unit, suggestions: List<String>) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        OutlinedTextField(
            value = currentValue,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
            textStyle = MaterialTheme.typography.bodyMedium,
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        
        if (suggestions.size > 1) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                items(suggestions) { suggestion ->
                    if (suggestion != currentValue) {
                        AssistChip(
                            onClick = { 
                                val sep = if (currentValue.isEmpty()) "" else ", "
                                if (!currentValue.contains(suggestion)) {
                                    onValueChange(currentValue + sep + suggestion) 
                                }
                            },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ResultField(label: String, value: String, icon: ImageVector, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
        textStyle = MaterialTheme.typography.bodyMedium,
        singleLine = label != "Address",
        shape = RoundedCornerShape(8.dp)
    )
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
}
