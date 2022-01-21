//
//  Mesh.m
//  video_player
//
//  Created by Eittipat Kraichingrith on 20/1/2565 BE.
//

#import "Mesh.h"
#import "Utils.h"

@import OpenGLES;

#define POSITION_COORDS_PER_VERTEX 3
#define TEXTURE_COORDS_PER_VERTEX 4
#define CPV 7
#define VERTEX_STRIDE_BYTES 28
#define MEDIA_MONOSCOPIC 0
#define MEDIA_STEREO_LEFT_RIGHT 1
#define MEDIA_STEREO_TOP_BOTTOM 2

const NSString* VERTEX_SHADER_CODE =
@"#version 300 es\n"\
@"uniform mat4 uMvpMatrix;\n"\
@"in vec4 aPosition;\n"\
@"in vec2 aTexCoords;\n"\
@"out vec2 vTexCoords;\n"\
@"void main() {\n"\
@"  gl_Position = uMvpMatrix * aPosition;\n"\
@"  vTexCoords = aTexCoords;\n"\
@"}";

const NSString* FRAGMENT_SHADER_CODE =
@"#version 300 es\n"\
@"precision mediump float;\n"\
@"uniform sampler2D uTexture;\n"\
@"in vec2 vTexCoords;\n"\
@"out vec4 fragmentColor;\n"\
@"void main() {\n"\
@"  fragmentColor = texture(uTexture, vTexCoords);\n"\
@"}";

@interface Mesh()


@end

@implementation Mesh {
  float* _vertices;
  int _length;
  GLuint _program;
  GLint _mvpMatrixHandle;
  GLuint _positionHandle;
  GLuint _texCoordsHandle;
  GLuint _textureHandle;
  GLuint _vertexBuffer;
  GLuint _textureBuffer;
  GLuint _vertexArray;
}
-(instancetype)initWithVertices:(float*)vertices :(int)length {
  self = [super init];
  if (self) {
    _vertices = vertices;
    _length = length;
    _mvpMatrixHandle = 0;
    _positionHandle = 0;
    _texCoordsHandle = 0;
    _textureHandle = 0;
    _vertexBuffer = 0;
    _textureBuffer = 0;
    _vertexArray = 0;
  }
  return self;
}

+(instancetype)createUvSphereWithRadius:(float)radius
                              latitudes:(int)latitudes
                             longitudes:(int)longitudes
                     verticalFovDegrees:(float)verticalFovDegrees
                   horizontalFovDegrees:(float)horizontalFovDegrees
                            mediaFormat:(int)mediaFormat {
  
  if (radius <= 0
      || latitudes < 1 || longitudes < 1
      || verticalFovDegrees <= 0 || verticalFovDegrees > 180
      || horizontalFovDegrees <= 0 || horizontalFovDegrees > 360) {
    NSLog(@"Invalid Parameters");
    assert(NO);
  }
  
  // Compute angular size in radians of each UV quad.
  float verticalFovRads = GLKMathDegreesToRadians(verticalFovDegrees);
  float horizontalFovRads = GLKMathDegreesToRadians(horizontalFovDegrees);
  float quadHeightRads = verticalFovRads / (float)latitudes;
  float quadWidthRads = horizontalFovRads / (float)longitudes;
  
  // Each latitude strip has 2 * (longitudes quads + extra edge) vertices + 2 degenerate vertices.
  int vertexCount = (2 * (longitudes + 1) + 2) * latitudes;
  // Buffer to return.
  int length = vertexCount*CPV;
  float *vertexData = (float*)malloc(length*sizeof(float));
  
  // Generate the data for the sphere which is a set of triangle strips representing each
  // latitude band.
  int v = 0; // Index into the vertex array.
  // (i, j) represents a quad in the equirectangular sphere.
  for (int j = 0; j < latitudes; ++j) { // For each horizontal triangle strip.
    // Each latitude band lies between the two phi values. Each vertical edge on a band lies on
    // a theta value.
    float phiLow = (quadHeightRads * j - verticalFovRads / 2);
    float phiHigh = (quadHeightRads * (j + 1) - verticalFovRads / 2);
    
    for (int i = 0; i < longitudes + 1; ++i) { // For each vertical edge in the band.
      for (int k = 0; k < 2; ++k) { // For low and high points on an edge.
        // For each point, determine it's position in polar coordinates.
        float phi = (k == 0) ? phiLow : phiHigh;
        float theta = quadWidthRads * i + (float) M_PI - horizontalFovRads / 2;
        
        // Set vertex position data as Cartesian coordinates.
        vertexData[CPV * v + 0] = -(float) (radius * sin(theta) * cos(phi));
        vertexData[CPV * v + 1] = (float) (radius * sin(phi));
        vertexData[CPV * v + 2] = (float) (radius * cos(theta) * cos(phi));
        
        // Set vertex texture.x data.
        if (mediaFormat == MEDIA_STEREO_LEFT_RIGHT) {
          // For left-right media, each eye's x coordinate points to the left or right half of the
          // texture.
          vertexData[CPV * v + 3] = (i * quadWidthRads / horizontalFovRads) / 2;
          vertexData[CPV * v + 5] = (i * quadWidthRads / horizontalFovRads) / 2 + .5f;
        } else {
          // For top-bottom or monoscopic media, the eye's x spans the full width of the texture.
          vertexData[CPV * v + 3] = i * quadWidthRads / horizontalFovRads;
          vertexData[CPV * v + 5] = i * quadWidthRads / horizontalFovRads;
        }
        
        // Set vertex texture.y data. The "1 - ..." is due to Canvas vs GL coords.
        if (mediaFormat == MEDIA_STEREO_TOP_BOTTOM) {
          // For top-bottom media, each eye's y coordinate points to the top or bottom half of the
          // texture.
          vertexData[CPV * v + 4] = 1 - (((j + k) * quadHeightRads / verticalFovRads) / 2 + .5f);
          vertexData[CPV * v + 6] = 1 - ((j + k) * quadHeightRads / verticalFovRads) / 2;
        } else {
          // For left-right or monoscopic media, the eye's y spans the full height of the texture.
          vertexData[CPV * v + 4] = 1 - (j + k) * quadHeightRads / verticalFovRads;
          vertexData[CPV * v + 6] = 1 - (j + k) * quadHeightRads / verticalFovRads;
        }
        
        v++;
        
        // Break up the triangle strip with degenerate vertices by copying first and last points.
        if ((i == 0 && k == 0) || (i == longitudes && k == 1)) {
          int dstPos = CPV*v;
          int srcPos = CPV*(v-1);
          memcpy(vertexData+dstPos, vertexData+srcPos, sizeof(float)*CPV);
          v++;
        }
      }
      // Move on to the next vertical edge in the triangle strip.
    }
    // Move on to the next triangle strip.
  }
  
  return [[self alloc]initWithVertices:vertexData :length];
}

