import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:location/location.dart';

void main() {
  runApp(new LocationApp(plugin: new LocationPlugin()));
}

class LocationApp extends StatelessWidget {
  final LocationPlugin plugin;

  LocationApp({Key key, @required this.plugin}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      title: 'Location Demo',
      home: new Scaffold(
        appBar: new AppBar(
          title: new Text('Location Demo'),
        ),
        body: new FutureBuilder(
          future: plugin.getLocation,
          builder: (BuildContext context, AsyncSnapshot<Location> snapshot) {
            if (snapshot.hasData) {
              return new Center(
                child: new Text('${snapshot.data}'),
              );
            } else if (snapshot.hasError) {
              return new Center(child: new Text('${snapshot.error}'));
            } else {
              return new Center(child: new CircularProgressIndicator());
            }
          },
        ),
      ),
    );
  }
}

class LocationStreamApp extends StatelessWidget {
  final LocationPlugin plugin;

  LocationStreamApp({Key key, @required this.plugin}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return new MaterialApp(
      title: 'Location Demo',
      home: new Scaffold(
        appBar: new AppBar(
          title: new Text('Location Demo'),
        ),
        body: new StreamBuilder(
          stream: plugin.onLocationChanged,
          builder: (BuildContext context, AsyncSnapshot<Location> snapshot) {
            if (snapshot.hasData) {
              return new Center(
                child: new Text('Stream ${snapshot.data}'),
              );
            } else if (snapshot.hasError) {
              return new Center(child: new Text('${snapshot.error}'));
            } else {
              return new Center(child: new CircularProgressIndicator());
            }
          },
        ),
      ),
    );
  }
}
