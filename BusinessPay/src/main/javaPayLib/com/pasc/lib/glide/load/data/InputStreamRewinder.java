package com.pasc.lib.glide.load.data;

import android.support.annotation.NonNull;
import com.pasc.lib.glide.load.engine.bitmap_recycle.ArrayPool;
import com.pasc.lib.glide.load.resource.bitmap.RecyclableBufferedInputStream;
import com.pasc.lib.glide.util.Synthetic;
import java.io.IOException;
import java.io.InputStream;

/**
 * Implementation for {@link InputStream}s that rewinds streams by wrapping them in a buffered
 * stream.
 */
public final class InputStreamRewinder implements DataRewinder<InputStream> {
  // 5mb.
  private static final int MARK_LIMIT = 5 * 1024 * 1024;

  private final RecyclableBufferedInputStream bufferedStream;

  @Synthetic
  InputStreamRewinder(InputStream is, ArrayPool byteArrayPool) {
    bufferedStream = new RecyclableBufferedInputStream(is, byteArrayPool);
    bufferedStream.mark(MARK_LIMIT);
  }

  @NonNull
  @Override
  public InputStream rewindAndGet() throws IOException {
    bufferedStream.reset();
    return bufferedStream;
  }

  @Override
  public void cleanup() {
    bufferedStream.release();
  }

  /**
   * Factory for producing {@link InputStreamRewinder}s from {@link
   * InputStream}s.
   */
  public static final class Factory implements DataRewinder.Factory<InputStream> {
    private final ArrayPool byteArrayPool;

    public Factory(ArrayPool byteArrayPool) {
      this.byteArrayPool = byteArrayPool;
    }

    @NonNull
    @Override
    public DataRewinder<InputStream> build(InputStream data) {
      return new InputStreamRewinder(data, byteArrayPool);
    }

    @NonNull
    @Override
    public Class<InputStream> getDataClass() {
      return InputStream.class;
    }
  }
}
