package com.example.myfit_new

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.core.content.edit

data class JournalEntry(
    val heading: String,
    val body: String,
    val timestamp: String,
    val imageUri: String? = null,
    val positiveCount: Int = 0,
    val negativeCount: Int = 0
)

fun saveEntriesToStorage(context: Context, entries: List<JournalEntry>) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("journal_prefs", Context.MODE_PRIVATE)
    sharedPreferences.edit() {
        val json = Gson().toJson(entries)
        putString("journal_entries", json)
    }
}

fun loadEntriesFromStorage(context: Context): MutableList<JournalEntry> {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("journal_prefs", Context.MODE_PRIVATE)
    val json = sharedPreferences.getString("journal_entries", null)
    return if (json != null) {
        val type = object : TypeToken<MutableList<JournalEntry>>() {}.type
        Gson().fromJson(json, type)
    } else {
        mutableListOf()
    }
}

fun analyzeEmotions(text: String): Pair<Int, Int> {
    val negativeEmotions = listOf(
        "sad", "angry", "anxious", "lonely", "weak", "tired", "depressed", "stressed", "overwhelmed", "worried",
        "guilty", "frustrated", "jealous", "hopeless", "bored", "irritated", "disappointed", "afraid",
        "nervous", "ashamed", "resentful", "unmotivated", "insecure", "lost", "unhappy", "panicked",
        "helpless", "confused", "hurt", "excluded", "worthless", "rejected", "disgusted", "angsty",
        "inadequate", "trapped", "annoyed", "misunderstood", "vulnerable", "restless", "regretful",
        "tense", "melancholy"
    )

    val positiveEmotions = listOf(
        "happy", "joy", "grateful", "excited", "content", "hopeful", "loved", "motivated", "cheerful",
        "inspired", "peaceful", "relaxed", "proud", "enthusiastic", "playful", "optimistic", "confident",
        "energized", "satisfied", "amused", "trusting", "secure", "caring", "curious", "friendly",
        "affectionate", "encouraged", "fulfilled", "generous", "free", "open", "balanced", "determined",
        "brave", "refreshed", "supportive", "warm", "reassured", "interested", "blessed", "appreciated",
        "strong"
    )

    val words = text.lowercase().split(Regex("\\W+"))
    val positiveCount = words.count { word ->
        positiveEmotions.any { it.equals(word, ignoreCase = true) }
    }
    val negativeCount = words.count { word ->
        negativeEmotions.any { it.equals(word, ignoreCase = true) }
    }

    return positiveCount to negativeCount
}

@Composable
fun JournalScreen() {
    val context = LocalContext.current
    val journalEntries = remember { mutableStateListOf<JournalEntry>().apply { addAll(loadEntriesFromStorage(context)) } }

    val sortedEntries by remember { derivedStateOf { journalEntries.sortedByDescending { it.timestamp } } }

    var selectedEntry by remember { mutableStateOf<JournalEntry?>(null) }
    var creatingNewEntry by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<JournalEntry?>(null) }

    if (entryToDelete != null) {
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            title = { Text("Delete Entry", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = { Text("Are you sure you want to delete this entry?") },
            confirmButton = {
                Button(onClick = {
                    journalEntries.remove(entryToDelete)
                    saveEntriesToStorage(context, journalEntries)
                    entryToDelete = null
                }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colorScheme.primary)) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                Button(onClick = { entryToDelete = null }, colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colorScheme.primary)) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (selectedEntry != null || creatingNewEntry) {
        EditJournalEntryScreen(
            entry = selectedEntry ?: JournalEntry("", "", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))),
            onSave = { updatedEntry ->
                if (updatedEntry.heading.isBlank() && updatedEntry.body.isBlank()) return@EditJournalEntryScreen

                val (positive, negative) = analyzeEmotions(updatedEntry.heading + " " + updatedEntry.body)
                val finalEntry = updatedEntry.copy(
                    timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                    positiveCount = positive,
                    negativeCount = negative
                )

                if (creatingNewEntry) {
                    journalEntries.add(finalEntry)
                } else {
                    val index = journalEntries.indexOf(selectedEntry)
                    if (index != -1) {
                        journalEntries[index] = finalEntry
                    }
                }
                saveEntriesToStorage(context, journalEntries)
                selectedEntry = null
                creatingNewEntry = false
            },
            onCancel = {
                selectedEntry = null
                creatingNewEntry = false
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)) {
                items(sortedEntries) { entry ->
                    JournalEntryCard(entry, onClick = { selectedEntry = entry }, onLongPress = { entryToDelete = entry })
                }
            }
            FloatingActionButton(
                onClick = { creatingNewEntry = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                backgroundColor = MaterialTheme.colorScheme.primary
            ) {
                Text(text = "+", fontSize = 24.sp, color = Color.White)
            }
        }
    }
}
fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): Uri {
    val filename = "journal_image_${System.currentTimeMillis()}.png"
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    return Uri.fromFile(file)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JournalEntryCard(entry: JournalEntry, onClick: () -> Unit, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongPress),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.heading, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = entry.timestamp, fontSize = 12.sp, color = Color.Gray)
            }

            val total = entry.positiveCount + entry.negativeCount
            if (total > 0) {
                val positivePercent = (entry.positiveCount * 100 / total)
                val negativePercent = (entry.negativeCount * 100 / total)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "😊 $positivePercent%",
                        color = Color(0xFFFF9800),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        text = "😞 $negativePercent%",
                        color = Color(0xFF2196F3),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                }
            }
        }
    }
}


@Composable
fun EditJournalEntryScreen(entry: JournalEntry, onSave: (JournalEntry) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var heading by remember { mutableStateOf(entry.heading) }
    var body by remember { mutableStateOf(entry.body) }
    var imageUri by remember { mutableStateOf(entry.imageUri) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToInternalStorage(context, bitmap)
            imageUri = uri.toString()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            // Decode picked image into a Bitmap
            val inputStream = context.contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Save locally and store the returned file URI
            val savedUri = saveBitmapToInternalStorage(context, bitmap)
            imageUri = savedUri.toString()
        }
    }

    var showImagePickerDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(text = entry.timestamp, fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))

        TextField(
            value = heading,
            onValueChange = { heading = it },
            label = { Text("Heading") },
            modifier = Modifier.fillMaxWidth(),
            textStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold)
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Body") },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            textStyle = TextStyle(fontSize = 16.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        IconButton(
            onClick = { showImagePickerDialog = true },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = "Add Photo",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        if (showImagePickerDialog) {
            AlertDialog(
                onDismissRequest = { showImagePickerDialog = false },
                title = { Text("Add Photo") },
                text = {
                    Column {
                        Text("Choose a source:")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                cameraLauncher.launch(null)
                                showImagePickerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Take Photo")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                galleryLauncher.launch("image/*")
                                showImagePickerDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Choose from Gallery")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showImagePickerDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel")
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {}

            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        imageUri?.let {
            Image(
                painter = rememberAsyncImagePainter(it),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Cancel", fontSize = 20.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = {
                    if (heading.isBlank() && body.isBlank()) return@Button
                    onSave(entry.copy(heading = heading, body = body, imageUri = imageUri))
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", fontSize = 20.sp, color = Color.White)
            }
        }
    }
}

