// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// ignore_for_file: public_member_api_docs

/// An example of using the plugin, controlling lifecycle and playback of the
/// video.

import 'dart:math';

import 'package:flutter/cupertino.dart';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';

void main() {
  runApp(
    MaterialApp(
      home: _App(),
    ),
  );
}

class _App extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      key: const ValueKey<String>('home_page'),
      appBar: AppBar(
        title: const Text('Video player example'),
        actions: <Widget>[
          IconButton(
            key: const ValueKey<String>('push_tab'),
            icon: const Icon(Icons.navigation),
            onPressed: () {
              Navigator.push<_PlayerVideoAndPopPage>(
                context,
                MaterialPageRoute<_PlayerVideoAndPopPage>(
                  builder: (BuildContext context) => _PlayerVideoAndPopPage(),
                ),
              );
            },
          )
        ],
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            _BumbleBeeRemoteVideo(url: 'https://flutter.github.io/assets-for-api-docs/assets/videos/bee.mp4', mediaFormat: MediaFormat.STANDARD),
            _BumbleBeeRemoteVideo(url: 'https://content.tockto.me/JP-JP-01/JP-JP-0101/JP-JP-010101/paid.m3u8',mediaFormat: MediaFormat.VR3D180_SBS),
          ],
        ),
      ),
    );
  }
}

class _BumbleBeeRemoteVideo extends StatefulWidget {
  final String url;
  final int mediaFormat;

  _BumbleBeeRemoteVideo({
    Key? key,
    required this.url,
    required this.mediaFormat,
  }) : super(key: key);

  @override
  _BumbleBeeRemoteVideoState createState() => _BumbleBeeRemoteVideoState();
}

double _cameraPitch = 0;
double _cameraYaw = 0;


class _BumbleBeeRemoteVideoState extends State<_BumbleBeeRemoteVideo> {
  late VideoPlayerController _controller;


  Future<ClosedCaptionFile> _loadCaptions() async {
    final String fileContents = await DefaultAssetBundle.of(context).loadString('assets/bumble_bee_captions.vtt');
    return WebVTTCaptionFile(fileContents); // For vtt files, use WebVTTCaptionFile
  }

