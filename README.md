# <img src="assets/muhportalicon.png" width="30" height="30" /> MuhPortal

An Android application built with Kotlin to monitor and control various portals (doors and locks) via MQTT over WebSockets.

## Features

- **Real-time Monitoring**: Subscribes to JSON status updates for multiple portals.
- **Command Control**: Sends toggle and state commands to garage and house doors.
- **Connection Management**: 
    - Automatic reconnection logic.
    - Visual connection status bar (Green for connected, Red for disconnected).
 - **Payload Parsing**: Extracts door states and original timestamps from MQTT messages.

## Screenshots

<p align="center">
  <img src="assets/screenshot1.png" width="200" />
  <img src="assets/screenshot2.png" width="200" />
  <img src="assets/screenshot3.png" width="200" />
</p>

## Technical Specifications

- **Language**: Kotlin / Java
- **Build System**: Gradle
- **Library**: Eclipse Paho MQTT Client
- **Protocol**: MQTT over WebSockets (`ws://192.168.22.5:1884`)
- **IDE**: Android Studio Otter 3 Feature Drop

## MQTT Architecture

### Subscriptions
The app monitors topics with the pattern: `muh/portal/{KEY}/json`
- `G`: Garage
- `GD`: Garage Door
- `GDL`: Garage Door Lock
- `HD`: House Door
- `HDL`: House Door Lock

### Commands
Commands are published to: `muh/portal/RLY/cmnd`
The following commands are supported:
- `G_T`: Garage Toggle
- `GD_O` / `GD_U` / `GD_L`: Garage Door Open/Unlock/Lock
- `HD_O` / `HD_U` / `HD_L`: House Door Open/Unlock/Lock

## Data Structure

Portal updates are received as JSON and mapped to the `PortalUpdate` data class:
- **ID**: The portal key (e.g., "G", "HD").
- **State**: `OPEN`, `CLOSED`, or `UNKNOWN`.
- **Timestamp**: Extracted from the MQTT payload using various date formats or fallback to system time.

## Installation

1. Open the project in Android Studio.
2. Ensure the MQTT broker is accessible at the configured URI.
3. Build and run the application on a Linux-based environment or Android device.
