import 'package:flutter/material.dart';

class SettingsScreen extends StatelessWidget {
  final bool isDarkMode;
  final Function(bool) onDarkModeChange;
  final bool isBlackWhiteMode;
  final Function(bool) onBlackWhiteModeChange;
  final VoidCallback onBack;

  const SettingsScreen({
    super.key,
    required this.isDarkMode,
    required this.onDarkModeChange,
    required this.isBlackWhiteMode,
    required this.onBlackWhiteModeChange,
    required this.onBack,
  });

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: onBack,
        ),
        title: const Text('Settings'),
      ),
      body: ListView(
        children: [
          SwitchListTile(
            title: const Text('Dark Mode'),
            value: isDarkMode,
            onChanged: onDarkModeChange,
          ),
          SwitchListTile(
            title: const Text('Black & White Mode'),
            value: isBlackWhiteMode,
            onChanged: onBlackWhiteModeChange,
          ),
        ],
      ),
    );
  }
}
