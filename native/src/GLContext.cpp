#include "../include/GLContext.h"
#ifdef ANDROID
#include <android/log.h>
#else
#include <cstdio>
#endif

namespace future2d {

GLContext::GLContext() {}

GLContext::~GLContext() { destroy(); }

bool GLContext::create(ANativeWindow* window) {
#ifdef ANDROID
    if (!window) return false;

    display_ = eglGetDisplay(EGL_DEFAULT_DISPLAY);
    if (display_ == EGL_NO_DISPLAY) return false;

    if (!eglInitialize(display_, nullptr, nullptr)) return false;

    const EGLint configAttribs[] = {
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT,
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        EGL_ALPHA_SIZE, 8,
        EGL_DEPTH_SIZE, 24,
        EGL_NONE
    };

    EGLint numConfigs;
    if (!eglChooseConfig(display_, configAttribs, &config_, 1, &numConfigs) || numConfigs == 0) {
        // fallback to GLES2
        const EGLint configAttribs2[] = {
            EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
            EGL_RED_SIZE, 8,
            EGL_GREEN_SIZE, 8,
            EGL_BLUE_SIZE, 8,
            EGL_ALPHA_SIZE, 8,
            EGL_DEPTH_SIZE, 16,
            EGL_NONE
        };
        if (!eglChooseConfig(display_, configAttribs2, &config_, 1, &numConfigs) || numConfigs == 0) {
            return false;
        }
    }

    const EGLint contextAttribs[] = { EGL_CONTEXT_CLIENT_VERSION, 3, EGL_NONE };
    context_ = eglCreateContext(display_, config_, EGL_NO_CONTEXT, contextAttribs);
    if (context_ == EGL_NO_CONTEXT) {
        // try GLES2 context
        const EGLint contextAttribs2[] = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE };
        context_ = eglCreateContext(display_, config_, EGL_NO_CONTEXT, contextAttribs2);
        if (context_ == EGL_NO_CONTEXT) return false;
    }

    surface_ = eglCreateWindowSurface(display_, config_, window, nullptr);
    if (surface_ == EGL_NO_SURFACE) return false;

    if (!eglMakeCurrent(display_, surface_, surface_, context_)) return false;

    eglQuerySurface(display_, surface_, EGL_WIDTH, &width_);
    eglQuerySurface(display_, surface_, EGL_HEIGHT, &height_);

#ifdef ANDROID
    __android_log_print(ANDROID_LOG_INFO, "Future2D", "GLContext created %dx%d", width_, height_);
#else
    std::printf("Future2D: GLContext created %dx%d\n", width_, height_);
#endif
    return true;
#else
    (void)window;
    return false;
#endif
}

void GLContext::destroy() {
#ifdef ANDROID
    if (display_ != EGL_NO_DISPLAY) {
        eglMakeCurrent(display_, EGL_NO_SURFACE, EGL_NO_SURFACE, EGL_NO_CONTEXT);
        if (context_ != EGL_NO_CONTEXT) eglDestroyContext(display_, context_);
        if (surface_ != EGL_NO_SURFACE) eglDestroySurface(display_, surface_);
        eglTerminate(display_);
    }
    display_ = EGL_NO_DISPLAY;
    context_ = EGL_NO_CONTEXT;
    surface_ = EGL_NO_SURFACE;
#else
    (void)display_;
    (void)context_;
    (void)surface_;
#endif
}

void GLContext::makeCurrent() {
#ifdef ANDROID
    if (display_ != EGL_NO_DISPLAY && surface_ != EGL_NO_SURFACE) {
        eglMakeCurrent(display_, surface_, surface_, context_);
    }
#else
    (void)display_;
    (void)surface_;
    (void)context_;
#endif
}

void GLContext::swapBuffers() {
#ifdef ANDROID
    if (display_ != EGL_NO_DISPLAY && surface_ != EGL_NO_SURFACE) {
        eglSwapBuffers(display_, surface_);
    }
#else
    (void)display_;
    (void)surface_;
#endif
}

} // namespace future2d
