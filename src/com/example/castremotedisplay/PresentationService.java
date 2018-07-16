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

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.gms.cast.CastPresentation;
import com.google.android.gms.cast.CastRemoteDisplayLocalService;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Service to keep the remote display running even when the app goes into the background
 */
public class PresentationService extends CastRemoteDisplayLocalService {

    private static final String TAG = "PresentationService";

    // First screen
    private CastPresentation mPresentation;
    private MediaPlayer mMediaPlayer;

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
     * The presentation to show on the first screen (the TV).
     * <p>
     * Note that this display may have different metrics from the display on
     * which the main activity is showing so we must be careful to use the
     * presentation's own {@link Context} whenever we load resources.
     * </p>
     */
    private Surface mSurface;
    private Intent mStickyBroadcast;
    private ImageView mImageView;
    private TextureView mTextureView;

    private class FirstScreenPresentation extends CastPresentation {

        public FirstScreenPresentation(Context context, Display display) {
            super(context, display);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            final FrameLayout frameLayout = new FrameLayout(getContext());
            frameLayout.setBackgroundColor(Color.BLACK);
            mTextureView = new TextureView(getContext());
            frameLayout.addView(mTextureView);
            mImageView = new ImageView(getContext());
            mImageView.setImageResource(R.drawable.background);
            mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            frameLayout.addView(mImageView);

            setContentView(frameLayout);

            frameLayout.getLayoutParams().width = frameLayout.getLayoutParams().height = MATCH_PARENT;
            mImageView.getLayoutParams().width = frameLayout.getLayoutParams().height = MATCH_PARENT;
            mTextureView.getLayoutParams().height = 1024;
            mTextureView.getLayoutParams().width = 1024;

            mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
                    surface.setDefaultBufferSize(1024, 1024);
                    mSurface = new Surface(surface);

                    mStickyBroadcast = new Intent("com.samsung.mps.gvrf");
                    mStickyBroadcast.putExtra("surface", mSurface);
                    getContext().sendStickyBroadcast(mStickyBroadcast);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    mTextureView.setVisibility(View.INVISIBLE);
                    mImageView.setVisibility(View.VISIBLE);
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                    mImageView.setVisibility(View.INVISIBLE);
                    mTextureView.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        protected void onStop() {
            super.onStop();

            if (null != mStickyBroadcast) {
                getContext().removeStickyBroadcast(mStickyBroadcast);
            }
        }

    }

}
