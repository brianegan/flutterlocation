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
  final double accuracy;
  final double altitude;

  Location(this.latitude, this.longitude, this.accuracy, this.altitude);

  factory Location.fromResult(Map<String, double> result) {
    return new Location(
      result['latitude'],
      result['longitude'],
      result['accuracy'],
      result['altitude'],
    );
  }

  @override
  String toString() =>
      'Location{latitude: $latitude, longitude: $longitude, accuracy: $accuracy, altitude: $altitude}';

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is Location &&
          runtimeType == other.runtimeType &&
          latitude == other.latitude &&
          longitude == other.longitude &&
          accuracy == other.accuracy &&
          altitude == other.altitude;

  @override
  int get hashCode =>
      latitude.hashCode ^
      longitude.hashCode ^
      accuracy.hashCode ^
      altitude.hashCode;
}
