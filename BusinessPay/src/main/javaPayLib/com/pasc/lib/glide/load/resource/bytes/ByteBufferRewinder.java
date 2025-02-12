package com.pasc.lib.glide.load.resource.bytes;

import android.support.annotation.NonNull;
import com.pasc.lib.glide.load.data.DataRewinder;
import java.nio.ByteBuffer;

/**
 * Rewinds {@link ByteBuffer}s.
 */
public class ByteBufferRewinder implements DataRewinder<ByteBuffer> {
  private final ByteBuffer buffer;

  // Public API.
  @SuppressWarnings("WeakerAccess")
  public ByteBufferRewinder(ByteBuffer buffer) {
    this.buffer = buffer;
  }

  @NonNull
  @Override
  public ByteBuffer rewindAndGet() {
    buffer.position(0);
    return buffer;
  }

  @Override
  public void cleanup() {
    // Do nothing.
  }

  /**
   * Factory for {@link ByteBufferRewinder}.
   */
  public static class Factory implements DataRewinder.Factory<ByteBuffer> {

    @NonNull
    @Override
    public DataRewinder<ByteBuffer> build(ByteBuffer data) {
      return new ByteBufferRewinder(data);
    }

    @NonNull
    @Override
    public Class<ByteBuffer> getDataClass() {
      return ByteBuffer.class;
    }
  }
}
