import 'package:flutter/material.dart';
import 'models.dart';
import 'common_ui.dart';
import 'theme.dart';

class HAScreen extends StatefulWidget {
  final ConnState connState;
  final Map<String, SensorUpdate> sensorStates;
  final Map<String, SwitchUpdate> switchStates;
  final Map<String, PvUpdate> pvStates;
  final Function(String, bool) onSwitchAction;
  final VoidCallback onRefresh;
  final bool isBlackWhiteMode;
  final VoidCallback onOpenSettings;

  const HAScreen({
    super.key,
    required this.connState,
    required this.sensorStates,
    required this.switchStates,
    required this.pvStates,
    required this.onSwitchAction,
    required this.onRefresh,
    required this.isBlackWhiteMode,
    required this.onOpenSettings,
  });

  @override
  State<HAScreen> createState() => _HAScreenState();
}

class _HAScreenState extends State<HAScreen> {
  bool _isRefreshing = false;

  Future<void> _handleRefresh() async {
    setState(() => _isRefreshing = true);
    widget.onRefresh();
    await Future.delayed(const Duration(seconds: 1));
    setState(() => _isRefreshing = false);
  }

  @override
  Widget build(BuildContext context) {
    final tempSensor = widget.sensorStates['B327'];
    final pv = widget.pvStates['E07000055917'];
    final kommerSensor = widget.sensorStates['87'];
    final kommerSwitch = widget.switchStates['tasmota_BDC5E0'];
    final brennerS1 = widget.sensorStates['DS18B20-3628FF'];
    final brennerS2 = widget.sensorStates['DS18B20-1C16E1'];
    final brennerSwitch = widget.switchStates['tasmota_A7EEA3'];

    return Column(
      children: [
        TitleBar(
          connState: widget.connState,
          onRefresh: widget.onRefresh,
          title: 'HA',
          icon: Icons.lightbulb,
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
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                _HASection(
                  title: 'Temperatur',
                  value1: tempSensor != null ? '${tempSensor.temp.toStringAsFixed(1)}°' : null,
                  value2: tempSensor != null ? '${tempSensor.humidity.toStringAsFixed(0)}%' : null,
                  switchState: false,
                  onSwitchChange: (_) {},
                  isBlackWhiteMode: widget.isBlackWhiteMode,
                  showSwitch: false,
                ),
                const Divider(height: 48),
                _HASection(
                  title: 'PV',
                  value1: pv != null ? '${(pv.p1 + pv.p2).toStringAsFixed(0)}w' : null,
                  value2: pv != null ? '${pv.p1.toStringAsFixed(0)}/${pv.p2.toStringAsFixed(0)}' : null,
                  switchState: false,
                  onSwitchChange: (_) {},
                  isBlackWhiteMode: widget.isBlackWhiteMode,
                  showSwitch: false,
                ),
                const SizedBox(height: 24),
                _HASection(
                  title: 'PV Produktion',
                  value1: pv != null ? '${(pv.e1 + pv.e2).toStringAsFixed(1)}kW' : null,
                  value2: pv != null ? '${pv.e1.toStringAsFixed(1)}/${pv.e2.toStringAsFixed(1)}' : null,
                  switchState: false,
                  onSwitchChange: (_) {},
                  isBlackWhiteMode: widget.isBlackWhiteMode,
                  showSwitch: false,
                ),
                const Divider(height: 48),
                _HASection(
                  title: 'Kommer',
                  value1: kommerSensor != null ? '${kommerSensor.temp.toStringAsFixed(0)}°' : null,
                  value2: kommerSensor != null ? '${kommerSensor.humidity.toStringAsFixed(0)}%' : null,
                  switchState: kommerSwitch?.state ?? false,
                  onSwitchChange: (value) => widget.onSwitchAction('tasmota_BDC5E0', value),
                  isBlackWhiteMode: widget.isBlackWhiteMode,
                ),
                const SizedBox(height: 24),
                _HASection(
                  title: 'Brenner',
                  value1: brennerS1 != null ? '${brennerS1.temp.toStringAsFixed(0)}°' : null,
                  value2: brennerS2 != null ? '${brennerS2.temp.toStringAsFixed(0)}°' : null,
                  switchState: brennerSwitch?.state ?? false,
                  onSwitchChange: (value) => widget.onSwitchAction('tasmota_A7EEA3', value),
                  isBlackWhiteMode: widget.isBlackWhiteMode,
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _HASection extends StatelessWidget {
  final String title;
  final String? value1;
  final String? value2;
  final bool switchState;
  final Function(bool) onSwitchChange;
  final bool isBlackWhiteMode;
  final bool showSwitch;

  const _HASection({
    required this.title,
    required this.value1,
    required this.value2,
    required this.switchState,
    required this.onSwitchChange,
    required this.isBlackWhiteMode,
    this.showSwitch = true,
  });

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Expanded(
          child: Text(
            title,
            style: const TextStyle(fontSize: 24, color: Colors.grey),
          ),
        ),
        Row(
          children: [
            Text(
              value1 ?? '--.-',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
                color: Theme.of(context).colorScheme.onBackground,
              ),
            ),
            const SizedBox(width: 12),
            Text(
              value2 ?? '--.-',
              style: TextStyle(
                fontSize: 16,
                color: Theme.of(context).colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
        if (showSwitch) ...[
          const SizedBox(width: 16),
          Switch(
            value: switchState,
            onChanged: onSwitchChange,
            activeColor: getAppColor(AppColor.green, isBlackWhiteMode),
            activeTrackColor: getAppColor(AppColor.green, isBlackWhiteMode).withOpacity(0.5),
          ),
        ],
      ],
    );
  }
}
