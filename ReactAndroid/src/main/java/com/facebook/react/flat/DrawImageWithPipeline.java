/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.react.flat;

import javax.annotation.Nullable;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;

import com.facebook.drawee.drawable.ScalingUtils.ScaleType;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Assertions;
import com.facebook.react.views.image.ImageResizeMode;

/**
 * DrawImageWithPipeline is DrawCommand that can draw a local or remote image.
 * It uses BitmapRequestHelper internally to fetch and cache the images.
 */
/* package */ final class DrawImageWithPipeline extends AbstractDrawCommand implements DrawImage {

  private static final Paint PAINT = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

  private final Matrix mTransform = new Matrix();
  private ScaleType mScaleType = ImageResizeMode.defaultValue();
  private @Nullable BitmapRequestHelper mBitmapRequestHelper;
  private @Nullable PorterDuffColorFilter mColorFilter;
  private @Nullable FlatViewGroup.InvalidateCallback mCallback;
  private boolean mForceClip;

  @Override
  public boolean hasImageRequest() {
    return mBitmapRequestHelper != null;
  }

  @Override
  public void setImageRequest(@Nullable ImageRequest imageRequest) {
    if (imageRequest == null) {
      mBitmapRequestHelper = null;
    } else {
      mBitmapRequestHelper = new BitmapRequestHelper(imageRequest, this);
    }
  }

  @Override
  public void setTintColor(int tintColor) {
    if (tintColor == 0) {
      mColorFilter = null;
    } else {
      mColorFilter = new PorterDuffColorFilter(tintColor, PorterDuff.Mode.SRC_ATOP);
    }
  }

  @Override
  public void setScaleType(ScaleType scaleType) {
    mScaleType = scaleType;
  }

  @Override
  public ScaleType getScaleType() {
    return mScaleType;
  }

  @Override
  public void draw(Canvas canvas) {
    Bitmap bitmap = Assertions.assumeNotNull(mBitmapRequestHelper).getBitmap();
    if (bitmap == null) {
      return;
    }

    PAINT.setColorFilter(mColorFilter);
    if (mForceClip) {
      canvas.save();
      canvas.clipRect(getLeft(), getTop(), getRight(), getBottom());
      canvas.drawBitmap(bitmap, mTransform, PAINT);
      canvas.restore();
    } else {
      canvas.drawBitmap(bitmap, mTransform, PAINT);
    }
  }

  @Override
  public void onAttached(FlatViewGroup.InvalidateCallback callback) {
    mCallback = callback;
    Assertions.assumeNotNull(mBitmapRequestHelper).attach();
  }

  @Override
  public void onDetached() {
    Assertions.assumeNotNull(mBitmapRequestHelper).detach();
  }

  /* package */ void updateBounds(Bitmap bitmap) {
    Assertions.assumeNotNull(mCallback).invalidate();

    float left = getLeft();
    float top = getTop();

    float containerWidth = getRight() - left;
    float containerHeight = getBottom() - top;

    float imageWidth = (float) bitmap.getWidth();
    float imageHeight = (float) bitmap.getHeight();

    mForceClip = false;

    if (mScaleType == ScaleType.FIT_XY) {
      mTransform.setScale(containerWidth / imageWidth, containerHeight / imageHeight);
      mTransform.postTranslate(left, top);
      return;
    }

    final float scale;

    if (mScaleType == ScaleType.CENTER_INSIDE) {
      final float ratio;
      if (containerWidth >= imageWidth && containerHeight >= imageHeight) {
        scale = 1.0f;
      } else {
        scale = Math.min(containerWidth / imageWidth, containerHeight / imageHeight);
      }
    } else {
      scale = Math.max(containerWidth / imageWidth, containerHeight / imageHeight);
    }

    float paddingLeft = (containerWidth - imageWidth * scale) / 2;
    float paddingTop = (containerHeight - imageHeight * scale) / 2;

    mForceClip = paddingLeft < 0 || paddingTop < 0;

    mTransform.setScale(scale, scale);
    mTransform.postTranslate(left + paddingLeft, top + paddingTop);
  }
}
