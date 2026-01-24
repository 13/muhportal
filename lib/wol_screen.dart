import 'package:flutter/material.dart';
import 'models.dart';
import 'common_ui.dart';
import 'theme.dart';

class WolScreen extends StatefulWidget {
  final ConnState connState;
  final Map<String, WolUpdate> wolStates;
  final VoidCallback onRefresh;
  final Function(String, String) onWolAction;
  final bool isBlackWhiteMode;
  final VoidCallback onOpenSettings;
  final Function(String) onShowSnackbar;

  const WolScreen({
    super.key,
    required this.connState,
    required this.wolStates,
    required this.onRefresh,
    required this.onWolAction,
    required this.isBlackWhiteMode,
    required this.onOpenSettings,
    required this.onShowSnackbar,
  });

  @override
  State<WolScreen> createState() => _WolScreenState();
}

class _WolScreenState extends State<WolScreen> {
  WolUpdate? _selectedWol;
  bool _isRefreshing = false;

  Future<void> _handleRefresh() async {
    setState(() => _isRefreshing = true);
    widget.onRefresh();
    await Future.delayed(const Duration(seconds: 1));
    setState(() => _isRefreshing = false);
  }

  @override
  Widget build(BuildContext context) {
    final sortedWols = widget.wolStates.values.toList()
      ..sort((a, b) {
        final priorityCompare = a.priority.compareTo(b.priority);
        return priorityCompare != 0 ? priorityCompare : a.name.compareTo(b.name);
      });

    return Column(
      children: [
        TitleBar(
          connState: widget.connState,
          onRefresh: widget.onRefresh,
          title: 'WOL',
          icon: Icons.lan,
          isBlackWhiteMode: widget.isBlackWhiteMode,
          onOpenSettings: widget.onOpenSettings,
        ),
        Container(
          height: 4,
          color: getConnColor(widget.connState, widget.isBlackWhiteMode),
        ),
        Expanded(
          child: RefreshIndicator(
            onRefresh: _handleRefresh,
            child: ListView.builder(
              padding: const EdgeInsets.only(top: 16),
              itemCount: sortedWols.length,
              itemBuilder: (context, index) {
                final wol = sortedWols[index];
                return _WolItem(
                  wol: wol,
                  isBlackWhiteMode: widget.isBlackWhiteMode,
                  onTap: () {
                    if (wol.mac.isNotEmpty) {
                      setState(() => _selectedWol = wol);
                    }
                  },
                );
              },
            ),
          ),
        ),
        if (_selectedWol != null)
          _WolActionDialog(
            wol: _selectedWol!,
            onDismiss: () => setState(() => _selectedWol = null),
            onAction: widget.onWolAction,
            onShowSnackbar: widget.onShowSnackbar,
          ),
      ],
    );
  }
}

class _WolItem extends StatefulWidget {
  final WolUpdate wol;
  final bool isBlackWhiteMode;
  final VoidCallback onTap;

  const _WolItem({
    required this.wol,
    required this.isBlackWhiteMode,
    required this.onTap,
  });

  @override
  State<_WolItem> createState() => _WolItemState();
}

class _WolItemState extends State<_WolItem> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    final displayName = widget.wol.name.split('.').first;

    return GestureDetector(
      onTapDown: (_) => setState(() => _isPressed = true),
      onTapUp: (_) {
        setState(() => _isPressed = false);
        widget.onTap();
      },
      onTapCancel: () => setState(() => _isPressed = false),
      child: AnimatedScale(
        scale: _isPressed ? 0.98 : 1.0,
        duration: const Duration(milliseconds: 100),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    displayName,
                    style: TextStyle(
                      fontSize: 24,
                      color: Theme.of(context).colorScheme.onBackground,
                    ),
                  ),
                  _StatusBadge(
                    text: widget.wol.alive ? 'ON' : 'OFF',
                    color: getAppColor(
                      widget.wol.alive ? AppColor.green : AppColor.red,
                      widget.isBlackWhiteMode,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 4),
              Text(
                widget.wol.ip,
                style: const TextStyle(fontSize: 14, color: Colors.grey),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _StatusBadge extends StatelessWidget {
  final String text;
  final Color color;

  const _StatusBadge({
    required this.text,
    required this.color,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 48,
      padding: const EdgeInsets.symmetric(vertical: 4),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(4),
      ),
      child: Text(
        text,
        textAlign: TextAlign.center,
        style: const TextStyle(
          fontSize: 14,
          fontWeight: FontWeight.bold,
          color: Colors.white,
        ),
      ),
    );
  }
}

class _WolActionDialog extends StatelessWidget {
  final WolUpdate wol;
  final VoidCallback onDismiss;
  final Function(String, String) onAction;
  final Function(String) onShowSnackbar;

  const _WolActionDialog({
    required this.wol,
    required this.onDismiss,
    required this.onAction,
    required this.onShowSnackbar,
  });

  @override
  Widget build(BuildContext context) {
    final displayName = wol.name.split('.').first;

    return ModalOverlay(
      onDismiss: onDismiss,
      child: Card(
        margin: const EdgeInsets.all(16),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(vertical: 12),
              decoration: BoxDecoration(
                color: Theme.of(context).colorScheme.surfaceVariant,
                borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
              ),
              child: Text(
                displayName,
                textAlign: TextAlign.center,
                style: TextStyle(
                  fontSize: 24,
                  fontWeight: FontWeight.w500,
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                children: [
                  ActionButton(
                    text: 'Wake',
                    icon: Icons.power_settings_new,
                    onPressed: () {
                      onAction(wol.mac, 'WAKE');
                      onShowSnackbar('Wake $displayName');
                      onDismiss();
                    },
                  ),
                  const SizedBox(height: 16),
                  ActionButton(
                    text: 'Shutdown',
                    icon: Icons.power_settings_new,
                    onPressed: () {
                      onAction(wol.mac, 'SHUTDOWN');
                      onShowSnackbar('Shutdown $displayName');
                      onDismiss();
                    },
                  ),
                  const SizedBox(height: 16),
                  ActionButton(
                    // Note: German strings match original Android app (e.g., "Abbrechen" = "Cancel")
                    text: 'Abbrechen',
                    containerColor: Theme.of(context).colorScheme.onSurface,
                    contentColor: Theme.of(context).colorScheme.surface,
                    onPressed: onDismiss,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
