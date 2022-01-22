//
//  Video360Renderer.m
//  video_player
//
//  Created by Eittipat K on 20/1/2565 BE.
//

#import "Video360Renderer.h"

#define FIELD_OF_VIEW 90

@interface Video360Renderer()

@end

@implementation Video360Renderer {
  Mesh *_requestedDisplayMesh;
  Mesh *_displayMesh;
  GLKMatrix4 _mvpMatrix;
  CVOpenGLESTextureRef _texture;
  CVOpenGLESTextureCacheRef _textureCache;
  float _roll;
  float _pitch;
  float _yaw;
}

-(instancetype)init {
  self = [super init];
  if(self) {
    _mvpMatrix = GLKMatrix4Identity;
    _roll=0;
    _pitch=0;
    _yaw=0;
  }
  return self;
}

-(void)configureSurface:(Mesh*)mesh {
  _requestedDisplayMesh = mesh;
}

- (void)onDrawFrame {
  [self computePerspective];
  
  if([self glConfigureScene]==NO) {
    return;
  }
  
  glClear(GL_COLOR_BUFFER_BIT);
  glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
  glEnable(GL_BLEND);
  
  // if frame available update texture!
  
  [_displayMesh glDrawWithTexture:_texture mvpMatrix:_mvpMatrix];
}

- (void)onSurfaceChanged:(int)width :(int)height {
  glViewport(0, 0, width, height);
  [self computePerspective];
}

- (void)onSurfaceCreated {
  glClearColor(0.0, 0.0, 0.0, 1.0);
}


-(BOOL)glConfigureScene {
  
  // This scene is not ready! no mesh
  if(_displayMesh==NULL && _requestedDisplayMesh==NULL) {
    return NO;
  }
  
  // This scene is ready, no change
  if(_requestedDisplayMesh==NULL) {
    return YES;
  }
  
  // Configure scene
  if(_displayMesh) {
    [_displayMesh glDestroy];
  }
  
  _displayMesh = _requestedDisplayMesh;
  _requestedDisplayMesh = NULL;
  [_displayMesh glInit];
  return YES;
}

-(void)computePerspective {
  float fieldOfViewInRadians = GLKMathDegreesToRadians(FIELD_OF_VIEW);
  GLKMatrix4 projectionMatrix = GLKMatrix4MakePerspective(fieldOfViewInRadians, 1.0, 0.1, 100.0);
  GLKMatrix4 modelViewMatrix = GLKMatrix4Identity;
  modelViewMatrix = GLKMatrix4RotateX(modelViewMatrix,-GLKMathDegreesToRadians(_pitch));
  modelViewMatrix = GLKMatrix4RotateY(modelViewMatrix,-GLKMathDegreesToRadians(_yaw));
  modelViewMatrix = GLKMatrix4RotateZ(modelViewMatrix,-GLKMathDegreesToRadians(_roll));
  _mvpMatrix = GLKMatrix4Multiply(projectionMatrix, modelViewMatrix);
}

-(void)setCameraRotation:(float)roll :(float)pitch :(float)yaw {
  _roll=roll;
  _pitch=pitch;
  _yaw=yaw;
}

-(void)updateTexture:(CVPixelBufferRef)pixelBuffer context:(EAGLContext*)context {
  if(_textureCache==NULL) {
    CVReturn result = CVOpenGLESTextureCacheCreate(kCFAllocatorDefault, nil,context, nil, &_textureCache);
    assert(result == kCVReturnSuccess);
  }
  
  GLsizei textureWidth = (GLsizei)CVPixelBufferGetWidth(pixelBuffer);
  GLsizei textureHeight =(GLsizei)CVPixelBufferGetHeight(pixelBuffer);
  
  if(_texture) {
    CFRelease(_texture);
    _texture = NULL;
  }
  if(_textureCache) {
    CVOpenGLESTextureCacheFlush(_textureCache, 0);
  }
  
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

-(void)glShutdown {
  if(_displayMesh) {
    [_displayMesh glDestroy];
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
