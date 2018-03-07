import 'dart:async';

import 'package:flutter/services.dart';

class LocationPlugin {
  static const MethodChannel _channel = const MethodChannel('lyokone/location');
  static const EventChannel _stream =
      const EventChannel('lyokone/locationstream');

  Stream<Map<String, double>> _onLocationChanged;

  Future<Location> get getLocation => _channel
      .invokeMethod('getLocation')
      .then((result) => new Location.fromResult(result));

  Stream<Location> get onLocationChanged {
    if (_onLocationChanged == null) {
      _onLocationChanged = _stream.receiveBroadcastStream();
    }
    return _onLocationChanged.map((result) => new Location.fromResult(result));
  }
}

class Location {
  final double latitude;
  final double longitude;

  Location(this.latitude, this.longitude);

  factory Location.fromResult(Map<String, double> result) {
    return new Location(result['latitude'], result['longitude']);
  }

  @override
  String toString() {
    return 'LocationData{latitude: $latitude, longitude: $longitude}';
  }

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Location &&
          runtimeType == other.runtimeType &&
          latitude == other.latitude &&
          longitude == other.longitude;

  @override
  int get hashCode => latitude.hashCode ^ longitude.hashCode;
}
