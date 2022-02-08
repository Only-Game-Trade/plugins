// Copyright 2013 The Flutter Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#import <Flutter/Flutter.h>
#import <AVFoundation/AVFoundation.h>

@interface FLTVideoPlayer : NSObject <FlutterTexture, FlutterStreamHandler>
@property(readonly, nonatomic) AVPlayer *player;
@property(readonly, nonatomic) AVPlayerItemVideoOutput *videoOutput;
@property(nonatomic, readonly) BOOL disposed;
@property(nonatomic, readonly) BOOL isPlaying;
@property(nonatomic, readonly) BOOL isInitialized;

- (void)play;

- (void)pause;

- (int64_t)position;

- (int64_t)duration;

- (void)seekTo:(int)location;

- (void)setIsLooping:(BOOL)isLooping;

- (void)setVolume:(double)volume;

- (void)setPlaybackSpeed:(double)speed;

- (CVPixelBufferRef)copyPixelBuffer;
@end



@interface FLTVideoPlayerPlugin : NSObject <FlutterPlugin>
@property(readonly, strong, nonatomic)
    NSMutableDictionary<NSNumber *, FLTVideoPlayer *> *playersByTextureId;

- (instancetype)initWithRegistrar:(NSObject<FlutterPluginRegistrar> *)registrar;
@end


