//
//  OpenGLRenderer.h
//  video_player
//
//  Created by Eittipat K on 22/1/2565 BE.
//

#import <Flutter/Flutter.h>
#import <GLKit/GLKit.h>
#import <AVFoundation/AVFoundation.h>

@import OpenGLES;

@protocol Renderer<NSObject>
- (void)onSurfaceCreated;
- (void)onSurfaceChanged:(int)width :(int)height;
- (void)updateTexture:(CVPixelBufferRef)pixelBuffer context:(EAGLContext*)context;
- (void)onDrawFrame;
@end

@interface OpenGLRenderer : NSObject<FlutterTexture>
@property (nonatomic, readonly) bool disposed;
- (instancetype)initWithVideoOutput:(AVPlayerItemVideoOutput *)videoOutput;
- (void)setRenderer:(id<Renderer>)renderer;
- (void)surfaceCreated;
- (void)surfaceChanged:(int)width :(int)height;
- (void)surfaceDestroyed;
- (void)dispose;
@end


