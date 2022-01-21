//
//  OpenGLRender.m
//  opengl_texture
//
//  Created by German Saprykin on 22/4/18.
//
#import "SurfaceRenderer.h"
#import "Video360Renderer.h"


@interface SurfaceRenderer() {
  float _roll;
  float _pitch;
  float _yaw;
  int _mediaFormat;
}
@property(strong, nonatomic) EAGLContext *context;
@property(nonatomic) GLuint frameBuffer;
@property(nonatomic) GLuint depthBuffer;
@property(nonatomic) CVPixelBufferRef target;
@property(nonatomic) CVOpenGLESTextureRef texture;
@property(nonatomic) CVOpenGLESTextureCacheRef textureCache;
@property(nonatomic) CGSize surfaceSize;
@property(nonatomic, readonly) bool running;
@property(nonatomic, readonly) bool initialized;
@property(nonatomic) AVPlayerItemVideoOutput *videoOutput;
@end

@implementation SurfaceRenderer

- (instancetype)initWithVideoOutput:(AVPlayerItemVideoOutput *)videoOutput {
  self = [super init];
  if (self) {
    self.videoOutput = videoOutput;
    _running = YES;
    _disposed = NO;
    _initialized = NO;
    _mediaFormat = 0;
  }
  return self;
}

- (void)setMediaFormat:(int)mediaFormat {
  _mediaFormat = mediaFormat;
}

- (void)start {
  NSThread *thread = [[NSThread alloc] initWithTarget:self selector:@selector(run) object:nil];
  thread.name = @"OpenGLRender";
  [thread start];
}

-(void)setCameraRotationWithRoll:(float)roll pitch:(float)pitch yaw:(float)yaw {
  _roll = roll;
  _pitch=pitch;
  _yaw =yaw;
}

- (void)run {
  bool enable3D = _mediaFormat>>3==1;
  int sphericalType = _mediaFormat & 0x2>>1;
  int mediaType = (_mediaFormat & 0x1)+(_mediaFormat & 0x4);
  Mesh *mesh = [Mesh createUvSphereWithRadius:50
                                    latitudes:50
                                   longitudes:50
                           verticalFovDegrees:180
                         horizontalFovDegrees:sphericalType==0?180:360
                                  mediaFormat:mediaType];
  Video360Renderer * renderer;
  while (_running) {
    CFTimeInterval loopStart = CACurrentMediaTime();
    
    if (_videoOutput == NULL)
      continue;
    
    CMTime outputTime = [_videoOutput itemTimeForHostTime:CACurrentMediaTime()];
    if ([_videoOutput hasNewPixelBufferForItemTime:outputTime] == NO)
      continue;
    
    CVPixelBufferRef inputSource = [_videoOutput copyPixelBufferForItemTime:outputTime itemTimeForDisplay:NULL];
    if (inputSource== NULL)
      continue;
    
    // Update surface size
    CGSize inputSize = CGSizeMake(CVPixelBufferGetWidth(inputSource), CVPixelBufferGetHeight(inputSource));
    NSLog(@"Input size is %f %f",inputSize.width,inputSize.height);
    if (inputSize.width  != _surfaceSize.width || inputSize.height != _surfaceSize.height) {
      _surfaceSize = inputSize;
      [self glInit];
      if(renderer == NULL) {
        NSLog(@"Init renderer");
        renderer = [[Video360Renderer alloc]initWithContext:_context withMesh:mesh];
      }
    }
    
    if(_initialized) {
      NSLog(@"Drawing...");
      [renderer updateModelViewProjectionMatrix:_roll: _pitch :_yaw];
      [renderer updateTexture:inputSource];
      [renderer render];
      glFlush();
    }
    
    CVBufferRelease(inputSource);
    CFTimeInterval waitDelta = 0.016 - (CACurrentMediaTime() - loopStart);
    if (waitDelta > 0) {
      [NSThread sleepForTimeInterval:waitDelta];
    }
    
    // update buffer
    //        if (_target != NULL) {
    //            CVPixelBufferLockBaseAddress(inputSource,kCVPixelBufferLock_ReadOnly);
    //            CVPixelBufferLockBaseAddress(_target,0);
    //            size_t bytesPerRow = CVPixelBufferGetBytesPerRow(inputSource);
    //            uint8_t *baseAddress = (uint8_t*)CVPixelBufferGetBaseAddress(inputSource);
    //            size_t bufferHeight = CVPixelBufferGetHeight(inputSource);
    //            uint8_t *copyBaseAddress = (uint8_t*)CVPixelBufferGetBaseAddress(_target);
    //            memcpy(copyBaseAddress, baseAddress, bufferHeight * bytesPerRow);
    //            CVPixelBufferUnlockBaseAddress(inputSource,kCVPixelBufferLock_ReadOnly);
    //            CVPixelBufferUnlockBaseAddress(_target,0);
    //        }
    
  }
  NSLog(@"DeInitGL");
  [renderer dispose];
  glDeleteFramebuffers(1, &_frameBuffer);
  glDeleteFramebuffers(1, &_depthBuffer);
  CVBufferRelease(_target);
  CFRelease(_textureCache);
  CFRelease(_texture);
  _disposed = YES;
}


