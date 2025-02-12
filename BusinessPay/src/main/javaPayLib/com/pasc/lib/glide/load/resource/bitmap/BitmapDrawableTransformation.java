package com.pasc.lib.glide.load.resource.bitmap;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import com.pasc.lib.glide.load.Transformation;
import com.pasc.lib.glide.load.engine.Resource;
import com.pasc.lib.glide.load.engine.bitmap_recycle.BitmapPool;
import com.pasc.lib.glide.util.Preconditions;
import java.security.MessageDigest;

/**
 * Transforms {@link BitmapDrawable}s.
 *
 * @deprecated Use {@link DrawableTransformation} instead.
 */
@Deprecated
public class BitmapDrawableTransformation implements Transformation<BitmapDrawable> {

  private final Transformation<Drawable> wrapped;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public BitmapDrawableTransformation(Transformation<Bitmap> wrapped) {
    this.wrapped =
        Preconditions.checkNotNull(new DrawableTransformation(wrapped, /*isRequired=*/ false));
  }

  /**
   * @deprecated use {@link #BitmapDrawableTransformation(Transformation)}}
   */
  @Deprecated
  public BitmapDrawableTransformation(
      @SuppressWarnings("unused") Context context, Transformation<Bitmap> wrapped) {
    this(wrapped);
  }

  /**
   * @deprecated use {@link #BitmapDrawableTransformation(Transformation)}}
   */
  @Deprecated
  public BitmapDrawableTransformation(
      @SuppressWarnings("unused") Context context,
      @SuppressWarnings("unused") BitmapPool bitmapPool,
      Transformation<Bitmap> wrapped) {
    this(wrapped);
  }

  @NonNull
  @Override
  public Resource<BitmapDrawable> transform(
      @NonNull Context context, @NonNull Resource<BitmapDrawable> drawableResourceToTransform,
      int outWidth, int outHeight) {
    Resource<Drawable> toTransform = convertToDrawableResource(drawableResourceToTransform);
    Resource<Drawable> transformed = wrapped.transform(context, toTransform, outWidth, outHeight);
    return convertToBitmapDrawableResource(transformed);
  }

  @SuppressWarnings("unchecked")
  private static Resource<BitmapDrawable> convertToBitmapDrawableResource(
      Resource<Drawable> resource) {
    if (!(resource.get() instanceof BitmapDrawable)) {
      throw new IllegalArgumentException(
          "Wrapped transformation unexpectedly returned a non BitmapDrawable resource: "
              + resource.get());
    }
    return (Resource<BitmapDrawable>) (Resource<?>) resource;
  }

  @SuppressWarnings("unchecked")
  private static Resource<Drawable> convertToDrawableResource(
      Resource<BitmapDrawable> toConvert) {
    return (Resource<Drawable>) (Resource<? extends Drawable>) toConvert;
  }

  @SuppressWarnings("deprecation")
  @Override
  public boolean equals(Object o) {
    if (o instanceof BitmapDrawableTransformation) {
      BitmapDrawableTransformation other = (BitmapDrawableTransformation) o;
      return wrapped.equals(other.wrapped);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return wrapped.hashCode();
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    wrapped.updateDiskCacheKey(messageDigest);
  }
}
