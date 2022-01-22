//
//  OpenGLRenderer.m
//  video_player
//
//  Created by Eittipat K on 22/1/2565 BE.
//

#import "OpenGLRenderer.h"


@interface OpenGLRenderer()
@property (strong, nonatomic) EAGLContext *context;
@property (strong, nonatomic) id<Renderer> renderer;
@property (nonatomic) AVPlayerItemVideoOutput *videoOutput;

@property (nonatomic) GLuint frameBuffer;
@property (nonatomic) GLuint depthBuffer;
@property (nonatomic) CVPixelBufferRef output;
@property (nonatomic) CVOpenGLESTextureCacheRef textureCache;
@property (nonatomic) CVOpenGLESTextureRef texture;
@property (nonatomic) CGSize renderSize;
@property (nonatomic) BOOL running;
@property (nonatomic) BOOL signalCreated;
@property (nonatomic) BOOL signalChanged;
@property (nonatomic) BOOL readyToDraw;
@property (nonatomic) BOOL newFrameAvailable;
@end

@implementation OpenGLRenderer


- (instancetype)initWithVideoOutput:(AVPlayerItemVideoOutput *)videoOutput {
  self = [super init];
  if (self) {
    _videoOutput = videoOutput;
    _running = YES;
    _disposed = NO;
    _signalCreated = NO;
    _signalChanged = NO;
    _readyToDraw = NO;
    _newFrameAvailable = NO;
    NSThread *thread = [[NSThread alloc] initWithTarget:self selector:@selector(run) object:nil];
    thread.name = @"OpenGLRender";
    [thread start];
  }
  return self;
}

-(void)setRenderer:(id<Renderer>)renderer {
  _renderer = renderer;
}

-(void)surfaceCreated {
  _signalCreated = YES;
}

-(void)surfaceChanged:(int)width :(int)height {
  _signalChanged = YES;
  _renderSize = CGSizeMake((float)width, (float)height);
}

-(void)surfaceDestroyed {
  _running = NO;
}

- (void)run {
  [self initGL];
  while (_running) {
    CFTimeInterval loopStart = CACurrentMediaTime();
    
    if (_videoOutput == NULL)
      continue;
    
    CMTime outputTime = [_videoOutput itemTimeForHostTime:CACurrentMediaTime()];
    _newFrameAvailable = [_videoOutput hasNewPixelBufferForItemTime:outputTime];
    
    CVPixelBufferRef inputSource = [_videoOutput copyPixelBufferForItemTime:outputTime itemTimeForDisplay:NULL];
    if (inputSource== NULL)
      continue;
    
    CGSize inputSize = CGSizeMake(CVPixelBufferGetWidth(inputSource), CVPixelBufferGetHeight(inputSource));
    if (inputSize.width  != _renderSize.width || inputSize.height != _renderSize.height) {
      NSLog(@"Detect surface changed! %d %d",(int)inputSize.width, (int)inputSize.height);
      [self surfaceChanged:inputSize.width :inputSize.height];
    }
    
    if(_signalCreated) {
      [_renderer onSurfaceCreated];
      _signalCreated = NO;
    }
    
    if(_signalChanged) {
      [self resetTextureSize];
      [_renderer onSurfaceChanged:_renderSize.width :_renderSize.height];
      _signalChanged = NO;
      _readyToDraw = YES;
    }
    
    if(_readyToDraw) {
      [_renderer updateTexture:inputSource context:_context];
      [_renderer onDrawFrame];
      glFlush();
      
    }
    
    CVBufferRelease(inputSource);
    CFTimeInterval waitDelta = 0.016 - (CACurrentMediaTime() - loopStart);
    if (waitDelta > 0) {
      [NSThread sleepForTimeInterval:waitDelta];
    }
  }
  [self deinitGL];
}



- (CVPixelBufferRef)copyPixelBuffer {
  CVBufferRetain(_output);
  return _output;
}


- (void)initGL {
  
  _context = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES3];
  [EAGLContext setCurrentContext:_context];
  
  [self resetTextureSize];
  
  glClearColor(0, 0, 0, 1);
  glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
  glViewport(0, 0, _renderSize.width, _renderSize.height);
  glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
  glEnable(GL_BLEND);
  
  NSLog(@"OpenGL init OK.");
}

- (void) resetTextureSize {
  glDeleteFramebuffers(1, &_frameBuffer);
  glDeleteFramebuffers(1, &_depthBuffer);
  
  [self createCVBufferWithSize:_renderSize withRenderTarget:&_output withTextureOut:&_texture];
  glBindTexture(CVOpenGLESTextureGetTarget(_texture), CVOpenGLESTextureGetName(_texture));
  glTexImage2D(GL_TEXTURE_2D,
               0, GL_RGBA,
               _renderSize.width, _renderSize.height,
               0, GL_RGBA,
               GL_UNSIGNED_BYTE, NULL);
  
  glGenRenderbuffers(1, &_depthBuffer);
  glBindRenderbuffer(GL_RENDERBUFFER, _depthBuffer);
  glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, _renderSize.width, _renderSize.height);
  
  glGenFramebuffers(1, &_frameBuffer);
  glBindFramebuffer(GL_FRAMEBUFFER, _frameBuffer);
  glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, CVOpenGLESTextureGetName(_texture), 0);
  glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, _depthBuffer);
  
  assert(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE);
  
}

- (void)createCVBufferWithSize:(CGSize)size
              withRenderTarget:(CVPixelBufferRef *)target
                withTextureOut:(CVOpenGLESTextureRef *)texture {
  
  if(_textureCache==NULL) {
    CVReturn result = CVOpenGLESTextureCacheCreate(kCFAllocatorDefault, nil,_context, nil, &_textureCache);
    assert(result == kCVReturnSuccess);
  }
  
  // Clean texture
  if(_texture) {
    CFRelease(_texture);
    _texture = NULL;
  }
  if(_textureCache) {
    CVOpenGLESTextureCacheFlush(_textureCache, 0);
  }
  
  CFDictionaryRef empty;
  CFMutableDictionaryRef attrs;
  empty = CFDictionaryCreate(kCFAllocatorDefault,
                             NULL,
                             NULL,
                             0,
                             &kCFTypeDictionaryKeyCallBacks,
                             &kCFTypeDictionaryValueCallBacks);
  
  attrs = CFDictionaryCreateMutable(kCFAllocatorDefault, 1,
                                    &kCFTypeDictionaryKeyCallBacks,
                                    &kCFTypeDictionaryValueCallBacks);
  
  CFDictionarySetValue(attrs, kCVPixelBufferIOSurfacePropertiesKey, empty);
  CVPixelBufferCreate(kCFAllocatorDefault, size.width, size.height,
                      kCVPixelFormatType_32BGRA, attrs, target);
  
  CVOpenGLESTextureCacheCreateTextureFromImage(kCFAllocatorDefault,
                                               _textureCache,
                                               *target,
                                               NULL, // texture attributes
                                               GL_TEXTURE_2D,
                                               GL_RGBA, // opengl format
                                               size.width,
                                               size.height,
                                               GL_BGRA, // native iOS format
                                               GL_UNSIGNED_BYTE,
                                               0,
                                               texture);
  
  CFRelease(empty);
  CFRelease(attrs);
}

- (void)deinitGL {
  glDeleteFramebuffers(1, &_frameBuffer);
  glDeleteFramebuffers(1, &_depthBuffer);
  CFRelease(_output);
  CFRelease(_texture);
  CFRelease(_textureCache);
  NSLog(@"OpenGL deinit OK.");
}


- (void)dispose {
  _running = NO;
}


@end
