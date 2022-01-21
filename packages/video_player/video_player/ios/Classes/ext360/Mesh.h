//
//  Mesh.h
//  video_player
//
//  Created by Eittipat Kraichingrith on 20/1/2565 BE.
//

#import <Foundation/Foundation.h>
#import <GLKit/GLKit.h>


@interface Mesh : NSObject
+(instancetype)createUvSphereWithRadius:(float)radius
                              latitudes:(int)latitudes
                             longitudes:(int)longitudes
                     verticalFovDegrees:(float)verticalFovDegrees
                   horizontalFovDegrees:(float)horizontalFovDegrees
                            mediaFormat:(int)mediaFormat;
-(void)glInit;
-(void)glDrawWithTexture:(CVOpenGLESTextureRef)texture mvpMatrix:(GLKMatrix4)mvpMatrix;
-(void)glDestroy;
@end

