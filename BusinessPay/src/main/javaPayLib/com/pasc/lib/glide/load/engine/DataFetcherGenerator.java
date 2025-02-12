package com.pasc.lib.glide.load.engine;

import android.support.annotation.Nullable;
import com.pasc.lib.glide.load.DataSource;
import com.pasc.lib.glide.load.Key;
import com.pasc.lib.glide.load.data.DataFetcher;

/**
 * Generates a series of {@link DataFetcher DataFetchers} using
 * registered {@link com.pasc.lib.glide.load.model.ModelLoader ModelLoaders} and a model.
 */
interface DataFetcherGenerator {
  /**
   * Called when the generator has finished loading data from a
   * {@link DataFetcher}.
   */
  interface FetcherReadyCallback {

    /**
     * Requests that we call startNext() again on a Glide owned thread.
     */
    void reschedule();

    /**
     * Notifies the callback that the load is complete.
     *
     * @param sourceKey The id of the loaded data.
     * @param data The loaded data, or null if the load failed.
     * @param fetcher The data fetcher we attempted to load from.
     * @param dataSource The data source we were loading from.
     * @param attemptedKey The key we were loading data from (may be an alternate).
     */
    void onDataFetcherReady(Key sourceKey, @Nullable Object data, DataFetcher<?> fetcher,
                            DataSource dataSource, Key attemptedKey);

    /**
     * Notifies the callback when the load fails.
     *
     * @param attemptedKey The key we were using to load (may be an alternate).
     * @param e The exception that caused the load to fail.
     * @param fetcher The fetcher we were loading from.
     * @param dataSource The data source we were loading from.
     */
    void onDataFetcherFailed(Key attemptedKey, Exception e, DataFetcher<?> fetcher,
                             DataSource dataSource);
  }

  /**
   * Attempts to a single new {@link DataFetcher} and returns true if
   * a {@link DataFetcher} was started, and false otherwise.
   */
  boolean startNext();

  /**
   * Attempts to cancel the currently running fetcher.
   *
   * <p> This will be called on the main thread and should complete quickly. </p>
   */
  void cancel();
}