  @override
  void initState() {
    super.initState();
    _controller = VideoPlayerController.network(
      widget.url,
      //'https://content.tockto.me/JP-JP-01/JP-JP-0101/JP-JP-010101/paid.m3u8',
      //'https://bitmovin-a.akamaihd.net/content/playhouse-vr/progressive.mp4',
      //'https://content.tockto.me/sample180_720.mp4',
      //closedCaptionFile: _loadCaptions(),
      videoPlayerOptions: VideoPlayerOptions(
        mixWithOthers: true,
        mediaFormat: widget.mediaFormat,
      ),
    );

    _controller.addListener(() {
      setState(() {});
    });
    _controller.setLooping(true);
    _controller.initialize();
  }



  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Column(
        children: <Widget>[
          Container(padding: const EdgeInsets.only(top: 20.0)),
          const Text('With remote mp4'),
          Container(
            padding: const EdgeInsets.all(20),
            child: AspectRatio(
              aspectRatio: _controller.value.mediaFormat == 0 ? _controller.value.aspectRatio : 1.0,
              child: Stack(
                alignment: Alignment.bottomCenter,
                children: <Widget>[
                  VideoPlayer(_controller),
                  //ClosedCaption(text: _controller.value.caption.text),
                  _ControlsOverlay(controller: _controller),
                  VideoProgressIndicator(_controller, allowScrubbing: true),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}



class _ControlsOverlay extends StatelessWidget {
  const _ControlsOverlay({Key? key, required this.controller}) : super(key: key);

  static const _examplePlaybackRates = [
    0.25,
    0.5,
    1.0,
    1.5,
    2.0,
    3.0,
    5.0,
    10.0,
  ];

  static const _mediaFormats = {
    MediaFormat.STANDARD: "Standard",
    MediaFormat.VR2D180: "Monoscopic 180",
    MediaFormat.VR2D360: "Monoscopic 360",
    MediaFormat.VR3D180_OU: "Stereoscopic 180 OverUnder",
    MediaFormat.VR3D180_SBS: "Stereoscopic 180 SideBySide",
    MediaFormat.VR3D360_OU: "Stereoscopic 360 OverUnder",
    MediaFormat.VR3D360_SBS: "Stereoscopic 360 SideBySide",
  };

  final VideoPlayerController controller;

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: <Widget>[
        AnimatedSwitcher(
          duration: Duration(milliseconds: 50),
          reverseDuration: Duration(milliseconds: 200),
          child: controller.value.isPlaying
              ? SizedBox.shrink()
              : Container(
                  color: Colors.black26,
                  child: Center(
                    child: Icon(
                      Icons.play_arrow,
                      color: Colors.white,
                      size: 100.0,
                      semanticLabel: 'Play',
                    ),
                  ),
                ),
        ),
        GestureDetector(
          onTap: () {
            controller.value.isPlaying ? controller.pause() : controller.play();
          },
          onPanUpdate: (details) {
            double touchX = details.delta.dx;
            double touchY = details.delta.dy;
            double r = 0;
            double cr = cos(r);
            double sr = sin(r);
            _cameraYaw -= cr * touchX - sr * touchY;
            _cameraPitch += sr * touchX + cr * touchY;
            _cameraPitch = max(-45, min(45, _cameraPitch));
            controller.setCameraRotation(0.0, _cameraPitch, _cameraYaw);
          },
        ),
        Align(
          alignment: Alignment.topRight,
          child: PopupMenuButton<double>(
            initialValue: controller.value.playbackSpeed,
            tooltip: 'Playback speed',
            onSelected: (speed) {
              controller.setPlaybackSpeed(speed);
            },
            itemBuilder: (context) {
              return [
                for (final speed in _examplePlaybackRates)
                  PopupMenuItem(
                    value: speed,
                    child: Text('${speed}x'),
                  )
              ];
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(
                // Using less vertical padding as the text is also longer
                // horizontally, so it feels like it would need more spacing
                // horizontally (matching the aspect ratio of the video).
                vertical: 12,
                horizontal: 16,
              ),
              child: Text('${controller.value.playbackSpeed}x'),
            ),
          ),
        ),
        Align(
          alignment: Alignment.topLeft,
          child: PopupMenuButton<int>(
            initialValue: controller.value.mediaFormat,
            tooltip: 'Switch between media format',
            onSelected: (format) {
              controller.setMediaFormat(format);
            },
            itemBuilder: (context) {
              return [
                for (var keyvalue in _mediaFormats.entries)
                  PopupMenuItem(
                    value: keyvalue.key,
                    child: Text(keyvalue.value),
                  ),
              ];
            },
            child: Padding(
              padding: const EdgeInsets.symmetric(
                // Using less vertical padding as the text is also longer
                // horizontally, so it feels like it would need more spacing
                // horizontally (matching the aspect ratio of the video).
                vertical: 12,
                horizontal: 16,
              ),
              child: Text(_mediaFormats[controller.value.mediaFormat]!),
            ),
          ),
        ),
      ],
    );
  }
}


class _PlayerVideoAndPopPage extends StatefulWidget {
  @override
  _PlayerVideoAndPopPageState createState() => _PlayerVideoAndPopPageState();
}

class _PlayerVideoAndPopPageState extends State<_PlayerVideoAndPopPage> {
  late VideoPlayerController _videoPlayerController;
  bool startedPlaying = false;

  @override
  void initState() {
    super.initState();

    _videoPlayerController = VideoPlayerController.asset('assets/Butterfly-209.mp4');
    _videoPlayerController.addListener(() {
      if (startedPlaying && !_videoPlayerController.value.isPlaying) {
        if(Navigator.canPop(context)) {
          Navigator.pop(context);
        }
      }
    });
  }

  @override
  void dispose() {
    _videoPlayerController.dispose();
    super.dispose();
  }

  Future<bool> started() async {
    await _videoPlayerController.initialize();
    await _videoPlayerController.play();
    startedPlaying = true;
    return true;
  }

  @override
  Widget build(BuildContext context) {
    return Material(
      elevation: 0,
      child: Center(
        child: FutureBuilder<bool>(
          future: started(),
          builder: (BuildContext context, AsyncSnapshot<bool> snapshot) {
            if (snapshot.data == true) {
              return AspectRatio(
                aspectRatio: _videoPlayerController.value.aspectRatio,
                child: VideoPlayer(_videoPlayerController),
              );
            } else {
              return const Text('waiting for video to load');
            }
          },
        ),
      ),
    );
  }
}