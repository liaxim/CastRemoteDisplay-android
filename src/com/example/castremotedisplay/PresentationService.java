/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.castremotedisplay;

import com.google.android.gms.cast.CastPresentation;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;

import android.content.Context;
import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.TextView;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

/**
 * Service to keep the remote display running even when the app goes into the background
 */
public class PresentationService extends CastRemoteDisplayLocalService {

    private static final String TAG = "PresentationService";

    // First screen
    private CastPresentation mPresentation;
    private MediaPlayer mMediaPlayer;
    private CubeRenderer mCubeRenderer;

    @Override
    public void onCreate() {
        super.onCreate();
        // Audio
        mMediaPlayer = MediaPlayer.create(this, R.raw.sound);
        mMediaPlayer.setVolume((float) 0.1, (float) 0.1);
        mMediaPlayer.setLooping(true);
    }

    @Override
    public void onCreatePresentation(Display display) {
        createPresentation(display);
    }

    @Override
    public void onDismissPresentation() {
        dismissPresentation();
    }

    private void dismissPresentation() {
        if (mPresentation != null) {
            mMediaPlayer.stop();
            mPresentation.dismiss();
            mPresentation = null;
        }
    }

    private void createPresentation(Display display) {
        dismissPresentation();
        mPresentation = new FirstScreenPresentation(this, display);

        try {
            mPresentation.show();
            mMediaPlayer.start();
        } catch (WindowManager.InvalidDisplayException ex) {
            Log.e(TAG, "Unable to show presentation, display was removed.", ex);
            dismissPresentation();
        }
    }


    /**
     * Utility method to allow the user to change the cube color.
     */
    public void changeColor() {
        mCubeRenderer.changeColor();
    }

    /**
     * The presentation to show on the first screen (the TV).
     * <p>
     * Note that this display may have different metrics from the display on
     * which the main activity is showing so we must be careful to use the
     * presentation's own {@link Context} whenever we load resources.
     * </p>
     */
    private Surface mSurface;

    private class FirstScreenPresentation extends CastPresentation {

        private final String TAG = "FirstScreenPresentation";

        public FirstScreenPresentation(Context context, Display display) {
            super(context, display);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final TextureView textureView = new TextureView(getContext());
            setContentView(textureView);
            textureView.getLayoutParams().height = 1024;
            textureView.getLayoutParams().width = 1024;

            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
                    Log.i("mmarinov", "onSurfaceTextureAvailable");

                    mSurface = new Surface(surface);

                    Intent intent = new Intent("com.samsung.mps.gvrf");
                    intent.putExtra("surface", mSurface);
                    getContext().sendStickyBroadcast(intent);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                    Log.i("mmarinov", "onSurfaceTextureSizeChanged");
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    Log.i("mmarinov", "onSurfaceTextureDestroyed");
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    Log.i("mmarinov", "onSurfaceTextureUpdated");                    
                }
            });
        }

        /**
         * OpenGL config to enable custom anti-aliasing
         */
        private final class CustomConfigChooser implements GLSurfaceView.EGLConfigChooser {

            private int[] mValue = new int[1];
            protected int mRedSize = 8;
            protected int mGreenSize = 8;
            protected int mBlueSize = 8;
            protected int mAlphaSize = 8;
            protected int mDepthSize = 16;
            protected int mStencilSize = 0;

            @Override
            public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
                int[] configSpec = {
                        EGL10.EGL_RED_SIZE, mRedSize,
                        EGL10.EGL_GREEN_SIZE, mGreenSize,
                        EGL10.EGL_BLUE_SIZE, mBlueSize,
                        EGL10.EGL_ALPHA_SIZE, mAlphaSize,
                        EGL10.EGL_DEPTH_SIZE, mDepthSize,
                        EGL10.EGL_STENCIL_SIZE, mStencilSize,
                        EGL10.EGL_RENDERABLE_TYPE, 4,
                        EGL10.EGL_SAMPLE_BUFFERS, 1,
                        EGL10.EGL_SAMPLES, 4,
                        EGL10.EGL_NONE
                };
                int[] num_config = new int[1];
                if (!egl.eglChooseConfig(display, configSpec, null, 0, num_config)) {
                    throw new IllegalArgumentException("eglChooseConfig1 failed");
                }

                int numConfigs = num_config[0];

                if (numConfigs <= 0) {
                    // Don't do anti-aliasing
                    configSpec = new int[]{
                            EGL10.EGL_RED_SIZE, mRedSize,
                            EGL10.EGL_GREEN_SIZE, mGreenSize,
                            EGL10.EGL_BLUE_SIZE, mBlueSize,
                            EGL10.EGL_ALPHA_SIZE, mAlphaSize,
                            EGL10.EGL_DEPTH_SIZE, mDepthSize,
                            EGL10.EGL_STENCIL_SIZE, mStencilSize,
                            EGL10.EGL_RENDERABLE_TYPE, 4,
                            EGL10.EGL_NONE
                    };

                    if (!egl.eglChooseConfig(display, configSpec, null, 0, num_config)) {
                        throw new IllegalArgumentException("eglChooseConfig2 failed");
                    }
                    numConfigs = num_config[0];

                    if (numConfigs <= 0) {
                        throw new IllegalArgumentException("No configs match configSpec");
                    }
                }

                EGLConfig[] configs = new EGLConfig[numConfigs];
                if (!egl.eglChooseConfig(display, configSpec, configs, numConfigs, num_config)) {
                    throw new IllegalArgumentException("eglChooseConfig3 failed");
                }
                EGLConfig config = findConfig(egl, display, configs);
                if (config == null) {
                    throw new IllegalArgumentException("No config chosen");
                }
                return config;
            }

            private EGLConfig findConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
                for (EGLConfig config : configs) {
                    int d = findConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE, 0);
                    int s = findConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE, 0);
                    if ((d >= mDepthSize) && (s >= mStencilSize)) {
                        int r = findConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE, 0);
                        int g = findConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE, 0);
                        int b = findConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE, 0);
                        int a = findConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE, 0);
                        if ((r == mRedSize) && (g == mGreenSize) && (b == mBlueSize) && (a
                                == mAlphaSize)) {
                            return config;
                        }
                    }
                }
                return null;
            }

            private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config,
                    int attribute,
                    int defaultValue) {
                if (egl.eglGetConfigAttrib(display, config, attribute, mValue)) {
                    return mValue[0];
                }
                return defaultValue;
            }
        }
    }

}
