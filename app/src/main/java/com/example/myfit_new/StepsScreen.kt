package com.example.myfit_new

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepsScreen() {
    val context = LocalContext.current
    val stepTracker = remember { StepTracker(context) }
    val stepCount by stepTracker.stepCount.collectAsState()

    // State for target steps
    var targetSteps by rememberSaveable { mutableStateOf(10000) }
    var showMenu by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var newTargetSteps by remember { mutableStateOf(TextFieldValue(targetSteps.toString())) }

    // Save step count across configuration changes
    var savedStepCount by rememberSaveable { mutableStateOf(0) }

    // Update saved count when step count changes
    LaunchedEffect(stepCount) {
        savedStepCount = stepCount
    }

    // Calculate progress
    val progress = (stepCount.toFloat() / targetSteps).coerceIn(0f, 1f)

    // Animate the progress
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    // Effect to register/unregister sensor listener
    DisposableEffect(key1 = stepTracker) {
        stepTracker.startTracking()
        onDispose {
            stepTracker.stopTracking()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MyFit") },
                actions = {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            painter = painterResource(id = android.R.drawable.ic_menu_more),
                            contentDescription = "Menu"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Set Step Goal") },
                            onClick = {
                                showMenu = false
                                showDialog = true
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8F8F8)),
            contentAlignment = Alignment.Center

        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Today's Steps",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(contentAlignment = Alignment.Center) {
                    // Custom circular progress indicator
                    StepProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.size(250.dp)
                    )

                    // Step count in the center
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$stepCount",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "of $targetSteps steps",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard(
                        title = "Distance",
                        value = "${calculateDistance(stepCount)} km",
                        icon = Icons.Filled.DirectionsRun,
                        modifier = Modifier.width(140.dp)
                    )
                    StatCard(
                        title = "Calories",
                        value = "${calculateCalories(stepCount)} kcal",
                        icon = Icons.Filled.LocalFireDepartment,
                        modifier = Modifier.width(140.dp)
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
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Set Step Goal",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value = newTargetSteps,
                        onValueChange = { newTargetSteps = it },
                        label = { Text("Step Goal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
                            targetSteps = newTargetSteps.text.toIntOrNull() ?: targetSteps
                            showDialog = false
                        }) {
                            Text("Set")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE1BEE7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.8f)
            )
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.6f)
            )
        }
    }
}

// Helper functions for calculations
private fun calculateDistance(steps: Int): Double {
    // Average stride length is about 0.762 meters
    val distanceInMeters = steps * 0.762
    return (distanceInMeters / 1000).toDouble().round(2)
}

private fun calculateCalories(steps: Int): Int {
    // Rough estimate: 0.04 calories per step
    return (steps * 0.04).toInt()
}

private fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}