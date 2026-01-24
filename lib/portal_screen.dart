import 'package:flutter/material.dart';
import 'models.dart';
import 'common_ui.dart';
import 'theme.dart';

enum PortalGroup { haustuer, garagentuer, garage }

class PortalScreen extends StatefulWidget {
  final ConnState connState;
  final Map<String, PortalUpdate> portalStates;
  final VoidCallback onRefresh;
  final Function(String) onToggle;
  final bool isBlackWhiteMode;
  final VoidCallback onOpenSettings;
  final Function(String) onShowSnackbar;

  const PortalScreen({
    super.key,
    required this.connState,
    required this.portalStates,
    required this.onRefresh,
    required this.onToggle,
    required this.isBlackWhiteMode,
    required this.onOpenSettings,
    required this.onShowSnackbar,
  });

  @override
  State<PortalScreen> createState() => _PortalScreenState();
}

class _PortalScreenState extends State<PortalScreen> {
  PortalGroup? _selectedGroup;
  bool _isRefreshing = false;

  Future<void> _handleRefresh() async {
    setState(() => _isRefreshing = true);
    widget.onRefresh();
    await Future.delayed(const Duration(seconds: 1));
    setState(() => _isRefreshing = false);
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        TitleBar(
          connState: widget.connState,
          onRefresh: widget.onRefresh,
          title: 'Portal',
          icon: Icons.lock,
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
            child: _PortalContent(
              portalStates: widget.portalStates,
              isBlackWhiteMode: widget.isBlackWhiteMode,
              onGroupClick: (group) => setState(() => _selectedGroup = group),
            ),
          ),
        ),
        if (_selectedGroup != null)
          _PortalActionDialog(
            group: _selectedGroup!,
            portalStates: widget.portalStates,
            isBlackWhiteMode: widget.isBlackWhiteMode,
            onDismiss: () => setState(() => _selectedGroup = null),
            onToggle: widget.onToggle,
            onShowSnackbar: widget.onShowSnackbar,
          ),
      ],
    );
  }
}

class _PortalContent extends StatelessWidget {
  final Map<String, PortalUpdate> portalStates;
  final bool isBlackWhiteMode;
  final Function(PortalGroup) onGroupClick;

  const _PortalContent({
    required this.portalStates,
    required this.isBlackWhiteMode,
    required this.onGroupClick,
  });

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        _PortalGroupCard(
          title: 'Haustür',
          group: PortalGroup.haustuer,
          portalIds: ['HD', 'HDL'],
          portalStates: portalStates,
          isBlackWhiteMode: isBlackWhiteMode,
          onTap: () => onGroupClick(PortalGroup.haustuer),
        ),
        const SizedBox(height: 16),
        _PortalGroupCard(
          title: 'Garagentür',
          group: PortalGroup.garagentuer,
          portalIds: ['GD', 'GDL'],
          portalStates: portalStates,
          isBlackWhiteMode: isBlackWhiteMode,
          onTap: () => onGroupClick(PortalGroup.garagentuer),
        ),
        const SizedBox(height: 16),
        _PortalGroupCard(
          title: 'Garage',
          group: PortalGroup.garage,
          portalIds: ['G'],
          portalStates: portalStates,
          isBlackWhiteMode: isBlackWhiteMode,
          onTap: () => onGroupClick(PortalGroup.garage),
        ),
      ],
    );
  }
}

class _PortalGroupCard extends StatefulWidget {
  final String title;
  final PortalGroup group;
  final List<String> portalIds;
  final Map<String, PortalUpdate> portalStates;
  final bool isBlackWhiteMode;
  final VoidCallback onTap;

  const _PortalGroupCard({
    required this.title,
    required this.group,
    required this.portalIds,
    required this.portalStates,
    required this.isBlackWhiteMode,
    required this.onTap,
  });

  @override
  State<_PortalGroupCard> createState() => _PortalGroupCardState();
}

class _PortalGroupCardState extends State<_PortalGroupCard> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    final doorState = widget.portalStates[widget.portalIds[0]]?.state ?? DoorState.unknown;
    final lockState = widget.portalIds.length > 1
        ? widget.portalStates[widget.portalIds[1]]?.state
        : null;

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
        child: Card(
          elevation: 2,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  widget.title,
                  style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    _StatusBadge(
                      text: doorState == DoorState.open ? 'OFFEN' : 'ZU',
                      color: getAppColor(
                        doorState == DoorState.open ? AppColor.red : AppColor.green,
                        widget.isBlackWhiteMode,
                      ),
                    ),
                    if (lockState != null) ...[
                      const SizedBox(width: 8),
                      _StatusBadge(
                        text: lockState == DoorState.open ? 'AUF' : 'VER',
                        color: getAppColor(
                          lockState == DoorState.open ? AppColor.red : AppColor.green,
                          widget.isBlackWhiteMode,
                        ),
                      ),
                    ],
                  ],
                ),
              ],
            ),
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

class _PortalActionDialog extends StatelessWidget {
  final PortalGroup group;
  final Map<String, PortalUpdate> portalStates;
  final bool isBlackWhiteMode;
  final VoidCallback onDismiss;
  final Function(String) onToggle;
  final Function(String) onShowSnackbar;

  const _PortalActionDialog({
    required this.group,
    required this.portalStates,
    required this.isBlackWhiteMode,
    required this.onDismiss,
    required this.onToggle,
    required this.onShowSnackbar,
  });

  String get _title {
    switch (group) {
      case PortalGroup.haustuer:
        return 'Haustür';
      case PortalGroup.garagentuer:
        return 'Garagentür';
      case PortalGroup.garage:
        return 'Garage';
    }
  }

  List<Map<String, String>> get _actions {
    switch (group) {
      case PortalGroup.haustuer:
        return [
          {'text': 'Öffnen', 'cmd': 'HD_O'},
          {'text': 'Entriegeln', 'cmd': 'HD_U'},
          {'text': 'Verriegeln', 'cmd': 'HD_L'},
        ];
      case PortalGroup.garagentuer:
        return [
          {'text': 'Öffnen', 'cmd': 'GD_O'},
          {'text': 'Entriegeln', 'cmd': 'GD_U'},
          {'text': 'Verriegeln', 'cmd': 'GD_L'},
        ];
      case PortalGroup.garage:
        return [
          {'text': 'Toggle', 'cmd': 'G_T'},
        ];
    }
  }

  @override
  Widget build(BuildContext context) {
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
                _title,
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
                  ..._actions.map((action) => Padding(
                        padding: const EdgeInsets.only(bottom: 16),
                        child: ActionButton(
                          text: action['text']!,
                          onPressed: () {
                            onToggle(action['cmd']!);
                            onShowSnackbar(action['text']!);
                            onDismiss();
                          },
                        ),
                      )),
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
