package com.example.mc_a2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.mc_a2.data.model.Flight
import com.example.mc_a2.data.model.LiveData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FlightTrackerScreen(
    uiState: FlightTrackingState,
    lastFetchTime: String?,
    onTrackFlight: (String) -> Unit,
    onStopTracking: () -> Unit
) {
    var flightNumber by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var isTrackingStopped by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Flight Tracker",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Input field for flight number
            OutlinedTextField(
                value = flightNumber,
                onValueChange = { flightNumber = it.uppercase() },
                label = { Text("Flight Number (e.g., AA123)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        onTrackFlight(flightNumber)
                        isTrackingStopped = false
                    }
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Track/Stop buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        focusManager.clearFocus()
                        onTrackFlight(flightNumber) 
                        isTrackingStopped = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Track Flight")
                }
                
                Button(
                    onClick = { 
                        onStopTracking() 
                        isTrackingStopped = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop Tracking")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show last fetch time or tracking stopped message
            if (isTrackingStopped) {
                Text(
                    text = "Tracking stopped.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (lastFetchTime != null) {
                Text(
                    text = "Last fetched: $lastFetchTime",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Content based on state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when (uiState) {
                    is FlightTrackingState.Initial -> {
                        Text(
                            text = "Enter a flight number to start tracking",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    
                    is FlightTrackingState.Loading -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(50.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading flight information...")
                        }
                    }
                    
                    is FlightTrackingState.Success -> {
                        FlightInfoContent(
                            flight = uiState.flight,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    
                    is FlightTrackingState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Error",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlightInfoContent(
    flight: Flight,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
    ) {
        // Flight header information
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    flight.airline?.name?.let { airlineName ->
                        Text(
                            text = airlineName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    flight.flightInfo?.iata?.let { flightIata ->
                        Text(
                            text = "Flight $flightIata",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                flight.flightStatus?.let { status ->
                    Text(
                        text = "Status: $status",
                        style = MaterialTheme.typography.bodyLarge,
                        color = when (status.lowercase()) {
                            "active" -> Color.Green
                            "scheduled" -> Color.Blue
                            "delayed" -> Color.Red
                            "landed" -> Color.Green
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Route information
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Route Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Departure information
                    Column {
                        Text(
                            text = "From",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = flight.departure?.iata ?: "N/A",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = flight.departure?.airport ?: "N/A",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Flight direction indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Divider(
                            modifier = Modifier
                                .width(100.dp)
                                .padding(vertical = 16.dp)
                        )
                    }
                    
                    // Arrival information
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "To",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = flight.arrival?.iata ?: "N/A",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = flight.arrival?.airport ?: "N/A",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Time information
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Schedule",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Departure time
                    Column {
                        Text(
                            text = "Departure",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatDateTime(flight.departure?.scheduled) ?: "N/A",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (flight.departure?.delay != null && flight.departure.delay > 0) {
                            Text(
                                text = "Delayed by ${flight.departure.delay} min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    // Arrival time
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Arrival",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = formatDateTime(flight.arrival?.scheduled) ?: "N/A",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (flight.arrival?.delay != null && flight.arrival.delay > 0) {
                            Text(
                                text = "Delayed by ${flight.arrival.delay} min",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Live data if available
        flight.live?.let { liveData ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Live Flight Data",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Updated time
                    liveData.updated?.let { updated ->
                        Text(
                            text = "Last Updated: ${formatDateTime(updated)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Flight parameters
                    liveData.altitude?.let { altitude ->
                        FlightParameter("Altitude", "$altitude ft")
                    }
                    
                    liveData.speedHorizontal?.let { speed ->
                        FlightParameter("Speed", "$speed km/h")
                    }
                    
                    liveData.speedVertical?.let { vspeed ->
                        FlightParameter("Vertical Speed", "$vspeed m/s")
                    }
                    
                    liveData.direction?.let { direction ->
                        FlightParameter("Direction", "$direction°")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Location information in text format
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Current Position: " +
                                    "${liveData.latitude?.toString()?.take(7) ?: "N/A"}, " +
                                    "${liveData.longitude?.toString()?.take(7) ?: "N/A"}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Visual indicator for ground status
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    liveData.isGround?.let { isGround ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(
                                        color = if (isGround) Color.Red else Color.Green,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = if (isGround) "On Ground" else "In Air",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        } ?: run {
            // Show message when live data is not available
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Live data not available",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "The flight may not be currently in the air or tracking data is unavailable.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun FlightParameter(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Helper function to format date-time
fun formatDateTime(isoDateTime: String?): String? {
    if (isoDateTime == null) return null
    
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(isoDateTime) ?: return isoDateTime
        val outputFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        outputFormat.format(date)
    } catch (e: Exception) {
        isoDateTime
    }
}

// Helper function to get current time
fun getCurrentTime(): String {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    return dateFormat.format(Date())
}