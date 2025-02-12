package com.pasc.lib.glide.request;

import com.pasc.lib.glide.request.target.Target;
import java.util.concurrent.Future;

/**
 * An interface for an object that is both a {@link Target} and a
 * {@link Future}. For example:
 * <pre>
 * {@code
 * FutureTarget<Bitmap> futureTarget = Glide.with(fragment)
 *                                       .load("http://goo.gl/1asf12")
 *                                       .asBitmap()
 *                                       .into(250, 250);
 * Bitmap myBitmap = futureTarget.get();
 * ... // do things with bitmap and then release when finished:
 * futureTarget.cancel(false);
 * }
 * </pre>
 *
 * <p> Note - {@link #get()} and {@link #get(long, java.util.concurrent.TimeUnit)} must be called
 * off of the main thread or they will block forever. </p>
 *
 * @param <R> The type of resource this FutureTarget will retrieve.
 */
public interface FutureTarget<R> extends Future<R>, Target<R> {
}
