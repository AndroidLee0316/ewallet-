package com.pasc.lib.glide;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import com.pasc.lib.glide.load.DecodeFormat;
import com.pasc.lib.glide.load.engine.Engine;
import com.pasc.lib.glide.load.engine.bitmap_recycle.ArrayPool;
import com.pasc.lib.glide.load.engine.bitmap_recycle.BitmapPool;
import com.pasc.lib.glide.load.engine.bitmap_recycle.BitmapPoolAdapter;
import com.pasc.lib.glide.load.engine.bitmap_recycle.LruArrayPool;
import com.pasc.lib.glide.load.engine.bitmap_recycle.LruBitmapPool;
import com.pasc.lib.glide.load.engine.cache.DiskCache;
import com.pasc.lib.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.pasc.lib.glide.load.engine.cache.LruResourceCache;
import com.pasc.lib.glide.load.engine.cache.MemoryCache;
import com.pasc.lib.glide.load.engine.cache.MemorySizeCalculator;
import com.pasc.lib.glide.load.engine.executor.GlideExecutor;
import com.pasc.lib.glide.manager.ConnectivityMonitorFactory;
import com.pasc.lib.glide.manager.DefaultConnectivityMonitorFactory;
import com.pasc.lib.glide.manager.RequestManagerRetriever;
import com.pasc.lib.glide.manager.RequestManagerRetriever.RequestManagerFactory;
import com.pasc.lib.glide.request.RequestOptions;
import com.pasc.lib.glide.request.target.Target;
import java.util.Map;

/**
 * A builder class for setting default structural classes for Glide to use.
 */
// Public API.
@SuppressWarnings({"unused", "WeakerAccess"})
public final class GlideBuilder {
  private final Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions = new ArrayMap<>();
  private Engine engine;
  private BitmapPool bitmapPool;
  private ArrayPool arrayPool;
  private MemoryCache memoryCache;
  private GlideExecutor sourceExecutor;
  private GlideExecutor diskCacheExecutor;
  private DiskCache.Factory diskCacheFactory;
  private MemorySizeCalculator memorySizeCalculator;
  private ConnectivityMonitorFactory connectivityMonitorFactory;
  private int logLevel = Log.INFO;
  private RequestOptions defaultRequestOptions = new RequestOptions();
  @Nullable
  private RequestManagerFactory requestManagerFactory;
  private GlideExecutor animationExecutor;
  private boolean isActiveResourceRetentionAllowed;

  /**
   * Sets the {@link BitmapPool} implementation to use
   * to store and retrieve reused {@link Bitmap}s.
   *
   * @param bitmapPool The pool to use.
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setBitmapPool(@Nullable BitmapPool bitmapPool) {
    this.bitmapPool = bitmapPool;
    return this;
  }

  /**
   * Sets the {@link ArrayPool} implementation to allow variable sized arrays to be stored
   * and retrieved as needed.
   *
   * @param arrayPool The pool to use.
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setArrayPool(@Nullable ArrayPool arrayPool) {
    this.arrayPool = arrayPool;
    return this;
  }

  /**
   * Sets the {@link MemoryCache} implementation to store
   * {@link com.pasc.lib.glide.load.engine.Resource}s that are not currently in use.
   *
   * @param memoryCache The cache to use.
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setMemoryCache(@Nullable MemoryCache memoryCache) {
    this.memoryCache = memoryCache;
    return this;
  }

  /**
   * Sets the {@link DiskCache} implementation to use to store
   * {@link com.pasc.lib.glide.load.engine.Resource} data and thumbnails.
   *
   * @param diskCache The disk cache to use.
   * @return This builder.
   * @deprecated Creating a disk cache directory on the main thread causes strict mode violations,
   * use {@link #setDiskCache(DiskCache.Factory)} instead.
   * Scheduled to be removed in Glide 4.0.
   */
  @Deprecated
  public GlideBuilder setDiskCache(final DiskCache diskCache) {
    return setDiskCache(new DiskCache.Factory() {
      @Override
      public DiskCache build() {
        return diskCache;
      }
    });
  }

