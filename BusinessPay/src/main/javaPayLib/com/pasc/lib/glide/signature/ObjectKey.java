package com.pasc.lib.glide.signature;

import android.support.annotation.NonNull;
import com.pasc.lib.glide.load.Key;
import com.pasc.lib.glide.util.Preconditions;
import java.security.MessageDigest;

/**
 * Wraps an {@link Object}, delegating {@link #equals(Object)} and {@link #hashCode()} to
 * the wrapped Object and providing the bytes of the result of the Object's {@link #toString()}
 * method to the {@link MessageDigest} in
 * {@link #updateDiskCacheKey(MessageDigest)}.
 *
 * <p>The Object's {@link #toString()} method must be unique and suitable for use as a disk cache
 * key.</p>
 */
public final class ObjectKey implements Key {
  private final Object object;

  public ObjectKey(@NonNull Object object) {
    this.object = Preconditions.checkNotNull(object);
  }

  @Override
  public String toString() {
    return "ObjectKey{"
        + "object=" + object
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ObjectKey) {
      ObjectKey other = (ObjectKey) o;
      return object.equals(other.object);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return object.hashCode();
  }

  @Override
  public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
    messageDigest.update(object.toString().getBytes(CHARSET));
  }
}

