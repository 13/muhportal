import 'dart:async';
import 'dart:convert';
import 'package:mqtt_client/mqtt_client.dart';
import 'package:mqtt_client/mqtt_browser_client.dart';
import 'models.dart';

class PortalMqttClient {
  // TODO: Consider making these configurable via environment variables or settings UI
  // SECURITY WARNING: Currently using plain WebSocket (ws://) without TLS or authentication.
  // For production deployments on untrusted networks, consider:
  // 1. Using wss:// with TLS encryption
  // 2. Implementing MQTT authentication (username/password or certificates)
  // 3. Network-level security (VPN, firewall rules)
  static const String serverUri = '192.168.22.5';
  static const int serverPort = 1884;
  static const String clientIdPrefix = 'muhportal-';

  late MqttBrowserClient _client;
  ConnState _connState = ConnState.disconnected;
  StreamSubscription<List<MqttReceivedMessage<MqttMessage>>>? _subscription;

  final Function(ConnState) onConnState;
  final Function(PortalUpdate) onPortalUpdate;
  final Function(WolUpdate) onWolUpdate;
  final Function(SensorUpdate) onSensorUpdate;
  final Function(SwitchUpdate) onSwitchUpdate;
  final Function(PvUpdate) onPvUpdate;

  PortalMqttClient({
    required this.onConnState,
    required this.onPortalUpdate,
    required this.onWolUpdate,
    required this.onSensorUpdate,
    required this.onSwitchUpdate,
    required this.onPvUpdate,
  }) {
    final clientId = clientIdPrefix + DateTime.now().millisecondsSinceEpoch.toString();
    _client = MqttBrowserClient.withPort('ws://$serverUri', clientId, serverPort);
    _client.logging(on: false);
    _client.keepAlivePeriod = 60;
    _client.autoReconnect = true;
    _client.onConnected = _onConnected;
    _client.onDisconnected = _onDisconnected;
    _client.onSubscribed = _onSubscribed;
    // MqttBrowserClient uses WebSocket by default, no need to set useWebSocket
  }

  Future<void> connect() async {
    if (_client.connectionStatus?.state == MqttConnectionState.connected) {
      return;
    }

    _updateConnState(ConnState.connecting);

    try {
      await _client.connect();
    } catch (e) {
      _updateConnState(ConnState.disconnected);
      print('Connection failed: $e');
    }
  }

  void disconnect() {
    _client.disconnect();
    _updateConnState(ConnState.disconnected);
  }

  Future<void> reconnect() async {
    _updateConnState(ConnState.connecting);
    disconnect();
    await Future.delayed(const Duration(milliseconds: 500));
    await connect();
  }

  void toggle(String command) {
    if (_client.connectionStatus?.state != MqttConnectionState.connected) return;

    final builder = MqttClientPayloadBuilder();
    builder.addString(command);
    _client.publishMessage('muh/portal/RLY/cmnd', MqttQos.atMostOnce, builder.payload!);
  }

  void wolAction(String mac, String action) {
    if (_client.connectionStatus?.state != MqttConnectionState.connected) return;

    final topic = action == 'WAKE' ? 'muh/wol' : 'muh/poweroff';
    final payload = jsonEncode({'mac': mac});
    
    final builder = MqttClientPayloadBuilder();
    builder.addString(payload);
    _client.publishMessage(topic, MqttQos.atMostOnce, builder.payload!);
  }

  void setPower(String deviceId, bool state) {
    if (_client.connectionStatus?.state != MqttConnectionState.connected) return;

    final topic = 'tasmota/cmnd/$deviceId/POWER';
    final payload = state ? '1' : '0';
    
    final builder = MqttClientPayloadBuilder();
    builder.addString(payload);
    _client.publishMessage(topic, MqttQos.atMostOnce, builder.payload!);
  }