  /**
   * Sets the {@link DiskCache.Factory} implementation to use
   * to construct the {@link DiskCache} to use to store {@link
   * com.pasc.lib.glide.load.engine.Resource} data on disk.
   *
   * @param diskCacheFactory The disk cache factory to use.
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setDiskCache(@Nullable DiskCache.Factory diskCacheFactory) {
    this.diskCacheFactory = diskCacheFactory;
    return this;
  }

  /**
   * Sets the {@link GlideExecutor} to use when retrieving
   * {@link com.pasc.lib.glide.load.engine.Resource}s that are not already in the cache.
   *
   * <p>The thread count defaults to the number of cores available on the device, with a maximum of
   * 4.
   *
   * <p>Use the {@link GlideExecutor#newSourceExecutor()} methods if you'd like to specify options
   * for the source executor.
   *
   * @param service The ExecutorService to use.
   * @return This builder.
   * @see #setDiskCacheExecutor(GlideExecutor)
   * @see GlideExecutor
   *
   * @deprecated Use {@link #setSourceExecutor(GlideExecutor)}
   */
  @Deprecated
  public GlideBuilder setResizeExecutor(@Nullable GlideExecutor service) {
    return setSourceExecutor(service);
  }

  /**
   * Sets the {@link GlideExecutor} to use when retrieving
   * {@link com.pasc.lib.glide.load.engine.Resource}s that are not already in the cache.
   *
   * <p>The thread count defaults to the number of cores available on the device, with a maximum of
   * 4.
   *
   * <p>Use the {@link GlideExecutor#newSourceExecutor()} methods if you'd like to specify options
   * for the source executor.
   *
   * @param service The ExecutorService to use.
   * @return This builder.
   * @see #setDiskCacheExecutor(GlideExecutor)
   * @see GlideExecutor
   */
  @NonNull
  public GlideBuilder setSourceExecutor(@Nullable GlideExecutor service) {
    this.sourceExecutor = service;
    return this;
  }

  /**
   * Sets the {@link GlideExecutor} to use when retrieving
   * {@link com.pasc.lib.glide.load.engine.Resource}s that are currently in Glide's disk caches.
   *
   * <p>Defaults to a single thread which is usually the best combination of memory usage,
   * jank, and performance, even on high end devices.
   *
   * <p>Use the {@link GlideExecutor#newDiskCacheExecutor()} if you'd like to specify options
   * for the disk cache executor.
   *
   * @param service The {@link GlideExecutor} to use.
   * @return This builder.
   * @see #setSourceExecutor(GlideExecutor)
   * @see GlideExecutor
   */
  @NonNull
  public GlideBuilder setDiskCacheExecutor(@Nullable GlideExecutor service) {
    this.diskCacheExecutor = service;
    return this;
  }

  /**
   * Sets the {@link GlideExecutor} to use when loading frames of animated images and particularly
   * of {@link com.pasc.lib.glide.load.resource.gif.GifDrawable}s.
   *
   * <p>Defaults to one or two threads, depending on the number of cores available.
   *
   * <p>Use the {@link GlideExecutor#newAnimationExecutor()} methods  if you'd like to specify
   * options for the animation executor.
   *
   * @param service The {@link GlideExecutor} to use.
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setAnimationExecutor(@Nullable GlideExecutor service) {
    this.animationExecutor = service;
    return this;
  }

  /**
   * Sets the default {@link RequestOptions} to use for all loads across the app.
   *
   * <p>Applying additional options with {@link
   * RequestBuilder#apply(RequestOptions)} will override defaults
   * set here.
   *
   * @param requestOptions The options to use by default.
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setDefaultRequestOptions(@Nullable RequestOptions requestOptions) {
    this.defaultRequestOptions = requestOptions;
    return this;
  }

  /**
   * Sets the default {@link TransitionOptions} to use when starting a request that will load a
   * resource with the given {@link Class}.
   *
   * <p>It's preferable but not required for the requested resource class to match the resource
   * class applied here as long as the resource class applied here is assignable from the requested
   * resource class. For example you can set a default transition for
   * {@link android.graphics.drawable.Drawable} and that default transition will be used if you
   * subsequently start requests for specific {@link android.graphics.drawable.Drawable} types like
   * {@link com.pasc.lib.glide.load.resource.gif.GifDrawable} or
   * {@link android.graphics.drawable.BitmapDrawable}. Specific types are always preferred so if you
   * register a default transition for both {@link android.graphics.drawable.Drawable} and
   * {@link android.graphics.drawable.BitmapDrawable} and then start a request for
   * {@link android.graphics.drawable.BitmapDrawable}s, the transition you registered for
   * {@link android.graphics.drawable.BitmapDrawable}s will be used.
   */
  @NonNull
  public <T> GlideBuilder setDefaultTransitionOptions(
      @NonNull Class<T> clazz, @Nullable TransitionOptions<?, T> options) {
    defaultTransitionOptions.put(clazz, options);
    return this;
  }

