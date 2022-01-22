//
//  Utils.m
//  video_player
//
//  Created by Eittipat K on 20/1/2565 BE.
//

#import "Utils.h"

@interface Utils()

@end


@implementation Utils

+(void)checkGlError {
    BOOL isError = NO;
    GLenum error = glGetError();
    while(error!=GL_NO_ERROR){
        switch(error){
            case(GL_INVALID_ENUM):
                NSLog(@"glError GL_INVALID_ENUM %d",error);
                break;
            case(GL_INVALID_VALUE):
                NSLog(@"glError GL_INVALID_VALUE %d",error);
                break;
            case(GL_INVALID_OPERATION):
                NSLog(@"glError GL_INVALID_OPERATION %d",error);
                break;
            case(GL_INVALID_FRAMEBUFFER_OPERATION):
                NSLog(@"glError GL_INVALID_FRAMEBUFFER_OPERATION %d",error);
                break;
            case(GL_OUT_OF_MEMORY):
                NSLog(@"glError GL_OUT_OF_MEMORY %d",error);
                break;
            default:
                NSLog(@"glError Unknown %d",error);
                
        }
        error = glGetError();
        isError = YES;
    }
    assert(isError==NO);
}

+(GLuint)compileProgramWithVertexCode:(NSString*)vertexCode fragmentCode:(NSString*)fragmentCode {
    GLuint vertexShader = [Utils compileShader:GL_VERTEX_SHADER :vertexCode];
    GLuint fragmentShader = [Utils compileShader:GL_FRAGMENT_SHADER :fragmentCode];
    GLuint program = glCreateProgram();
    glAttachShader(program,vertexShader);
    glAttachShader(program,fragmentShader);
    glLinkProgram(program);
    GLint status = 0;
    glGetProgramiv(program, GL_LINK_STATUS, &status);
    assert(status==GL_TRUE);
    return program;
}

+(GLuint)compileShader:(GLenum)type :(NSString*)code {
    // NSLog(@"%@",code);
    GLint status = 0;
    GLuint shader = glCreateShader(type);
    const char* source = [code UTF8String];
    int length = [code length];
    glShaderSource(shader,1,&source,&length);
    glCompileShader(shader);
    glGetShaderiv(shader,GL_COMPILE_STATUS,&status);
    if(status==GL_FALSE) {
        glDeleteShader(shader);
    }
    assert(status==GL_TRUE);
    return shader;
}

@end