#pragma mark - Public

- (void)dispose {
  _running = NO;
}


#pragma mark - Private

- (void)glInit {
  NSLog(@"Initializing OpenGL Context");
  
  // Setup OpenGL Context
  _context = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES3];
  [EAGLContext setCurrentContext:_context];
  
  // Initiate output buffer
  [self initCVPixelBuffer:&_target withTexture:&_texture withSize:_surfaceSize];
  
  // Render OpenGL to texture
  glBindTexture(CVOpenGLESTextureGetTarget(_texture), CVOpenGLESTextureGetName(_texture));
  glTexImage2D(GL_TEXTURE_2D,
               0, GL_RGBA,
               _surfaceSize.width, _surfaceSize.height,
               0, GL_RGBA,
               GL_UNSIGNED_BYTE, NULL);
  
  glGenRenderbuffers(1, &_depthBuffer);
  glBindRenderbuffer(GL_RENDERBUFFER, _depthBuffer);
  glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, _surfaceSize.width, _surfaceSize.height);
  
  glGenFramebuffers(1, &_frameBuffer);
  glBindFramebuffer(GL_FRAMEBUFFER, _frameBuffer);
  glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, CVOpenGLESTextureGetName(_texture), 0);
  glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, _depthBuffer);
  
  assert(glCheckFramebufferStatus(GL_FRAMEBUFFER) == GL_FRAMEBUFFER_COMPLETE);
  
  glClearColor(0, 1, 0, 1);
  glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
  glViewport(0, 0, _surfaceSize.width, _surfaceSize.height);
  glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
  glEnable(GL_BLEND);
  
  _initialized = YES;
}

/**
 * initiate CVPixelBuffet & OpenGLTexture and associate them together
 * @param target
 * @param texture
 */
- (void)initCVPixelBuffer:(CVPixelBufferRef *)target withTexture:(CVOpenGLESTextureRef *)texture withSize:(CGSize)size {
  
  // create texture cache if not exist
  if (_textureCache == NULL) {
    CVReturn err = CVOpenGLESTextureCacheCreate(kCFAllocatorDefault, NULL, _context, NULL, &_textureCache);
    assert(err==kCVReturnSuccess);
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
  CVPixelBufferCreate(kCFAllocatorDefault, size.width, size.height, kCVPixelFormatType_32BGRA, attrs, target);
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


- (CVPixelBufferRef)copyPixelBuffer {
  CVBufferRetain(_target);
  return _target;
}

//- (cv::Mat)createCvMatWithPixelBuffer:(CVPixelBufferRef)source {
//    CVPixelBufferLockBaseAddress(source, 0);
//    void *address = CVPixelBufferGetBaseAddress(source);
//    int width = (int) CVPixelBufferGetWidth(source);
//    int height = (int) CVPixelBufferGetHeight(source);
//    
//    cv::Mat mat = cv::Mat(height, width, CV_8UC4, address, 0);
//    //cv::cvtColor(mat, _mat, CV_BGRA2BGR);
//    
//    CVPixelBufferUnlockBaseAddress(source, 0);
//    return mat;
//}
//
//- (CVImageBufferRef)createPixelBufferWithCvMat:(cv::Mat)source {
//    
//    //cv::cvtColor(mat, mat, CV_BGR2BGRA);
//    
//    int width = source.cols;
//    int height = source.rows;
//    
//    NSDictionary *options = [NSDictionary dictionaryWithObjectsAndKeys:
//                             // [NSNumber numberWithBool:YES], kCVPixelBufferCGImageCompatibilityKey,
//                             // [NSNumber numberWithBool:YES], kCVPixelBufferCGBitmapContextCompatibilityKey,
//                             [NSNumber numberWithInt:width], kCVPixelBufferWidthKey,
//                             [NSNumber numberWithInt:height], kCVPixelBufferHeightKey,
//                             nil];
//    
//    CVPixelBufferRef imageBuffer;
//    CVReturn status = CVPixelBufferCreate(kCFAllocatorMalloc, width, height, kCVPixelFormatType_32BGRA, (CFDictionaryRef) CFBridgingRetain(options), &imageBuffer);
//    
//    
//    NSParameterAssert(status == kCVReturnSuccess && imageBuffer != NULL);
//    
//    CVPixelBufferLockBaseAddress(imageBuffer, 0);
//    void *base = CVPixelBufferGetBaseAddress(imageBuffer);
//    memcpy(base, source.data, source.total() * 4);
//    CVPixelBufferUnlockBaseAddress(imageBuffer, 0);
//    
//    return imageBuffer;
//}



@end