  /**
   * Sets the {@link DecodeFormat} that will be the default format for all
   * the default decoders that can change the {@link Bitmap.Config} of the {@link
   * Bitmap}s they decode.
   *
   * <p> Decode format is always a suggestion, not a requirement. See {@link
   * DecodeFormat} for more details. </p>
   *
   * @param decodeFormat The format to use.
   * @return This builder.
   *
   * @deprecated Use {@link #setDefaultRequestOptions(RequestOptions)} instead.
   */
  @Deprecated
  public GlideBuilder setDecodeFormat(DecodeFormat decodeFormat) {
    defaultRequestOptions = defaultRequestOptions.apply(new RequestOptions().format(decodeFormat));
    return this;
  }

  /**
   * Sets the {@link MemorySizeCalculator} to use to calculate maximum sizes for default
   * {@link MemoryCache MemoryCaches} and/or default {@link BitmapPool BitmapPools}.
   *
   * @see #setMemorySizeCalculator(MemorySizeCalculator)
   *
   * @param builder The builder to use (will not be modified).
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setMemorySizeCalculator(@NonNull MemorySizeCalculator.Builder builder) {
    return setMemorySizeCalculator(builder.build());
  }

  /**
   * Sets the {@link MemorySizeCalculator} to use to calculate maximum sizes for default
   * {@link MemoryCache MemoryCaches} and/or default {@link BitmapPool BitmapPools}.
   *
   * <p>The given {@link MemorySizeCalculator} will not affect custom pools or caches provided
   * via {@link #setBitmapPool(BitmapPool)} or {@link #setMemoryCache(MemoryCache)}.
   *
   * @param calculator The calculator to use.
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setMemorySizeCalculator(@Nullable MemorySizeCalculator calculator) {
    this.memorySizeCalculator = calculator;
    return this;
  }

  /**
   * Sets the {@link ConnectivityMonitorFactory}
   * to use to notify {@link com.pasc.lib.glide.RequestManager} of connectivity events.
   * If not set {@link DefaultConnectivityMonitorFactory} would be used.
   *
   * @param factory The factory to use
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setConnectivityMonitorFactory(@Nullable ConnectivityMonitorFactory factory) {
    this.connectivityMonitorFactory = factory;
    return this;
  }

  /**
   * Sets a log level constant from those in {@link Log} to indicate the desired log verbosity.
   *
   * <p>The level must be one of {@link Log#VERBOSE}, {@link Log#DEBUG}, {@link Log#INFO},
   * {@link Log#WARN}, or {@link Log#ERROR}.
   *
   * <p>{@link Log#VERBOSE} means one or more lines will be logged per request, including
   * timing logs and failures. {@link Log#DEBUG} means at most one line will be logged
   * per successful request, including timing logs, although many lines may be logged for
   * failures including multiple complete stack traces. {@link Log#INFO} means
   * failed loads will be logged including multiple complete stack traces, but successful loads
   * will not be logged at all. {@link Log#WARN} means only summaries of failed loads will be
   * logged. {@link Log#ERROR} means only exceptional cases will be logged.
   *
   * <p>All logs will be logged using the 'Glide' tag.
   *
   * <p>Many other debugging logs are available in individual classes. The log level supplied here
   * only controls a small set of informative and well formatted logs. Users wishing to debug
   * certain aspects of the library can look for individual <code>TAG</code> variables at the tops
   * of classes and use <code>adb shell setprop log.tag.TAG</code> to enable or disable any relevant
   * tags.
   *
   * @param logLevel The log level to use from {@link Log}.
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setLogLevel(int logLevel) {
    if (logLevel < Log.VERBOSE || logLevel > Log.ERROR) {
      throw new IllegalArgumentException("Log level must be one of Log.VERBOSE, Log.DEBUG,"
          + " Log.INFO, Log.WARN, or Log.ERROR");
    }
    this.logLevel = logLevel;
    return this;
  }

  /**
   * If set to {@code true}, allows Glide to re-capture resources that are loaded into
   * {@link Target}s which are subsequently de-referenced and
   * garbage collected without being cleared.
   *
   * <p>Defaults to {@code false}.
   *
   * <p>Glide's resource re-use system is permissive, which means that's acceptable for callers to
   * load resources into {@link Target}s and then never clear the
   * {@link Target}. To do so, Glide uses
   * {@link java.lang.ref.WeakReference}s to track resources that belong to
   * {@link Target}s that haven't yet been cleared. Setting
   * this method to {@code true} allows Glide to also maintain a hard reference to the underlying
   * resource so that if the {@link Target} is garbage collected,
   * Glide can return the underlying resource to it's memory cache so that subsequent requests will
   * not unexpectedly re-load the resource from disk or source. As a side affect, it will take
   * the system slightly longer to garbage collect the underlying resource because the weak
   * reference has to be cleared and processed before the hard reference is removed. As a result,
   * setting this method to {@code true} may transiently increase the memory usage of an
   * application.
   *
   * <p>Leaving this method at the default {@code false} value will allow the platform to garbage
   * collect resources more quickly, but will lead to unexpected memory cache misses if callers load
   * resources into {@link Target}s but never clear them.
   *
   * <p>If you set this method to {@code true} you <em>must not</em> call
   * {@link Bitmap#recycle()} or mutate any Bitmaps returned by Glide. If this method is set to
   * {@code false}, recycling or mutating Bitmaps is inefficient but safe as long as you do not
   * clear the corresponding {@link Target} used to load the
   * {@link Bitmap}. However, if you set this method to {@code true} and recycle or mutate any
   * returned {@link Bitmap}s or other mutable resources, Glide may recover those resources and
   * attempt to use them later on, resulting in crashes, graphical corruption or undefined behavior.
   *
   * <p>Regardless of what value this method is set to, it's always good practice to clear
   * {@link Target}s when you're done with the corresponding
   * resource. Clearing {@link Target}s allows Glide to maximize
   * resource re-use, minimize memory overhead and minimize unexpected behavior resulting from
   * edge cases. If you use {@link RequestManager#clear(Target)}, calling {@link Bitmap#recycle()}
   * or mutating {@link Bitmap}s is not only unsafe, it's also totally unnecessary and should be
   * avoided. In all cases, prefer {@link RequestManager#clear(Target)} to {@link Bitmap#recycle()}.
   *
   * @return This builder.
   */
  @NonNull
  public GlideBuilder setIsActiveResourceRetentionAllowed(
      boolean isActiveResourceRetentionAllowed) {
    this.isActiveResourceRetentionAllowed = isActiveResourceRetentionAllowed;
    return this;
  }

