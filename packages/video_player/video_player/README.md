# Video Player Exra plugin for Flutter

[![pub package](https://img.shields.io/pub/v/video_player_extra.svg)](https://pub.dev/packages/video_player_extra)

A fork of flutter's video_player with extra ability to play 180 or 360 videos.

This plugin maintains same interface with the original package excepts:
1. Add mediaFormat in VideoPlayerOption
2. Add setMediaFormat in VideoPlayerController
3. Add setCameraRotation in VideoPlayerController

Ideally, this plugin can be used as drop-in replacement for video_player package.


![The example app running in iOS](https://github.com/Eittipat/plugins/blob/video_player_360/packages/video_player/video_player/doc/demo360.gif?raw=true)

## Camera control

Just pass roll pitch yaw (in degree) to setCameraRotation method. Please see [full example here](https://github.com/Eittipat/plugins/blob/video_player_360/packages/video_player/video_player/example/lib/main.dart)

## Example

```dart
import 'package:video_player_extra/video_player.dart';
import 'package:flutter/material.dart';

void main() => runApp(VideoApp());

class VideoApp extends StatefulWidget {
  @override
  _VideoAppState createState() => _VideoAppState();
}

class _VideoAppState extends State<VideoApp> {
  VideoPlayerController _controller;

  @override
  void initState() {
    super.initState();
    _controller = VideoPlayerController.network(
      'https://videojs-vr.netlify.app/samples/eagle-360.mp4',
      videoPlayerOptions: VideoPlayerOptions(
        mixWithOthers: true,
        mediaFormat: MediaFormat.VR2D360,
      ),
    )..initialize().then((_) {
      // Ensure the first frame is shown after the video is initialized, even before the play button has been pressed.
      setState(() {});
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Video Demo',
      home: Scaffold(
        body: Center(
          child: _controller.value.isInitialized
              ? AspectRatio(
                  aspectRatio: _controller.value.aspectRatio,
                  child: VideoPlayer(_controller),
                )
              : Container(),
        ),
        floatingActionButton: FloatingActionButton(
          onPressed: () {
            setState(() {
              _controller.value.isPlaying
                  ? _controller.pause()
                  : _controller.play();
            });
          },
          child: Icon(
            _controller.value.isPlaying ? Icons.pause : Icons.play_arrow,
          ),
        ),
      ),
    );
  }

  @override
  void dispose() {
    super.dispose();
    _controller.dispose();
  }
}
```

## More information
Please read the original [README](https://pub.dev/packages/video_player).


## Credits
This package contains some code from Android AOSP project & Google VR SDK




