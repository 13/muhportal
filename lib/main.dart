import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'dart:convert';
import 'theme.dart';
import 'models.dart';
import 'mqtt_client.dart';
import 'portal_screen.dart';
import 'wol_screen.dart';
import 'ha_screen.dart';
import 'settings_screen.dart';

void main() {
  runApp(const MuhPortalApp());
}

class MuhPortalApp extends StatefulWidget {
  const MuhPortalApp({super.key});

  @override
  State<MuhPortalApp> createState() => _MuhPortalAppState();
}

class _MuhPortalAppState extends State<MuhPortalApp> {
  bool _isDarkMode = false;
  bool _isBlackWhiteMode = false;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      _isDarkMode = prefs.getBool('dark_mode') ?? false;
      _isBlackWhiteMode = prefs.getBool('blackwhite_mode') ?? false;
      _isLoading = false;
    });
  }

  Future<void> _updateDarkMode(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('dark_mode', value);
    setState(() => _isDarkMode = value);
  }

  Future<void> _updateBlackWhiteMode(bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool('blackwhite_mode', value);
    setState(() => _isBlackWhiteMode = value);
  }

  @override
  Widget build(BuildContext context) {
    if (_isLoading) {
      return const MaterialApp(
        home: Scaffold(
          body: Center(child: CircularProgressIndicator()),
        ),
      );
    }

    return MaterialApp(
      title: 'MuhPortal',
      theme: _isDarkMode ? buildDarkTheme() : buildLightTheme(),
      home: MainScreen(
        isDarkMode: _isDarkMode,
        onDarkModeChange: _updateDarkMode,
        isBlackWhiteMode: _isBlackWhiteMode,
        onBlackWhiteModeChange: _updateBlackWhiteMode,
      ),
    );
  }
}

class MainScreen extends StatefulWidget {
  final bool isDarkMode;
  final Function(bool) onDarkModeChange;
  final bool isBlackWhiteMode;
  final Function(bool) onBlackWhiteModeChange;

  const MainScreen({
    super.key,
    required this.isDarkMode,
    required this.onDarkModeChange,
    required this.isBlackWhiteMode,
    required this.onBlackWhiteModeChange,
  });

  @override
  State<MainScreen> createState() => _MainScreenState();
}

class _MainScreenState extends State<MainScreen> {
  int _currentPage = 0;
  bool _showSettings = false;
  ConnState _connState = ConnState.disconnected;

  final Map<String, PortalUpdate> _portalStates = {};
  final Map<String, WolUpdate> _wolStates = {};
  final Map<String, SensorUpdate> _sensorStates = {};
  final Map<String, SwitchUpdate> _switchStates = {};
  final Map<String, PvUpdate> _pvStates = {};

