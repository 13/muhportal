import 'package:flutter_test/flutter_test.dart';
import 'package:muhportal/models.dart';

void main() {
  group('Model Tests', () {
    test('PortalUpdate creation', () {
      final update = PortalUpdate(
        id: 'TEST',
        state: DoorState.open,
      );
      
      expect(update.id, 'TEST');
      expect(update.state, DoorState.open);
      expect(update.timestamp, isNotNull);
    });

    test('WolUpdate creation', () {
      final update = WolUpdate(
        id: 'PC1',
        name: 'Test PC',
        ip: '192.168.1.1',
        mac: '00:11:22:33:44:55',
        alive: true,
      );
      
      expect(update.id, 'PC1');
      expect(update.name, 'Test PC');
      expect(update.alive, true);
    });

    test('SensorUpdate creation', () {
      final update = SensorUpdate(
        id: 'SENSOR1',
        temp: 22.5,
        humidity: 45.0,
      );
      
      expect(update.id, 'SENSOR1');
      expect(update.temp, 22.5);
      expect(update.humidity, 45.0);
    });

    test('ConnState enum values', () {
      expect(ConnState.connected, isNotNull);
      expect(ConnState.connecting, isNotNull);
      expect(ConnState.disconnected, isNotNull);
    });
  });
}
