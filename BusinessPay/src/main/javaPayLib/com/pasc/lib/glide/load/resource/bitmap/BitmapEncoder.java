package com.pasc.lib.glide.load.resource.bitmap;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.os.TraceCompat;
import android.util.Log;
import com.pasc.lib.glide.load.EncodeStrategy;
import com.pasc.lib.glide.load.Option;
import com.pasc.lib.glide.load.Options;
import com.pasc.lib.glide.load.ResourceEncoder;
import com.pasc.lib.glide.load.data.BufferedOutputStream;
import com.pasc.lib.glide.load.engine.Resource;
import com.pasc.lib.glide.load.engine.bitmap_recycle.ArrayPool;
import com.pasc.lib.glide.util.LogTime;
import com.pasc.lib.glide.util.Util;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An {@link com.pasc.lib.glide.load.ResourceEncoder} that writes {@link Bitmap}s
 * to {@link OutputStream}s.
 *
 * <p> {@link Bitmap}s that return true from
 * {@link Bitmap#hasAlpha ()}} are written using
 * {@link Bitmap.CompressFormat#PNG}
 * to preserve alpha and all other bitmaps are written using
 * {@link Bitmap.CompressFormat#JPEG}. </p>
 *
 * @see Bitmap#compress(Bitmap.CompressFormat, int,
 * OutputStream)
 */
public class BitmapEncoder implements ResourceEncoder<Bitmap> {
  /**
   * An integer option between 0 and 100 that is used as the compression quality.
   *
   * <p> Defaults to 90. </p>
   */
  public static final Option<Integer> COMPRESSION_QUALITY = Option.memory(
      "com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionQuality", 90);

  /**
   * An {@link Bitmap.CompressFormat} option used as the format to encode
   * the {@link Bitmap}.
   *
   * <p> Defaults to {@link Bitmap.CompressFormat#JPEG} for images without alpha
   * and {@link Bitmap.CompressFormat#PNG} for images with alpha. </p>
   */
  public static final Option<Bitmap.CompressFormat> COMPRESSION_FORMAT = Option.memory(
      "com.bumptech.glide.load.resource.bitmap.BitmapEncoder.CompressionFormat");

  private static final String TAG = "BitmapEncoder";
  @Nullable
  private final ArrayPool arrayPool;

  public BitmapEncoder(@NonNull ArrayPool arrayPool) {
    this.arrayPool = arrayPool;
  }

  /**
   * @deprecated Use {@link #BitmapEncoder(ArrayPool)} instead.
   */
  @Deprecated
  public BitmapEncoder() {
    arrayPool = null;
  }

  @Override
  public boolean encode(@NonNull Resource<Bitmap> resource, @NonNull File file,
      @NonNull Options options) {
    final Bitmap bitmap = resource.get();
    Bitmap.CompressFormat format = getFormat(bitmap, options);
    TraceCompat.beginSection(
        "encode: [" + bitmap.getWidth() + "x" + bitmap.getHeight() + "] " + format);
    try {
      long start = LogTime.getLogTime();
      int quality = options.get(COMPRESSION_QUALITY);

      boolean success = false;
      OutputStream os = null;
      try {
        os = new FileOutputStream(file);
        if (arrayPool != null) {
          os = new BufferedOutputStream(os, arrayPool);
        }
        bitmap.compress(format, quality, os);
        os.close();
        success = true;
      } catch (IOException e) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
          Log.d(TAG, "Failed to encode Bitmap", e);
        }
      } finally {
        if (os != null) {
          try {
            os.close();
          } catch (IOException e) {
            // Do nothing.
          }
        }
      }

      if (Log.isLoggable(TAG, Log.VERBOSE)) {
        Log.v(TAG, "Compressed with type: " + format + " of size " + Util.getBitmapByteSize(bitmap)
            + " in " + LogTime.getElapsedMillis(start)
            + ", options format: " + options.get(COMPRESSION_FORMAT)
            + ", hasAlpha: " + bitmap.hasAlpha());
      }
      return success;
    } finally {
      TraceCompat.endSection();
    }
  }

  private Bitmap.CompressFormat getFormat(Bitmap bitmap, Options options) {
    Bitmap.CompressFormat format = options.get(COMPRESSION_FORMAT);
    if (format != null) {
      return format;
    } else if (bitmap.hasAlpha()) {
      return Bitmap.CompressFormat.PNG;
    } else {
      return Bitmap.CompressFormat.JPEG;
    }
  }

  @NonNull
  @Override
  public EncodeStrategy getEncodeStrategy(@NonNull Options options) {
    return EncodeStrategy.TRANSFORMED;
  }
}
