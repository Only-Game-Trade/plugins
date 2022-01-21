//
//  OpenGLRender.h
//  opengl_texture
//
//  Created by German Saprykin on 22/4/18.
//
#import <Flutter/Flutter.h>
#import <GLKit/GLKit.h>
#import <AVFoundation/AVFoundation.h>

@import OpenGLES;

@interface SurfaceRenderer : NSObject
@property (nonatomic, readonly) bool disposed;
- (instancetype)initWithVideoOutput:(AVPlayerItemVideoOutput *)videoOutput;
- (void)start;
- (CVPixelBufferRef)copyPixelBuffer;
- (void)dispose;
- (void)setMediaFormat:(int)mediaFormat;
- (void)setCameraRotationWithRoll:(float)roll pitch:(float)pitch yaw:(float)yaw;
@end
