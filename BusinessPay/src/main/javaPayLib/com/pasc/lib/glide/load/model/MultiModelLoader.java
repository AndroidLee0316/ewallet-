package com.pasc.lib.glide.load.model;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools.Pool;
import com.pasc.lib.glide.Priority;
import com.pasc.lib.glide.load.DataSource;
import com.pasc.lib.glide.load.Key;
import com.pasc.lib.glide.load.Options;
import com.pasc.lib.glide.load.data.DataFetcher;
import com.pasc.lib.glide.load.data.DataFetcher.DataCallback;
import com.pasc.lib.glide.load.engine.GlideException;
import com.pasc.lib.glide.util.Preconditions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Allows attempting multiple ModelLoaders registered for a given model and data class.
 *
 * <p> TODO: we should try to find a way to remove this class. It exists to allow individual
 * ModelLoaders to delegate to multiple ModelLoaders without having to duplicate this logic
 * everywhere. We have very similar logic in the {@link
 * com.pasc.lib.glide.load.engine.DataFetcherGenerator} implementations and should try to avoid this
 * duplication. </p>
 */
class MultiModelLoader<Model, Data> implements ModelLoader<Model, Data> {

  private final List<ModelLoader<Model, Data>> modelLoaders;
  private final Pool<List<Throwable>> exceptionListPool;

  MultiModelLoader(@NonNull List<ModelLoader<Model, Data>> modelLoaders,
      @NonNull Pool<List<Throwable>> exceptionListPool) {
    this.modelLoaders = modelLoaders;
    this.exceptionListPool = exceptionListPool;
  }

  @Override
  public LoadData<Data> buildLoadData(@NonNull Model model, int width, int height,
      @NonNull Options options) {
    Key sourceKey = null;
    int size = modelLoaders.size();
    List<DataFetcher<Data>> fetchers = new ArrayList<>(size);
    //noinspection ForLoopReplaceableByForEach to improve perf
    for (int i = 0; i < size; i++) {
      ModelLoader<Model, Data> modelLoader = modelLoaders.get(i);
      if (modelLoader.handles(model)) {
        LoadData<Data> loadData = modelLoader.buildLoadData(model, width, height, options);
        if (loadData != null) {
          sourceKey = loadData.sourceKey;
          fetchers.add(loadData.fetcher);
        }
      }
    }
    return !fetchers.isEmpty()
        ? new LoadData<>(sourceKey, new MultiFetcher<>(fetchers, exceptionListPool)) : null;
  }

  @Override
  public boolean handles(@NonNull Model model) {
    for (ModelLoader<Model, Data> modelLoader : modelLoaders) {
      if (modelLoader.handles(model)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return "MultiModelLoader{" + "modelLoaders=" + Arrays.toString(modelLoaders.toArray()) + '}';
  }

  static class MultiFetcher<Data> implements DataFetcher<Data>, DataCallback<Data> {

    private final List<DataFetcher<Data>> fetchers;
    private final Pool<List<Throwable>> throwableListPool;
    private int currentIndex;
    private Priority priority;
    private DataCallback<? super Data> callback;
    @Nullable
    private List<Throwable> exceptions;

    MultiFetcher(@NonNull List<DataFetcher<Data>> fetchers,
        @NonNull Pool<List<Throwable>> throwableListPool) {
      this.throwableListPool = throwableListPool;
      Preconditions.checkNotEmpty(fetchers);
      this.fetchers = fetchers;
      currentIndex = 0;
    }

    @Override
    public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Data> callback) {
      this.priority = priority;
      this.callback = callback;
      exceptions = throwableListPool.acquire();
      fetchers.get(currentIndex).loadData(priority, this);
    }

    @Override
    public void cleanup() {
      if (exceptions != null) {
        throwableListPool.release(exceptions);
      }
      exceptions = null;
      for (DataFetcher<Data> fetcher : fetchers) {
        fetcher.cleanup();
      }
    }

    @Override
    public void cancel() {
      for (DataFetcher<Data> fetcher : fetchers) {
        fetcher.cancel();
      }
    }

    @NonNull
    @Override
    public Class<Data> getDataClass() {
      return fetchers.get(0).getDataClass();
    }

    @NonNull
    @Override
    public DataSource getDataSource() {
      return fetchers.get(0).getDataSource();
    }

    @Override
    public void onDataReady(@Nullable Data data) {
      if (data != null) {
        callback.onDataReady(data);
      } else {
        startNextOrFail();
      }
    }

    @Override
    public void onLoadFailed(@NonNull Exception e) {
      Preconditions.checkNotNull(exceptions).add(e);
      startNextOrFail();
    }

    private void startNextOrFail() {
      if (currentIndex < fetchers.size() - 1) {
        currentIndex++;
        loadData(priority, callback);
      } else {
        Preconditions.checkNotNull(exceptions);
        callback.onLoadFailed(new GlideException("Fetch failed", new ArrayList<>(exceptions)));
      }
    }
  }
}