  void setRequestManagerFactory(@Nullable RequestManagerFactory factory) {
    this.requestManagerFactory = factory;
  }

  // For testing.
  GlideBuilder setEngine(Engine engine) {
    this.engine = engine;
    return this;
  }

  @NonNull
  public Glide build(@NonNull Context context) {
    if (sourceExecutor == null) {
      sourceExecutor = GlideExecutor.newSourceExecutor();
    }

    if (diskCacheExecutor == null) {
      diskCacheExecutor = GlideExecutor.newDiskCacheExecutor();
    }

    if (animationExecutor == null) {
      animationExecutor = GlideExecutor.newAnimationExecutor();
    }

    if (memorySizeCalculator == null) {
      memorySizeCalculator = new MemorySizeCalculator.Builder(context).build();
    }

    if (connectivityMonitorFactory == null) {
      connectivityMonitorFactory = new DefaultConnectivityMonitorFactory();
    }

    if (bitmapPool == null) {
      int size = memorySizeCalculator.getBitmapPoolSize();
      if (size > 0) {
        bitmapPool = new LruBitmapPool(size);
      } else {
        bitmapPool = new BitmapPoolAdapter();
      }
    }

    if (arrayPool == null) {
      arrayPool = new LruArrayPool(memorySizeCalculator.getArrayPoolSizeInBytes());
    }

    if (memoryCache == null) {
      memoryCache = new LruResourceCache(memorySizeCalculator.getMemoryCacheSize());
    }

    if (diskCacheFactory == null) {
      diskCacheFactory = new InternalCacheDiskCacheFactory(context);
    }

    if (engine == null) {
      engine =
          new Engine(
              memoryCache,
              diskCacheFactory,
              diskCacheExecutor,
              sourceExecutor,
              GlideExecutor.newUnlimitedSourceExecutor(),
              GlideExecutor.newAnimationExecutor(),
              isActiveResourceRetentionAllowed);
    }

    RequestManagerRetriever requestManagerRetriever =
        new RequestManagerRetriever(requestManagerFactory);

    return new Glide(
        context,
        engine,
        memoryCache,
        bitmapPool,
        arrayPool,
        requestManagerRetriever,
        connectivityMonitorFactory,
        logLevel,
        defaultRequestOptions.lock(),
        defaultTransitionOptions);
  }
}