-(void)glInit {
  _program = [Utils compileProgramWithVertexCode:VERTEX_SHADER_CODE fragmentCode:FRAGMENT_SHADER_CODE];
  
  _mvpMatrixHandle = glGetUniformLocation(_program, "uMvpMatrix");
  _positionHandle = (GLuint)glGetAttribLocation(_program, "aPosition");
  _texCoordsHandle = (GLuint)glGetAttribLocation(_program, "aTexCoords");
  _textureHandle = (GLuint)glGetUniformLocation(_program, "uTexture");
  
  // Generate and bind a vertex array object
  glGenVertexArrays(1, &_vertexArray);
  glBindVertexArray(_vertexArray);
  
  // Generate and bind a vertex buffer object
  glGenBuffers(1, &_vertexBuffer);
  glBindBuffer(GL_ARRAY_BUFFER, _vertexBuffer);
  glBufferData(GL_ARRAY_BUFFER, _length*sizeof(float),_vertices, GL_STATIC_DRAW);
  
  glEnableVertexAttribArray(_positionHandle);
  glEnableVertexAttribArray(_texCoordsHandle);
  
  // Load position data
  glVertexAttribPointer(_positionHandle,POSITION_COORDS_PER_VERTEX,GL_FLOAT, GL_FALSE,VERTEX_STRIDE_BYTES, NULL);
  
  // Load texture data
  int *textureOffset = (POSITION_COORDS_PER_VERTEX+2)*4;
  glVertexAttribPointer(_texCoordsHandle,TEXTURE_COORDS_PER_VERTEX,GL_FLOAT, GL_FALSE,VERTEX_STRIDE_BYTES, textureOffset);
  
  // Unbind the vertex buffer and the vertex array object.
  glBindBuffer(GL_ARRAY_BUFFER, 0);
  glBindVertexArray(0);
  
  glDisableVertexAttribArray(_positionHandle);
  glDisableVertexAttribArray(_texCoordsHandle);
  
}

-(void)glDrawWithTexture:(CVOpenGLESTextureRef)texture mvpMatrix:(GLKMatrix4)mvpMatrix {
  glUseProgram(_program);
  glUniform1i(_textureHandle, 0);
  glUniformMatrix4fv(_mvpMatrixHandle, 1,GL_FALSE, mvpMatrix.m);
  
  glActiveTexture(GL_TEXTURE0);
  glBindTexture(CVOpenGLESTextureGetTarget(texture), CVOpenGLESTextureGetName(texture));
  
  // Render
  glBindVertexArray(_vertexArray);
  glDrawArrays(GL_TRIANGLE_STRIP, 0, _length/CPV);
  glBindVertexArray(0);
  
}

-(void)glDestroy {
  if(_program) {
    glDeleteProgram(_program);
  }
  glDeleteBuffers(1, &_vertexArray);
  glDeleteBuffers(1, &_vertexBuffer);
  free(_vertices);
}

@end
