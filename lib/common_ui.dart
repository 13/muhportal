import 'package:flutter/material.dart';
import 'package:intl/intl.dart';
import 'models.dart';
import 'theme.dart';

Color getConnColor(ConnState connState, bool isBlackWhiteMode) {
  switch (connState) {
    case ConnState.connected:
      return getAppColor(AppColor.green, isBlackWhiteMode);
    case ConnState.connecting:
      return getAppColor(AppColor.yellow, isBlackWhiteMode);
    case ConnState.disconnected:
      return getAppColor(AppColor.red, isBlackWhiteMode);
  }
}

String formatTime(int timestamp) {
  final now = DateTime.now();
  final time = DateTime.fromMillisecondsSinceEpoch(timestamp);
  
  final isSameDay = now.year == time.year &&
      now.month == time.month &&
      now.day == time.day;
  
  final pattern = isSameDay ? 'HH:mm' : 'dd.MM. HH:mm';
  return DateFormat(pattern).format(time);
}

class TitleBar extends StatelessWidget {
  final ConnState connState;
  final VoidCallback onRefresh;
  final String title;
  final IconData icon;
  final bool isBlackWhiteMode;
  final VoidCallback onOpenSettings;

  const TitleBar({
    super.key,
    required this.connState,
    required this.onRefresh,
    required this.title,
    required this.icon,
    required this.isBlackWhiteMode,
    required this.onOpenSettings,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 64,
      padding: const EdgeInsets.symmetric(horizontal: 16),
      child: Row(
        children: [
          Icon(
            icon,
            size: 28,
            color: connState == ConnState.connected
                ? getAppColor(AppColor.green, isBlackWhiteMode)
                : Colors.grey,
          ),
          const SizedBox(width: 32),
          Expanded(
            child: Text(
              title,
              style: TextStyle(
                fontSize: 24,
                color: Theme.of(context).colorScheme.onBackground,
              ),
            ),
          ),
          IconButton(
            onPressed: onRefresh,
            icon: Icon(
              Icons.refresh,
              color: Theme.of(context).colorScheme.onBackground,
            ),
          ),
          IconButton(
            onPressed: onOpenSettings,
            icon: Icon(
              Icons.settings,
              color: Theme.of(context).colorScheme.onBackground,
            ),
          ),
        ],
      ),
    );
  }
}

class ActionButton extends StatefulWidget {
  final String text;
  final IconData? icon;
  final Color? containerColor;
  final Color? contentColor;
  final VoidCallback onPressed;

  const ActionButton({
    super.key,
    required this.text,
    this.icon,
    this.containerColor,
    this.contentColor,
    required this.onPressed,
  });

  @override
  State<ActionButton> createState() => _ActionButtonState();
}

class _ActionButtonState extends State<ActionButton> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    final containerColor = widget.containerColor ??
        Theme.of(context).colorScheme.secondaryContainer;
    final contentColor = widget.contentColor ??
        Theme.of(context).colorScheme.onSecondaryContainer;

    return GestureDetector(
      onTapDown: (_) => setState(() => _isPressed = true),
      onTapUp: (_) => setState(() => _isPressed = false),
      onTapCancel: () => setState(() => _isPressed = false),
      child: AnimatedScale(
        scale: _isPressed ? 0.96 : 1.0,
        duration: const Duration(milliseconds: 100),
        child: SizedBox(
          width: double.infinity,
          height: 56,
          child: ElevatedButton(
            onPressed: widget.onPressed,
            style: ElevatedButton.styleFrom(
              backgroundColor: containerColor,
              foregroundColor: contentColor,
              elevation: 2,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(4),
              ),
            ),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                if (widget.icon != null) ...[
                  Icon(widget.icon, size: 24, color: contentColor),
                  const SizedBox(width: 8),
                ],
                Text(
                  widget.text,
                  style: TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.bold,
                    color: contentColor,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class ModalOverlay extends StatelessWidget {
  final VoidCallback onDismiss;
  final Widget child;

  const ModalOverlay({
    super.key,
    required this.onDismiss,
    required this.child,
  });

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: onDismiss,
      child: Container(
        color: Colors.black.withOpacity(0.6),
        child: Center(
          child: GestureDetector(
            onTap: () {}, // Prevent dismiss when tapping the dialog
            child: child,
          ),
        ),
      ),
    );
  }
}
