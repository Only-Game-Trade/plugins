//
//  Video360Renderer.m
//  video_player
//
//  Created by Eittipat Kraichingrith on 20/1/2565 BE.
//

#import "Video360Renderer.h"

#define FIELD_OF_VIEW 90

@interface Video360Renderer()

@end

@implementation Video360Renderer {
    EAGLContext* _context;
    Mesh *_mesh;
    GLKMatrix4 _mvpMatrix;
    CVOpenGLESTextureRef _texture;
    CVOpenGLESTextureCacheRef _textureCache;
}

-(instancetype)initWithContext:(EAGLContext*)context withMesh:(Mesh*)mesh {
    self = [super init];
    if(self) {
        _context = context;
        _mvpMatrix = GLKMatrix4Identity;
        _mesh = mesh;
        [_mesh glInit];
    }
    return self;
}

-(void)render {
    glClearColor(0.0, 0.0, 0.0, 1.0);
    glClear(GL_COLOR_BUFFER_BIT);
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    glEnable(GL_BLEND);
    [_mesh glDrawWithTexture:_texture mvpMatrix:_mvpMatrix];
}

-(void)updateModelViewProjectionMatrix:(float)roll :(float)pitch :(float)yaw {
    float fieldOfViewInRadians = GLKMathDegreesToRadians(FIELD_OF_VIEW);
    GLKMatrix4 projectionMatrix = GLKMatrix4MakePerspective(fieldOfViewInRadians, 1.0, 0.1, 100.0);
    GLKMatrix4 modelViewMatrix = GLKMatrix4MakeTranslation(0.0, 0.0, 0.0);
    modelViewMatrix = GLKMatrix4RotateX(modelViewMatrix,-GLKMathDegreesToRadians(pitch));
    modelViewMatrix = GLKMatrix4RotateY(modelViewMatrix,-GLKMathDegreesToRadians(yaw));
    modelViewMatrix = GLKMatrix4RotateZ(modelViewMatrix,-GLKMathDegreesToRadians(roll+180));
    _mvpMatrix = GLKMatrix4Multiply(projectionMatrix, modelViewMatrix);
}
-(void)updateTexture:(CVPixelBufferRef)pixelBuffer {
    if(_textureCache==NULL) {
        CVReturn result = CVOpenGLESTextureCacheCreate(kCFAllocatorDefault, nil,_context, nil, &_textureCache);
        assert(result == kCVReturnSuccess);
    }
    
    GLsizei textureWidth = (GLsizei)CVPixelBufferGetWidth(pixelBuffer);
    GLsizei textureHeight =(GLsizei)CVPixelBufferGetHeight(pixelBuffer);

    [self cleanTextures];

    glActiveTexture(GL_TEXTURE0);
    CVReturn result = CVOpenGLESTextureCacheCreateTextureFromImage(
                                                                   kCFAllocatorDefault,
                                                                   _textureCache,
                                                                   pixelBuffer,
                                                                   NULL,
                                                                   GL_TEXTURE_2D,
                                                                   GL_RGBA,
                                                                   textureWidth,
                                                                   textureHeight,
                                                                   GL_BGRA,
                                                                   GL_UNSIGNED_BYTE,
                                                                   0,
                                                                   &_texture);
    assert(result == kCVReturnSuccess);
}

-(void)cleanTextures {
    if(_texture) {
        CFRelease(_texture);
    }
    if(_textureCache) {
        CVOpenGLESTextureCacheFlush(_textureCache, 0);
    }
    
}

-(void)dispose {
    if(_mesh) {
        [_mesh glDestroy];
    }
    if(_texture) {
        CFRelease(_texture);
    }
    if(_textureCache) {
        CVOpenGLESTextureCacheFlush(_textureCache, 0);
        CFRelease(_textureCache);
    }
}
@end
