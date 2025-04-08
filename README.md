# Flight Tracker Application

## Overview
This Android application allows users to track flights in real-time, view flight status, collect historical flight data, and analyze flight statistics across different routes. Built with modern Android development practices, the app provides an intuitive interface for monitoring your favorite flights and understanding travel patterns.

## Features
- **Real-time Flight Tracking**: Search and monitor flights by flight number
- **Flight Details Display**: View comprehensive information including:
  - Departure/arrival airports and cities
  - Scheduled and actual departure/arrival times
  - Flight status (scheduled, delayed, landed)
  - Delay information
- **Historical Data Collection**: Automatically collect flight data in the background
- **Flight Statistics**: Analyze flight performance across routes with:
  - Average flight times between airports
  - Delay statistics
  - Historical flight records

## Technology Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM (Model-View-ViewModel)
- **Libraries**:
  - Retrofit & OkHttp: Network requests
  - Moshi: JSON parsing
  - Room: Local database
  - Coroutines & Flow: Asynchronous operations
  - WorkManager: Background tasks scheduling
  - Lifecycle components: ViewModel integration

## Setup Instructions

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- Minimum SDK: 24 (Android 7.0)
- Target SDK: 35
- Aviation Stack API key (for flight data)

### Getting Started
1. Clone the repository:
   ```
   git clone <repository-url>
   ```

2. Create API key file:
   Create a new Kotlin file at `app/src/main/java/com/example/mc_a2/data/api/ApiKeys.kt` with:
   ```kotlin
   package com.example.mc_a2.data.api

   object ApiKeys {
       const val AVIATION_STACK_API_KEY = "your_api_key_here"
   }
   ```

3. Build and run the application using Android Studio

## Usage Guide

### Tracking a Flight
1. From the main screen, enter a flight number in the search field
2. Tap the search button to fetch current flight information
3. View real-time flight details including status, departure/arrival information
4. Tap "Track This Flight" to add the flight to your tracked flights

### Viewing Flight Statistics
1. Navigate to the Statistics screen using the navigation menu
2. View average flight times across different routes
3. See the number of flight records collected for each route
4. Enable/disable background data collection with the toggle switch

### Background Data Collection
- Toggle background data collection on/off from the Statistics screen
- When enabled, the app will periodically fetch updated information for tracked flights
- Data is stored locally for offline access and statistical analysis

## Architecture

The application follows the MVVM (Model-View-ViewModel) architecture pattern with these key components:

- **Data Layer**:
  - `FlightRepository`: Central data source that coordinates API and database access
  - `FlightApiService`: Handles network requests to the Aviation Stack API
  - `FlightDatabase` & `FlightRecordDao`: Local data persistence

- **ViewModel Layer**:
  - `FlightTrackingViewModel`: Manages flight search and tracking
  - `FlightStatisticsViewModel`: Handles statistics and data collection settings

- **UI Layer**:
  - Jetpack Compose screens for a modern, declarative UI
  - State hoisting pattern for UI state management

## Database Structure

The app uses Room database with the following main entity:

- `FlightRecord`: Stores historical flight data including:
  - Flight identification (number, airline)
  - Route information (departure/arrival airports and cities)
  - Scheduled and actual times
  - Delay information
  - Record metadata (when collected, flight date)

## API Integration

The app integrates with the [Aviation Stack API](https://aviationstack.com/) to fetch real-time flight data. The implementation includes:

- RESTful API calls using Retrofit
- JSON parsing with Moshi adapters
- Error handling and offline support
- Rate limiting consideration