//
//  Video360Renderer.h
//  Pods
//
//  Created by Eittipat Kraichingrith on 20/1/2565 BE.
//

#import <Foundation/Foundation.h>
#import <GLKit/GLKit.h>
#import "Mesh.h"
@import OpenGLES;


@interface Video360Renderer : NSObject
-(instancetype)initWithContext:(EAGLContext*)context withMesh:(Mesh*)mesh;
-(void)render;
-(void)updateTexture:(CVPixelBufferRef)pixelBuffer;
-(void)updateModelViewProjectionMatrix:(float)roll :(float)pitch :(float)yaw;
-(void)dispose;
@end
