package com.pasc.lib.glide;

import android.content.Context;
import android.content.ContextWrapper;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.widget.ImageView;
import com.pasc.lib.glide.load.engine.Engine;
import com.pasc.lib.glide.load.engine.bitmap_recycle.ArrayPool;
import com.pasc.lib.glide.request.RequestOptions;
import com.pasc.lib.glide.request.target.ImageViewTargetFactory;
import com.pasc.lib.glide.request.target.ViewTarget;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Global context for all loads in Glide containing and exposing the various registries and classes
 * required to load resources.
 */
public class GlideContext extends ContextWrapper {
  @VisibleForTesting
  static final TransitionOptions<?, ?> DEFAULT_TRANSITION_OPTIONS =
      new GenericTransitionOptions<>();
  private final Handler mainHandler;
  private final ArrayPool arrayPool;
  private final Registry registry;
  private final ImageViewTargetFactory imageViewTargetFactory;
  private final RequestOptions defaultRequestOptions;
  private final Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions;
  private final Engine engine;
  private final int logLevel;

  public GlideContext(
      @NonNull Context context,
      @NonNull ArrayPool arrayPool,
      @NonNull Registry registry,
      @NonNull ImageViewTargetFactory imageViewTargetFactory,
      @NonNull RequestOptions defaultRequestOptions,
      @NonNull Map<Class<?>, TransitionOptions<?, ?>> defaultTransitionOptions,
      @NonNull Engine engine,
      int logLevel) {
    super(context.getApplicationContext());
    this.arrayPool = arrayPool;
    this.registry = registry;
    this.imageViewTargetFactory = imageViewTargetFactory;
    this.defaultRequestOptions = defaultRequestOptions;
    this.defaultTransitionOptions = defaultTransitionOptions;
    this.engine = engine;
    this.logLevel = logLevel;

    mainHandler = new Handler(Looper.getMainLooper());
  }

  public RequestOptions getDefaultRequestOptions() {
    return defaultRequestOptions;
  }

  @SuppressWarnings("unchecked")
  @NonNull
  public <T> TransitionOptions<?, T> getDefaultTransitionOptions(@NonNull Class<T> transcodeClass) {
    TransitionOptions<?, ?> result = defaultTransitionOptions.get(transcodeClass);
    if (result == null) {
      for (Entry<Class<?>, TransitionOptions<?, ?>> value : defaultTransitionOptions.entrySet()) {
        if (value.getKey().isAssignableFrom(transcodeClass)) {
          result = value.getValue();
        }
      }
    }
    if (result == null) {
      result = DEFAULT_TRANSITION_OPTIONS;
    }
    return (TransitionOptions<?, T>) result;
  }

  @NonNull
  public <X> ViewTarget<ImageView, X> buildImageViewTarget(
      @NonNull ImageView imageView, @NonNull Class<X> transcodeClass) {
    return imageViewTargetFactory.buildTarget(imageView, transcodeClass);
  }

  @NonNull
  public Handler getMainHandler() {
    return mainHandler;
  }

  @NonNull
  public Engine getEngine() {
    return engine;
  }

  @NonNull
  public Registry getRegistry() {
    return registry;
  }

  public int getLogLevel() {
    return logLevel;
  }

  @NonNull
  public ArrayPool getArrayPool() {
    return arrayPool;
  }
}
