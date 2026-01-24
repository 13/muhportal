enum DoorState { open, closed, unknown }

enum ConnState { connecting, connected, disconnected }

class PortalUpdate {
  final String id;
  final DoorState state;
  final int timestamp;

  PortalUpdate({
    required this.id,
    required this.state,
    int? timestamp,
  }) : timestamp = timestamp ?? DateTime.now().millisecondsSinceEpoch;

  factory PortalUpdate.fromJson(String id, Map<String, dynamic> json) {
    final stateInt = json['state'] as int? ?? -1;
    final state = stateInt == 0
        ? DoorState.open
        : stateInt == 1
            ? DoorState.closed
            : DoorState.unknown;

    return PortalUpdate(
      id: id,
      state: state,
      timestamp: _tryParseTime(json),
    );
  }

  Map<String, dynamic> toJson() => {
        'state': state.name,
        'timestamp': timestamp,
      };
}

class WolUpdate {
  final String id;
  final String name;
  final String ip;
  final String mac;
  final bool alive;
  final int priority;
  final int timestamp;

  WolUpdate({
    required this.id,
    required this.name,
    required this.ip,
    required this.mac,
    required this.alive,
    this.priority = 99,
    int? timestamp,
  }) : timestamp = timestamp ?? DateTime.now().millisecondsSinceEpoch;

  factory WolUpdate.fromJson(String id, Map<String, dynamic> json) {
    return WolUpdate(
      id: id,
      name: json['name'] as String,
      ip: json['ip'] as String,
      mac: json['mac'] as String,
      alive: json['alive'] as bool,
      priority: json['priority'] as int? ?? 99,
      timestamp: _tryParseTime(json),
    );
  }

  Map<String, dynamic> toJson() => {
        'name': name,
        'ip': ip,
        'mac': mac,
        'alive': alive,
        'priority': priority,
        'timestamp': timestamp,
      };
}

class SensorUpdate {
  final String id;
  final double temp;
  final double humidity;
  final int timestamp;

  SensorUpdate({
    required this.id,
    required this.temp,
    this.humidity = 0.0,
    int? timestamp,
  }) : timestamp = timestamp ?? DateTime.now().millisecondsSinceEpoch;

  factory SensorUpdate.fromJson(String id, Map<String, dynamic> json) {
    double? temp;
    if (json.containsKey('T1')) {
      temp = (json['T1'] as num).toDouble();
    } else if (json.containsKey('DS18B20')) {
      temp = (json['DS18B20']['Temperature'] as num).toDouble();
    } else if (json.containsKey('temp_c')) {
      temp = (json['temp_c'] as num).toDouble();
    }

    final humidity = json.containsKey('H1')
        ? (json['H1'] as num).toDouble()
        : json.containsKey('humidity')
            ? (json['humidity'] as num).toDouble()
            : 0.0;

    return SensorUpdate(
      id: id,
      temp: temp ?? 0.0,
      humidity: humidity,
      timestamp: _tryParseTime(json),
    );
  }

  Map<String, dynamic> toJson() => {
        'temp': temp,
        'humidity': humidity,
        'timestamp': timestamp,
      };
}

class SwitchUpdate {
  final String id;
  final bool state;
  final int timestamp;

  SwitchUpdate({
    required this.id,
    required this.state,
    int? timestamp,
  }) : timestamp = timestamp ?? DateTime.now().millisecondsSinceEpoch;

  factory SwitchUpdate.fromJson(String id, Map<String, dynamic> json) {
    bool state = false;
    if (json.containsKey('POWER')) {
      final powerStr = json['POWER'] as String;
      state = powerStr == 'ON' || powerStr == '1';
    } else if (json.containsKey('POWER1')) {
      final powerStr = json['POWER1'] as String;
      state = powerStr == 'ON' || powerStr == '1';
    }

    return SwitchUpdate(
      id: id,
      state: state,
      timestamp: _tryParseTime(json),
    );
  }

  Map<String, dynamic> toJson() => {
        'state': state,
        'timestamp': timestamp,
      };
}

class PvUpdate {
  final String id;
  final double p1;
  final double p2;
  final double e1;
  final double e2;
  final int timestamp;

  PvUpdate({
    required this.id,
    required this.p1,
    required this.p2,
    required this.e1,
    required this.e2,
    int? timestamp,
  }) : timestamp = timestamp ?? DateTime.now().millisecondsSinceEpoch;

  factory PvUpdate.fromJson(String id, Map<String, dynamic> json) {
    final data = json['data'] as Map<String, dynamic>;
    return PvUpdate(
      id: id,
      p1: (data['p1'] as num).toDouble(),
      p2: (data['p2'] as num).toDouble(),
      e1: (data['e1'] as num).toDouble(),
      e2: (data['e2'] as num).toDouble(),
      timestamp: _tryParseTime(json),
    );
  }

  Map<String, dynamic> toJson() => {
        'p1': p1,
        'p2': p2,
        'e1': e1,
        'e2': e2,
        'timestamp': timestamp,
      };
}

int _tryParseTime(Map<String, dynamic> json) {
  final timeKeys = ['timestamp', 'time', 'Time', 'ts', 'last_seen'];
  
  for (final key in timeKeys) {
    if (json.containsKey(key)) {
      final timeValue = json[key];
      
      if (timeValue is int) {
        return timeValue < 10000000000 ? timeValue * 1000 : timeValue;
      }
      
      if (timeValue is String) {
        final parsedInt = int.tryParse(timeValue);
        if (parsedInt != null) {
          return parsedInt < 10000000000 ? parsedInt * 1000 : parsedInt;
        }
        
        try {
          final dateTime = DateTime.parse(timeValue);
          return dateTime.millisecondsSinceEpoch;
        } catch (e) {
          // Ignore parse errors
        }
      }
    }
  }
  
  return DateTime.now().millisecondsSinceEpoch;
}
