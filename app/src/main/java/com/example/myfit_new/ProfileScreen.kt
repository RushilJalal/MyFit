import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import com.example.myfit_new.R
import com.example.myfit_new.StepTracker
import com.example.myfit_new.database.StepDatabaseHelper
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.saveable.rememberSaveable

// Function to save profile data to SharedPreferences
fun saveProfileData(context: Context, name: String, username: String) {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
    with(sharedPreferences.edit()) {
        putString("profile_name", name)
        putString("profile_username", username)
        apply()
    }
}

// Function to load profile data from SharedPreferences
fun loadProfileData(context: Context): Pair<String, String> {
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("profile_data", Context.MODE_PRIVATE)
    val name = sharedPreferences.getString("profile_name", "Rushil Jalal") ?: "Rushil Jalal"
    val username = sharedPreferences.getString("profile_username", "@rushiljalal") ?: "@rushiljalal"
    return Pair(name, username)
}


@Composable
fun ProfileScreen() {
    val context = LocalContext.current
    val stepTracker = remember { StepTracker(context) }
    val stepCount by stepTracker.stepCount.collectAsState()

    // Load step history data
    val dbHelper = remember { StepDatabaseHelper(context) }
    val stepHistory = remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    val weeklyAverage = remember { mutableStateOf(0) }

    // State for popup menu
    var showMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf("") }
    var newValue by remember { mutableStateOf(TextFieldValue("")) }

    // Load profile data from SharedPreferences
    val (initialName, initialUsername) = loadProfileData(context)
    var profileName by rememberSaveable { mutableStateOf(initialName) }
    var profileUsername by rememberSaveable { mutableStateOf(initialUsername) }



    LaunchedEffect(Unit) {
        Log.d("ProfileScreen", "Loading step history...")
        val history = dbHelper.getStepHistory(7)
        Log.d("ProfileScreen", "Step history: $history")
        stepHistory.value = history
        weeklyAverage.value = if (history.isNotEmpty()) {
            history.map { it.second }.average().toInt()
        } else 0
    }

    NestedScrollView(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Profile Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2EB932), Color(0xFFA73DDE)),
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
                    .padding(20.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onLongPress = { showMenu = true }
                        )
                    }
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Profile Image
                    Image(
                        painter = painterResource(id = R.drawable.default_profile),
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = profileName,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = profileUsername,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        content = { Text("Change Name") },
                        onClick = {
                            showMenu = false
                            dialogTitle = "Change Name"
                            newValue = TextFieldValue(profileName)
                            showDialog = true
                        }
                    )
                    DropdownMenuItem(
                        content = { Text("Change Username") },
                        onClick = {
                            showMenu = false
                            dialogTitle = "Change Username"
                            newValue = TextFieldValue(profileUsername)
                            showDialog = true
                        }
                    )
                }
            }

            // Stats Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Today's Steps Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Today's Steps",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stepCount.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF212121)
                        )
                    }
                }

                // Weekly Average Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Weekly Average",
                            fontSize = 14.sp,
                            color = Color(0xFF757575)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = weeklyAverage.value.toString(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF212121)
                        )
                    }
                }
            }

            // Step History Graph
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Step History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF212121),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // For the chart, we need to use AndroidView to embed MPAndroidChart
                    AndroidView(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        factory = { context ->
                            BarChart(context).apply {
                                description.isEnabled = false
                                setDrawGridBackground(false)
                                setDrawBarShadow(false)
                                setDrawValueAboveBar(true)
                                setPinchZoom(false)
                                setScaleEnabled(false)

                                // X-axis setup
                                xAxis.position = XAxis.XAxisPosition.BOTTOM
                                xAxis.setDrawGridLines(false)
                                xAxis.granularity = 1f

                                // Y-axis setup
                                axisLeft.setDrawGridLines(true)
                                axisLeft.spaceTop = 35f
                                axisLeft.axisMinimum = 0f

                                axisRight.isEnabled = false
                                legend.isEnabled = false
                            }
                        },
                        update = { chart ->
                            val last7DaysHistory = stepHistory.value.take(7)
                            val entries = last7DaysHistory.mapIndexed { index, (_, steps) ->
                                BarEntry(index.toFloat(), steps.toFloat())
                            }

                            if (entries.isNotEmpty()) {
                                val dataSet = BarDataSet(entries, "Steps").apply {
                                    color = "#C0A0C0".toColorInt()
                                    valueTextColor = android.graphics.Color.BLACK
                                    valueTextSize = 10f
                                    barShadowColor = android.graphics.Color.TRANSPARENT // Make shadow transparent
                                }

                                val barData = BarData(dataSet).apply {
                                    barWidth = 0.3f
                                }
                                chart.data = barData

                                // Set X-axis labels to show dates
                                val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val shortFormatter = SimpleDateFormat("EEE", Locale.getDefault())

                                chart.xAxis.valueFormatter = IndexAxisValueFormatter(
                                    stepHistory.value.map { (dateStr, _) ->
                                        val date = formatter.parse(dateStr)
                                        date?.let { shortFormatter.format(it) } ?: ""
                                    }
                                )

                                chart.invalidate() // refresh chart
                            }
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
            properties = DialogProperties(dismissOnClickOutside = false)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colors.background,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = dialogTitle, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colors.onBackground)
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = newValue,
                        onValueChange = { newValue = it },
                        label = { Text(dialogTitle) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { showDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            if(dialogTitle == "Change Name") {
                                profileName = newValue.text
                            } else if (dialogTitle == "Change Username") {
                                profileUsername = newValue.text
                            }
                            saveProfileData(context, profileName, profileUsername)
                            showDialog = false
                        }) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NestedScrollView(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        content()
    }
}