  PortalMqttClient? _mqtt;
  final GlobalKey<ScaffoldMessengerState> _scaffoldKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    _loadCache();
    _initMqtt();
  }

  @override
  void dispose() {
    _mqtt?.disconnect();
    super.dispose();
  }

  Future<void> _loadCache() async {
    try {
      final prefs = await SharedPreferences.getInstance();

      // Load all cache data first, then update state once
      final Map<String, PortalUpdate> portalTemp = {};
      final Map<String, WolUpdate> wolTemp = {};
      final Map<String, SensorUpdate> sensorTemp = {};
      final Map<String, SwitchUpdate> switchTemp = {};
      final Map<String, PvUpdate> pvTemp = {};

      try {
        final portalJson = prefs.getString('portal');
        if (portalJson != null) {
          final data = jsonDecode(portalJson) as Map<String, dynamic>;
          for (final entry in data.entries) {
            final obj = entry.value as Map<String, dynamic>;
            portalTemp[entry.key] = PortalUpdate(
              id: entry.key,
              state: DoorState.values.firstWhere((e) => e.name == obj['state']),
              timestamp: obj['timestamp'] as int,
            );
          }
        }
      } catch (e, st) {
        debugPrint('Failed to load portal cache: $e');
        debugPrint('$st');
      }

      try {
        final wolJson = prefs.getString('wol');
        if (wolJson != null) {
          final data = jsonDecode(wolJson) as Map<String, dynamic>;
          for (final entry in data.entries) {
            final obj = entry.value as Map<String, dynamic>;
            wolTemp[entry.key] = WolUpdate(
              id: entry.key,
              name: obj['name'] as String,
              ip: obj['ip'] as String,
              mac: obj['mac'] as String,
              alive: obj['alive'] as bool,
              priority: obj['priority'] as int? ?? 99,
              timestamp: obj['timestamp'] as int,
            );
          }
        }
      } catch (e, st) {
        debugPrint('Failed to load WOL cache: $e');
        debugPrint('$st');
      }

      try {
        final sensorsJson = prefs.getString('sensors');
        if (sensorsJson != null) {
          final data = jsonDecode(sensorsJson) as Map<String, dynamic>;
          for (final entry in data.entries) {
            final obj = entry.value as Map<String, dynamic>;
            sensorTemp[entry.key] = SensorUpdate(
              id: entry.key,
              temp: (obj['temp'] as num).toDouble(),
              humidity: (obj['humidity'] as num).toDouble(),
              timestamp: obj['timestamp'] as int,
            );
          }
        }
      } catch (e, st) {
        debugPrint('Failed to load sensor cache: $e');
        debugPrint('$st');
      }

      try {
        final switchesJson = prefs.getString('switches');
        if (switchesJson != null) {
          final data = jsonDecode(switchesJson) as Map<String, dynamic>;
          for (final entry in data.entries) {
            final obj = entry.value as Map<String, dynamic>;
            switchTemp[entry.key] = SwitchUpdate(
              id: entry.key,
              state: obj['state'] as bool,
              timestamp: obj['timestamp'] as int,
            );
          }
        }
      } catch (e, st) {
        debugPrint('Failed to load switch cache: $e');
        debugPrint('$st');
      }

      try {
        final pvJson = prefs.getString('pv');
        if (pvJson != null) {
          final data = jsonDecode(pvJson) as Map<String, dynamic>;
          for (final entry in data.entries) {
            final obj = entry.value as Map<String, dynamic>;
            pvTemp[entry.key] = PvUpdate(
              id: entry.key,
              p1: (obj['p1'] as num).toDouble(),
              p2: (obj['p2'] as num).toDouble(),
              e1: (obj['e1'] as num).toDouble(),
              e2: (obj['e2'] as num).toDouble(),
              timestamp: obj['timestamp'] as int,
            );
          }
        }
      } catch (e, st) {
        debugPrint('Failed to load PV cache: $e');
        debugPrint('$st');
      }

      // Update state once with all loaded data
      setState(() {
        _portalStates.addAll(portalTemp);
        _wolStates.addAll(wolTemp);
        _sensorStates.addAll(sensorTemp);
        _switchStates.addAll(switchTemp);
        _pvStates.addAll(pvTemp);
      });
    } catch (e, st) {
      debugPrint('Failed to load cache: $e');
      debugPrint('$st');
    }
  }

  Future<void> _savePortal() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final data = <String, dynamic>{};
      for (final entry in _portalStates.entries) {
        data[entry.key] = entry.value.toJson();
      }
      await prefs.setString('portal', jsonEncode(data));
    } catch (e, st) {
      debugPrint('Failed to save portal state to SharedPreferences: $e');
      debugPrint('$st');
    }
  }

  Future<void> _saveWol() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final data = <String, dynamic>{};
      for (final entry in _wolStates.entries) {
        data[entry.key] = entry.value.toJson();
      }
      await prefs.setString('wol', jsonEncode(data));
    } catch (e, st) {
      debugPrint('Failed to save WOL state to SharedPreferences: $e');
      debugPrint('$st');
    }
  }

  Future<void> _saveSensors() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final data = <String, dynamic>{};
      for (final entry in _sensorStates.entries) {
        data[entry.key] = entry.value.toJson();
      }
      await prefs.setString('sensors', jsonEncode(data));
    } catch (e, st) {
      debugPrint('Failed to save sensor state to SharedPreferences: $e');
      debugPrint('$st');
    }
  }

  Future<void> _saveSwitches() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final data = <String, dynamic>{};
      for (final entry in _switchStates.entries) {
        data[entry.key] = entry.value.toJson();
      }
      await prefs.setString('switches', jsonEncode(data));
    } catch (e, st) {
      debugPrint('Failed to save switch state to SharedPreferences: $e');
      debugPrint('$st');
    }
  }

  Future<void> _savePv() async {
    try {
      final prefs = await SharedPreferences.getInstance();
      final data = <String, dynamic>{};
      for (final entry in _pvStates.entries) {
        data[entry.key] = entry.value.toJson();
      }
      await prefs.setString('pv', jsonEncode(data));
    } catch (e, st) {
      debugPrint('Failed to save PV state to SharedPreferences: $e');
      debugPrint('$st');
    }
  }

  void _initMqtt() {
    _mqtt = PortalMqttClient(
      onConnState: (state) {
        setState(() => _connState = state);
      },
      onPortalUpdate: (update) {
        setState(() => _portalStates[update.id] = update);
        _savePortal();
      },
      onWolUpdate: (update) {
        setState(() => _wolStates[update.id] = update);
        _saveWol();
      },
      onSensorUpdate: (update) {
        setState(() => _sensorStates[update.id] = update);
        _saveSensors();
      },
      onSwitchUpdate: (update) {
        setState(() => _switchStates[update.id] = update);
        _saveSwitches();
      },
      onPvUpdate: (update) {
        setState(() => _pvStates[update.id] = update);
        _savePv();
      },
    );

    _mqtt!.connect();
  }

  void _showSnackbar(String message) {
    _scaffoldKey.currentState?.showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: getAppColor(AppColor.green, widget.isBlackWhiteMode),
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ScaffoldMessenger(
      key: _scaffoldKey,
      child: Scaffold(
        body: AnimatedSwitcher(
          duration: const Duration(milliseconds: 300),
          child: _showSettings
              ? SettingsScreen(
                  isDarkMode: widget.isDarkMode,
                  onDarkModeChange: widget.onDarkModeChange,
                  isBlackWhiteMode: widget.isBlackWhiteMode,
                  onBlackWhiteModeChange: widget.onBlackWhiteModeChange,
                  onBack: () => setState(() => _showSettings = false),
                )
              : _buildMainContent(),
        ),
        bottomNavigationBar: _showSettings
            ? null
            : NavigationBar(
                selectedIndex: _currentPage,
                onDestinationSelected: (index) {
                  setState(() => _currentPage = index);
                },
                destinations: const [
                  NavigationDestination(
                    icon: Icon(Icons.lock),
                    label: 'Portal',
                  ),
                  NavigationDestination(
                    icon: Icon(Icons.lan),
                    label: 'WOL',
                  ),
                  NavigationDestination(
                    icon: Icon(Icons.lightbulb),
                    label: 'HA',
                  ),
                ],
              ),
      ),
    );
  }

  Widget _buildMainContent() {
    switch (_currentPage) {
      case 0:
        return PortalScreen(
          connState: _connState,
          portalStates: _portalStates,
          onRefresh: () => _mqtt?.reconnect(),
          onToggle: (cmd) => _mqtt?.toggle(cmd),
          isBlackWhiteMode: widget.isBlackWhiteMode,
          onOpenSettings: () => setState(() => _showSettings = true),
          onShowSnackbar: _showSnackbar,
        );
      case 1:
        return WolScreen(
          connState: _connState,
          wolStates: _wolStates,
          onRefresh: () => _mqtt?.reconnect(),
          onWolAction: (mac, action) => _mqtt?.wolAction(mac, action),
          isBlackWhiteMode: widget.isBlackWhiteMode,
          onOpenSettings: () => setState(() => _showSettings = true),
          onShowSnackbar: _showSnackbar,
        );
      case 2:
        return HAScreen(
          connState: _connState,
          sensorStates: _sensorStates,
          switchStates: _switchStates,
          pvStates: _pvStates,
          onSwitchAction: (id, state) => _mqtt?.setPower(id, state),
          onRefresh: () => _mqtt?.reconnect(),
          isBlackWhiteMode: widget.isBlackWhiteMode,
          onOpenSettings: () => setState(() => _showSettings = true),
        );
      default:
        return const SizedBox.shrink();
    }
  }
}