  void _onConnected() {
    _updateConnState(ConnState.connected);
    
    _client.subscribe('muh/portal/+/json', MqttQos.atMostOnce);
    _client.subscribe('muh/pc/+', MqttQos.atMostOnce);
    _client.subscribe('muh/sensors/#', MqttQos.atMostOnce);
    _client.subscribe('muh/wst/data/+', MqttQos.atMostOnce);
    _client.subscribe('muh/pv/+/json', MqttQos.atMostOnce);
    _client.subscribe('tasmota/tele/+/STATE', MqttQos.atMostOnce);
    _client.subscribe('tasmota/stat/+/RESULT', MqttQos.atMostOnce);

    // Cancel existing subscription to prevent memory leaks on reconnection
    _subscription?.cancel();
    _subscription = _client.updates?.listen(_onMessage);
  }

  void _onDisconnected() {
    _updateConnState(ConnState.disconnected);
  }

  void _onSubscribed(String topic) {
    print('Subscribed to $topic');
  }

  void _onMessage(List<MqttReceivedMessage<MqttMessage>> messages) {
    for (final message in messages) {
      final topic = message.topic;
      final payload = MqttPublishPayload.bytesToStringAsString(
        (message.payload as MqttPublishMessage).payload.message,
      );

      try {
        if (topic.startsWith('muh/portal/')) {
          _handlePortalMessage(topic, payload);
        } else if (topic.startsWith('muh/pc/')) {
          _handleWolMessage(topic, payload);
        } else if (topic.startsWith('muh/sensors/')) {
          _handleSensorMessage(topic, payload);
        } else if (topic.startsWith('muh/wst/data/')) {
          _handleWstMessage(topic, payload);
        } else if (topic.startsWith('muh/pv/')) {
          _handlePvMessage(topic, payload);
        } else if (topic.startsWith('tasmota/')) {
          _handleTasmotaMessage(topic, payload);
        }
      } catch (e) {
        print('Error processing message from $topic: $e');
      }
    }
  }

  void _handlePortalMessage(String topic, String payload) {
    final key = topic.replaceFirst('muh/portal/', '').replaceFirst('/json', '');
    if (key.isEmpty) return;

    final json = jsonDecode(payload) as Map<String, dynamic>;
    final update = PortalUpdate.fromJson(key, json);
    onPortalUpdate(update);
  }

  void _handleWolMessage(String topic, String payload) {
    final key = topic.replaceFirst('muh/pc/', '');
    if (key == 'cmnd') return;

    final json = jsonDecode(payload) as Map<String, dynamic>;
    final update = WolUpdate.fromJson(key, json);
    onWolUpdate(update);
  }

  void _handleSensorMessage(String topic, String payload) {
    final parts = topic.split('/');
    if (parts.length < 3) return;
    
    final id = parts.last == 'json' ? parts[parts.length - 2] : parts.last;
    final json = jsonDecode(payload) as Map<String, dynamic>;
    final update = SensorUpdate.fromJson(id, json);
    onSensorUpdate(update);
  }

  void _handleWstMessage(String topic, String payload) {
    final id = topic.replaceFirst('muh/wst/data/', '');
    final json = jsonDecode(payload) as Map<String, dynamic>;
    final update = SensorUpdate.fromJson(id, json);
    onSensorUpdate(update);
  }

  void _handlePvMessage(String topic, String payload) {
    final id = topic.replaceFirst('muh/pv/', '').replaceFirst('/json', '');
    final json = jsonDecode(payload) as Map<String, dynamic>;
    final update = PvUpdate.fromJson(id, json);
    onPvUpdate(update);
  }

  void _handleTasmotaMessage(String topic, String payload) {
    final key = topic.split('/')[2];
    
    try {
      final json = jsonDecode(payload) as Map<String, dynamic>;
      final update = SwitchUpdate.fromJson(key, json);
      onSwitchUpdate(update);
    } catch (e) {
      // Try parsing as simple string
      if (payload == '0' || payload == '1') {
        final update = SwitchUpdate(id: key, state: payload == '1');
        onSwitchUpdate(update);
      }
    }
  }

  void _updateConnState(ConnState state) {
    _connState = state;
    onConnState(state);
  }
}
