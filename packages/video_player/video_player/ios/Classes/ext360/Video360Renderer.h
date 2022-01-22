//
//  Video360Renderer.h
//  Pods
//
//  Created by Eittipat K on 20/1/2565 BE.
//

#import <Foundation/Foundation.h>
#import <GLKit/GLKit.h>
#import "Mesh.h"
#import "OpenGLRenderer.h"

@import OpenGLES;


@interface Video360Renderer : NSObject<Renderer>
-(instancetype)init;
-(void)configureSurface:(Mesh*)mesh;
-(void)setCameraRotation:(float)roll :(float)pitch :(float)yaw;
-(void)glShutdown;
@end
