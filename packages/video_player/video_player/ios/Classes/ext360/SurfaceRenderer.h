//
//  SurfaceRenderer.h
//  video_player
//
//  Created by Eittipat K on 20/1/2565 BE.
//
#import "OpenGLRenderer.h"

@import OpenGLES;

@interface SurfaceRenderer : OpenGLRenderer
- (instancetype)initWithVideoOutput:(AVPlayerItemVideoOutput *)videoOutput;
- (void)setResolution:(int)width :(int)height;
- (void)setMediaFormat:(int)mediaFormat;
- (void)setCameraRotationWithRoll:(float)roll pitch:(float)pitch yaw:(float)yaw;
- (void)dispose;
@end
