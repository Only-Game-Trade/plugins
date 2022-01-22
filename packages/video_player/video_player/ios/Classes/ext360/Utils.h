//
//  Utils.h
//  video_player
//
//  Created by Eittipat K on 20/1/2565 BE.
//

#import <Foundation/Foundation.h>
#import <GLKit/GLKit.h>

@interface Utils : NSObject
+(void)checkGlError;
+(GLuint)compileProgramWithVertexCode:(NSString*)vertexCode fragmentCode:(NSString*)fragmentCode;
@end


