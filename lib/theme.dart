import 'package:flutter/material.dart';

ThemeData buildLightTheme() {
  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.light,
    colorScheme: ColorScheme.light(
      primary: const Color(0xFF6750A4),
      secondary: const Color(0xFF625B71),
      surface: const Color(0xFFFEF7FF),
      background: const Color(0xFFFFFBFF),
      error: const Color(0xFFB3261E),
      onPrimary: Colors.white,
      onSecondary: Colors.white,
      onSurface: const Color(0xFF1C1B1F),
      onBackground: const Color(0xFF1C1B1F),
      onError: Colors.white,
    ),
  );
}

ThemeData buildDarkTheme() {
  return ThemeData(
    useMaterial3: true,
    brightness: Brightness.dark,
    colorScheme: ColorScheme.dark(
      primary: const Color(0xFFD0BCFF),
      secondary: const Color(0xFFCCC2DC),
      surface: const Color(0xFF1C1B1F),
      background: const Color(0xFF1C1B1F),
      error: const Color(0xFFF2B8B5),
      onPrimary: const Color(0xFF381E72),
      onSecondary: const Color(0xFF332D41),
      onSurface: const Color(0xFFE6E1E5),
      onBackground: const Color(0xFFE6E1E5),
      onError: const Color(0xFF601410),
    ),
  );
}

enum AppColor { green, red, yellow }

Color getAppColor(AppColor color, bool isBlackWhiteMode) {
  switch (color) {
    case AppColor.green:
      return isBlackWhiteMode
          ? const Color(0xFF7F7F7F)
          : const Color(0xFF4CAF50);
    case AppColor.red:
      return isBlackWhiteMode
          ? const Color(0xFF111111)
          : const Color(0xFFF44336);
    case AppColor.yellow:
      return isBlackWhiteMode
          ? const Color(0xFFBBBBBB)
          : const Color(0xFFFFEB3B);
  }
}
