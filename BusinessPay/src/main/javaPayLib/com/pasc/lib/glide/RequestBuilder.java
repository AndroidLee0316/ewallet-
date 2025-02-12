package com.pasc.lib.glide;

import static com.pasc.lib.glide.request.RequestOptions.diskCacheStrategyOf;
import static com.pasc.lib.glide.request.RequestOptions.signatureOf;
import static com.pasc.lib.glide.request.RequestOptions.skipMemoryCacheOf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.CheckResult;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RawRes;
import android.widget.ImageView;
import com.pasc.lib.glide.load.Transformation;
import com.pasc.lib.glide.load.engine.DiskCacheStrategy;
import com.pasc.lib.glide.request.ErrorRequestCoordinator;
import com.pasc.lib.glide.request.FutureTarget;
import com.pasc.lib.glide.request.Request;
import com.pasc.lib.glide.request.RequestCoordinator;
import com.pasc.lib.glide.request.RequestFutureTarget;
import com.pasc.lib.glide.request.RequestListener;
import com.pasc.lib.glide.request.RequestOptions;
import com.pasc.lib.glide.request.SingleRequest;
import com.pasc.lib.glide.request.ThumbnailRequestCoordinator;
import com.pasc.lib.glide.request.target.PreloadTarget;
import com.pasc.lib.glide.request.target.Target;
import com.pasc.lib.glide.request.target.ViewTarget;
import com.pasc.lib.glide.signature.ApplicationVersionSignature;
import com.pasc.lib.glide.util.Preconditions;
import com.pasc.lib.glide.util.Synthetic;
import com.pasc.lib.glide.util.Util;
import java.io.File;
import java.net.URL;

/**
 * A generic class that can handle setting options and staring loads for generic resource types.
 *
 * @param <TranscodeType> The type of resource that will be delivered to the
 * {@link Target}.
 */
// Public API.
@SuppressWarnings({"unused", "WeakerAccess"})
public class RequestBuilder<TranscodeType> implements Cloneable,
    ModelTypes<RequestBuilder<TranscodeType>> {
  // Used in generated subclasses
  protected static final RequestOptions DOWNLOAD_ONLY_OPTIONS =
      new RequestOptions().diskCacheStrategy(DiskCacheStrategy.DATA).priority(Priority.LOW)
          .skipMemoryCache(true);

  private final Context context;
  private final RequestManager requestManager;
  private final Class<TranscodeType> transcodeClass;
  private final RequestOptions defaultRequestOptions;
  private final Glide glide;
  private final GlideContext glideContext;

  @NonNull protected RequestOptions requestOptions;

  @NonNull
  @SuppressWarnings("unchecked")
  private TransitionOptions<?, ? super TranscodeType> transitionOptions;

  @Nullable private Object model;
  // model may occasionally be null, so to enforce that load() was called, put a boolean rather
  // than relying on model not to be null.
  @Nullable private RequestListener<TranscodeType> requestListener;
  @Nullable private RequestBuilder<TranscodeType> thumbnailBuilder;
  @Nullable private RequestBuilder<TranscodeType> errorBuilder;
  @Nullable private Float thumbSizeMultiplier;
  private boolean isDefaultTransitionOptionsSet = true;
  private boolean isModelSet;
  private boolean isThumbnailBuilt;

  protected RequestBuilder(Glide glide, RequestManager requestManager,
      Class<TranscodeType> transcodeClass, Context context) {
    this.glide = glide;
    this.requestManager = requestManager;
    this.transcodeClass = transcodeClass;
    this.defaultRequestOptions = requestManager.getDefaultRequestOptions();
    this.context = context;
    this.transitionOptions = requestManager.getDefaultTransitionOptions(transcodeClass);
    this.requestOptions = defaultRequestOptions;
    this.glideContext = glide.getGlideContext();
  }

  protected RequestBuilder(Class<TranscodeType> transcodeClass, RequestBuilder<?> other) {
    this(other.glide, other.requestManager, transcodeClass, other.context);
    model = other.model;
    isModelSet = other.isModelSet;
    requestOptions = other.requestOptions;
  }

  /**
   * Applies the given options to the request, options set or unset in the given options will
   * replace those previously set in options in this class.
   *
   * @see RequestOptions#apply(RequestOptions)
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public RequestBuilder<TranscodeType> apply(@NonNull RequestOptions requestOptions) {
    Preconditions.checkNotNull(requestOptions);
    this.requestOptions = getMutableOptions().apply(requestOptions);
    return this;
  }

  // We're checking to see if we need to clone our options object because we want to make sure the
  // original is never modified, so we need reference equality.
  @SuppressWarnings("ReferenceEquality")
  @NonNull
  protected RequestOptions getMutableOptions() {
    return defaultRequestOptions == this.requestOptions
        ? this.requestOptions.clone() : this.requestOptions;
  }

  /**
   * Sets the {@link TransitionOptions} to use to transition from the placeholder or thumbnail when
   * this load completes.
   *
   * <p>The given {@link TransitionOptions} will replace any {@link TransitionOptions} set
   * previously.
   *
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  public RequestBuilder<TranscodeType> transition(
      @NonNull TransitionOptions<?, ? super TranscodeType> transitionOptions) {
    this.transitionOptions = Preconditions.checkNotNull(transitionOptions);
    isDefaultTransitionOptionsSet = false;
    return this;
  }

  /**
   * Sets a {@link RequestListener} to monitor the resource load. It's best to create a single
   * instance of an exception handler per type of request (usually activity/fragment) rather than
   * pass one in per request to avoid some redundant object allocation.
   *
   * @param requestListener The request listener to use.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  @SuppressWarnings("unchecked")
  public RequestBuilder<TranscodeType> listener(
      @Nullable RequestListener<TranscodeType> requestListener) {
    this.requestListener = requestListener;

    return this;
  }

  /**
   * Sets a {@link RequestBuilder} that is built and run iff the load started by this
   * {@link RequestBuilder} fails.
   *
   * <p>If this {@link RequestBuilder} uses a thumbnail that succeeds the given error
   * {@link RequestBuilder} will be started anyway if the non-thumbnail request fails.
   *
   * <p>Recursive calls to this method as well as calls to {@link #thumbnail(float)} and
   * {@link #thumbnail(RequestBuilder)} are supported for the given error {@link RequestBuilder}.
   *
   * <p>Unlike {@link #thumbnail(RequestBuilder)} and {@link #thumbnail(float)}, no options from
   * this primary {@link RequestBuilder} are propagated to the given error {@link RequestBuilder}.
   * Options like priority, override widths and heights and transitions must be applied
   * independently to the error builder.
   *
   * <p>The given {@link RequestBuilder} will start and potentially override a fallback drawable
   * if it's set on this {@link RequestBuilder} via
   * {@link RequestOptions#fallback(Drawable)} or
   * {@link RequestOptions#fallback(int)}.
   *
   * @return This {@link RequestBuilder}.
   */
  @NonNull
  public RequestBuilder<TranscodeType> error(@Nullable RequestBuilder<TranscodeType> errorBuilder) {
    this.errorBuilder = errorBuilder;
    return this;
  }

  /**
   * Loads and displays the resource retrieved by the given thumbnail request if it finishes before
   * this request. Best used for loading thumbnail resources that are smaller and will be loaded
   * more quickly than the full size resource. There are no guarantees about the order in which the
   * requests will actually finish. However, if the thumb request completes after the full request,
   * the thumb resource will never replace the full resource.
   *
   * <p>Recursive calls to thumbnail are supported.
   *
   * <p>Overrides any previous calls to this method, {@link #thumbnail(float)} and
   * {@link #thumbnail(RequestBuilder[])}.
   *
   * @see #thumbnail(float)
   * @see #thumbnail(RequestBuilder[])
   *
   * @param thumbnailRequest The request to use to load the thumbnail.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  @SuppressWarnings("unchecked")
  public RequestBuilder<TranscodeType> thumbnail(
      @Nullable RequestBuilder<TranscodeType> thumbnailRequest) {
    this.thumbnailBuilder = thumbnailRequest;

    return this;
  }

  /**
   * Recursively applies {@link #thumbnail(RequestBuilder)} so that the {@link RequestBuilder}s are
   * loaded as thumbnails in the given priority order.
   *
   * <p>{@link #thumbnail(RequestBuilder)} is applied in the order given so that the
   * {@link RequestBuilder} at position 0 has the {@link RequestBuilder} at position 1 applied
   * as using its thumbnail method, the {@link RequestBuilder} at position 1 has the
   * {@link RequestBuilder} at position 2 applied using its thumbnail method and so on.
   *
   * <p>Calling this method with an {@code null} array of {@link RequestBuilder} thumbnails or
   * an empty array of {@link RequestBuilder} thumbnails is equivalent to calling
   * {@link #thumbnail(RequestBuilder)} with {@code null}.
   *
   * <p>Any individual {@link RequestBuilder} in the array of thumbnails provided here may be
   * {@code null}. {@code null} {@link RequestBuilder}s are ignored and excluded from the recursive
   * chain.
   *
   * <p>The {@link RequestBuilder} objects provided here may be mutated and have any previous
   * calls to this method or {@link #thumbnail(RequestBuilder)} methods overridden.
   *
   * <p>Overrides any previous calls to {@link #thumbnail(RequestBuilder)},
   * {@link #thumbnail(float)} and this method.
   *
   * @see #thumbnail(float)
   * @see #thumbnail(RequestBuilder)
   *
   * @return This request builder.
   */
  @SuppressWarnings({"CheckResult", "unchecked"})
  @NonNull
  @CheckResult
  public RequestBuilder<TranscodeType> thumbnail(
      @Nullable RequestBuilder<TranscodeType>... thumbnails) {
    if (thumbnails == null || thumbnails.length == 0) {
      return thumbnail((RequestBuilder<TranscodeType>) null);
    }

    RequestBuilder<TranscodeType> previous = null;

    // Start with the lowest priority thumbnail so that we can safely handle mutations if
    // autoClone() is enabled by assigning the result of calling thumbnail() during the iteration.
    // Starting with the highest priority thumbnail would prevent us from assigning the result of
    // thumbnail because the mutated request wouldn't be used in the next iteration.
    for (int i = thumbnails.length - 1; i >= 0; i--) {
      RequestBuilder<TranscodeType> current = thumbnails[i];
      // Ignore null thumbnails.
      if (current == null) {
        continue;
      }

      if (previous == null) {
        // If we don't yet have our first non-null request, set it and continue.
        previous = current;
      } else {
        // Otherwise make our next lowest priority request the thumbnail of our current request.
        previous = current.thumbnail(previous);
      }
    }
    return thumbnail(previous);
  }

  /**
   * Loads a resource in an identical manner to this request except with the dimensions of the
   * target multiplied by the given size multiplier. If the thumbnail load completes before the full
   * size load, the thumbnail will be shown. If the thumbnail load completes after the full size
   * load, the thumbnail will not be shown.
   *
   * <p>Note - The thumbnail resource will be smaller than the size requested so the target (or
   * {@link ImageView}) must be able to scale the thumbnail appropriately. See
   * {@link ImageView.ScaleType}.
   *
   * <p>Almost all options will be copied from the original load, including the {@link
   * com.pasc.lib.glide.load.model.ModelLoader}, {@link com.pasc.lib.glide.load.ResourceDecoder},
   * and {@link Transformation}s. However,
   * {@link RequestOptions#placeholder(int)} and
   * {@link RequestOptions#error(int)}, and
   * {@link #listener(RequestListener)} will only be used on the full size load and will not be
   * copied for the thumbnail load.
   *
   * <p>Recursive calls to thumbnail are supported.
   *
   * <p>Overrides any previous calls to this method, {@link #thumbnail(RequestBuilder[])},
   *  and {@link #thumbnail(RequestBuilder)}.
   *
   * @see #thumbnail(RequestBuilder)
   * @see #thumbnail(RequestBuilder[])
   *
   * @param sizeMultiplier The multiplier to apply to the {@link Target}'s dimensions when loading
   *                       the thumbnail.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  @SuppressWarnings("unchecked")
  public RequestBuilder<TranscodeType> thumbnail(float sizeMultiplier) {
    if (sizeMultiplier < 0f || sizeMultiplier > 1f) {
      throw new IllegalArgumentException("sizeMultiplier must be between 0 and 1");
    }
    this.thumbSizeMultiplier = sizeMultiplier;

    return this;
  }

  /**
   * Sets the specific model to load data for.
   *
   * @param model The model to load data for, or null.
   * @return This request builder.
   */
  @NonNull
  @CheckResult
  @SuppressWarnings("unchecked")
  @Override
  public RequestBuilder<TranscodeType> load(@Nullable Object model) {
    return loadGeneric(model);
  }

  @NonNull
  private RequestBuilder<TranscodeType> loadGeneric(@Nullable Object model) {
    this.model = model;
    isModelSet = true;
    return this;
  }
  /**
   * Returns an object to load the given {@link Bitmap}.
   *
   * <p>It's almost always better to allow Glide to load {@link Bitmap}s than
   * pass {@link Bitmap}s into Glide. If you have a custom way to obtain {@link Bitmap}s that is
   * not supported by Glide by default, consider registering a custom
   * {@link com.pasc.lib.glide.load.model.ModelLoader} or
   * {@link com.pasc.lib.glide.load.ResourceDecoder} instead of using this method.
   *
   * <p>The {@link DiskCacheStrategy} is set to {@link DiskCacheStrategy#NONE}. Previous calls to
   * {@link #apply(RequestOptions)} or previously applied {@link DiskCacheStrategy}s will be
   * overridden by this method. Applying an {@link DiskCacheStrategy} other than
   * {@link DiskCacheStrategy#NONE} after calling this method may result in undefined behavior.
   *
   * <p>In memory caching relies on Object equality. The contents of the {@link Bitmap}s are not
   * compared.
   *
   * @see #load(Object)
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<TranscodeType> load(@Nullable Bitmap bitmap) {
    return loadGeneric(bitmap)
        .apply(diskCacheStrategyOf(DiskCacheStrategy.NONE));
  }

  /**
   * Returns a request builder to load the given {@link Drawable}.
   *
   * <p>It's almost always better to allow Glide to load {@link Bitmap}s than to pass
   * {@link Bitmap}s into Glide using this method . If you have a custom way to obtain
   * {@link Bitmap}s that is not supported by Glide by default, consider registering a custom
   * {@link com.pasc.lib.glide.load.model.ModelLoader} or
   * {@link com.pasc.lib.glide.load.ResourceDecoder} instead of using this method.
   *
   * <p>The {@link DiskCacheStrategy} is set to {@link DiskCacheStrategy#NONE}. Previous calls to
   * {@link #apply(RequestOptions)} or previously applied {@link DiskCacheStrategy}s will be
   * overridden by this method. Applying an {@link DiskCacheStrategy} other than
   * {@link DiskCacheStrategy#NONE} after calling this method may result in undefined behavior.
   *
   * <p>In memory caching relies on Object equality. The contents of the {@link Drawable}s are not
   * compared.
   *
   * @see #load(Object)
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<TranscodeType> load(@Nullable Drawable drawable) {
    return loadGeneric(drawable)
        .apply(diskCacheStrategyOf(DiskCacheStrategy.NONE));
  }

  /**
   * Returns a request builder to load the given {@link String}.
   *
   * <p> Note - this method caches data using only the given String as the cache key. If the data is
   * a Uri outside of your control, or you otherwise expect the data represented by the given String
   * to change without the String identifier changing, Consider using
   * {@link RequestOptions#signature(com.pasc.lib.glide.load.Key)} to
   * mixin a signature you create that identifies the data currently at the given String that will
   * invalidate the cache if that data changes. Alternatively, using
   * {@link DiskCacheStrategy#NONE} and/or
   * {@link RequestOptions#skipMemoryCache(boolean)} may be
   * appropriate.
   * </p>
   *
   * @see #load(Object)
   *
   * @param string A file path, or a uri or url handled by
   * {@link com.pasc.lib.glide.load.model.UriLoader}.
   */
  @NonNull
  @Override
  @CheckResult
  public RequestBuilder<TranscodeType> load(@Nullable String string) {
    return loadGeneric(string);
  }

  /**
   * Returns a request builder to load the given {@link Uri}.
   *
   * <p> Note - this method caches data at Uris using only the Uri itself as the cache key. The data
   * represented by Uris from some content providers may change without the Uri changing, which
   * means using this method can lead to displaying stale data. Consider using
   * {@link RequestOptions#signature(com.pasc.lib.glide.load.Key)} to
   * mixin a signature you create based on the data at the given Uri that will invalidate the cache
   * if that data changes. Alternatively, using
   * {@link DiskCacheStrategy#NONE} and/or
   * {@link RequestOptions#skipMemoryCache(boolean)} may be
   * appropriate. </p>
   *
   * @see #load(Object)
   *
   * @param uri The Uri representing the image. Must be of a type handled by
   * {@link com.pasc.lib.glide.load.model.UriLoader}.
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<TranscodeType> load(@Nullable Uri uri) {
    return loadGeneric(uri);
  }

  /**
   * Returns a request builder to load the given {@link File}.
   *
   * <p>Note - this method caches data for Files using only the file path itself as the cache key.
   * The data in the File can change so using this method can lead to displaying stale data. If you
   * expect the data in the File to change, Consider using
   * {@link RequestOptions#signature(com.pasc.lib.glide.load.Key)}
   * to mixin a signature you create that identifies the data currently in the File that will
   * invalidate the cache if that data changes. Alternatively, using
   * {@link DiskCacheStrategy#NONE} and/or
   * {@link RequestOptions#skipMemoryCache(boolean)} may be
   * appropriate.
   *
   * @see #load(Object)
   *
   * @param file The File containing the image
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<TranscodeType> load(@Nullable File file) {
    return loadGeneric(file);
  }

  /**
   * Returns a request builder that uses the
   * {@link com.pasc.lib.glide.load.model.ModelLoaderFactory} currently registered or
   * {@link Integer} to load the image represented by the given {@link Integer} resource id.
   * Defaults to {@link com.pasc.lib.glide.load.model.ResourceLoader} to load resource id models.
   *
   * <p>By default this method adds a version code based signature to the cache key used to cache
   * this resource in Glide. This signature is sufficient to guarantee that end users will see the
   * most up to date versions of your Drawables, but during development if you do not increment your
   * version code before each install and you replace a Drawable with different data without
   * changing the Drawable name, you may see inconsistent cached data. To get around this, consider
   * using {@link DiskCacheStrategy#NONE} via
   * {@link RequestOptions#diskCacheStrategy(DiskCacheStrategy)}
   * during development, and re-enabling the default
   * {@link DiskCacheStrategy#RESOURCE} for release builds.
   *
   * <p>This method will load non-{@link Bitmap} resources like
   * {@link android.graphics.drawable.VectorDrawable}s. Although Glide makes a best effort to apply
   * {@link Transformation}s to these {@link Drawable}s by either extracting
   * the underlying {@link Bitmap} or by converting the {@link Drawable} to a {@link Bitmap}, Glide
   * is still not able to transform all types of resources. Animated {@link Drawable}s cannot be
   * transformed (other than {@link com.pasc.lib.glide.load.resource.gif.GifDrawable}). To avoid
   * load failures if a {@link Drawable} can't be transformed, use the optional transformation
   * methods like {@link RequestOptions#optionalTransform(Class, Transformation)}.
   *
   * <p>In some cases converting {@link Drawable}s to {@link Bitmap}s may be inefficient. Use this
   * method, especially in conjunction with {@link Transformation}s with
   * caution for non-{@link Bitmap} {@link Drawable}s.
   *
   * @see #load(Integer)
   * @see ApplicationVersionSignature
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<TranscodeType> load(@RawRes @DrawableRes @Nullable Integer resourceId) {
    return loadGeneric(resourceId).apply(signatureOf(ApplicationVersionSignature.obtain(context)));
  }

  /**
   * Returns a request builder to load the given {@link URL}.
   *
   * @param url The URL representing the image.
   * @see #load(Object)
   * @deprecated The {@link URL} class has
   * <a href="http://goo.gl/c4hHNu">a number of performance problems</a> and should generally be
   * avoided when possible. Prefer {@link #load(Uri)} or {@link #load(String)}.
   */
  @Deprecated
  @CheckResult
  @Override
  public RequestBuilder<TranscodeType> load(@Nullable URL url) {
    return loadGeneric(url);
  }

  /**
   * Returns a request to load the given byte array.
   *
   * <p>Note - by default loads for bytes are not cached in either the memory or the disk cache.
   *
   * @param model the data to load.
   * @see #load(Object)
   */
  @NonNull
  @CheckResult
  @Override
  public RequestBuilder<TranscodeType> load(@Nullable byte[] model) {
    RequestBuilder<TranscodeType> result = loadGeneric(model);
    if (!result.requestOptions.isDiskCacheStrategySet()) {
        result = result.apply(diskCacheStrategyOf(DiskCacheStrategy.NONE));
    }
    if (!result.requestOptions.isSkipMemoryCacheSet()) {
      result = result.apply(skipMemoryCacheOf(true /*skipMemoryCache*/));
    }
    return result;
  }

  /**
   * Returns a copy of this request builder with all of the options put so far on this builder.
   *
   * <p> This method returns a "deep" copy in that all non-immutable arguments are copied such that
   * changes to one builder will not affect the other builder. However, in addition to immutable
   * arguments, the current model is not copied copied so changes to the model will affect both
   * builders. </p>
   */
  @SuppressWarnings({
      "unchecked",
      // we don't want to throw to be user friendly
      "PMD.CloneThrowsCloneNotSupportedException"
  })
  @CheckResult
  @Override
  public RequestBuilder<TranscodeType> clone() {
    try {
      RequestBuilder<TranscodeType> result = (RequestBuilder<TranscodeType>) super.clone();
      result.requestOptions = result.requestOptions.clone();
      result.transitionOptions = result.transitionOptions.clone();
      return result;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Set the target the resource will be loaded into.
   *
   * @param target The target to load the resource into.
   * @return The given target.
   * @see RequestManager#clear(Target)
   */
  @NonNull
  public <Y extends Target<TranscodeType>> Y into(@NonNull Y target) {
    return into(target, /*targetListener=*/ null);
  }

  @NonNull
  @Synthetic <Y extends Target<TranscodeType>> Y into(
      @NonNull Y target,
      @Nullable RequestListener<TranscodeType> targetListener) {
    return into(target, targetListener, getMutableOptions());
  }

  private <Y extends Target<TranscodeType>> Y into(
      @NonNull Y target,
      @Nullable RequestListener<TranscodeType> targetListener,
      @NonNull RequestOptions options) {
    Util.assertMainThread();
    Preconditions.checkNotNull(target);
    if (!isModelSet) {
      throw new IllegalArgumentException("You must call #load() before calling #into()");
    }

    options = options.autoClone();
    Request request = buildRequest(target, targetListener, options);

    Request previous = target.getRequest();
    if (request.isEquivalentTo(previous)
        && !isSkipMemoryCacheWithCompletePreviousRequest(options, previous)) {
      request.recycle();
      // If the request is completed, beginning again will ensure the result is re-delivered,
      // triggering RequestListeners and Targets. If the request is failed, beginning again will
      // restart the request, giving it another chance to complete. If the request is already
      // running, we can let it continue running without interruption.
      if (!Preconditions.checkNotNull(previous).isRunning()) {
        // Use the previous request rather than the new one to allow for optimizations like skipping
        // setting placeholders, tracking and un-tracking Targets, and obtaining View dimensions
        // that are done in the individual Request.
        previous.begin();
      }
      return target;
    }

    requestManager.clear(target);
    target.setRequest(request);
    requestManager.track(target, request);

    return target;
  }

  // If the caller is using skipMemoryCache and the previous request is finished, calling begin on
  // the previous request will complete from memory because it will just use the resource that had
  // already been loaded. If the previous request isn't complete, we can wait for it to finish
  // because the previous request must also be using skipMemoryCache for the requests to be
  // equivalent. See #2663 for additional context.
  private boolean isSkipMemoryCacheWithCompletePreviousRequest(
      RequestOptions options, Request previous) {
    return !options.isMemoryCacheable() && previous.isComplete();
  }

  /**
   * Sets the {@link ImageView} the resource will be loaded into, cancels any existing loads into
   * the view, and frees any resources Glide may have previously loaded into the view so they may be
   * reused.
   *
   * @see RequestManager#clear(Target)
   *
   * @param view The view to cancel previous loads for and load the new resource into.
   * @return The
   * {@link Target} used to wrap the given {@link ImageView}.
   */
  @NonNull
  public ViewTarget<ImageView, TranscodeType> into(@NonNull ImageView view) {
    Util.assertMainThread();
    Preconditions.checkNotNull(view);

    RequestOptions requestOptions = this.requestOptions;
    if (!requestOptions.isTransformationSet()
        && requestOptions.isTransformationAllowed()
        && view.getScaleType() != null) {
      // Clone in this method so that if we use this RequestBuilder to load into a View and then
      // into a different target, we don't retain the transformation applied based on the previous
      // View's scale type.
      switch (view.getScaleType()) {
        case CENTER_CROP:
          requestOptions = requestOptions.clone().optionalCenterCrop();
          break;
        case CENTER_INSIDE:
          requestOptions = requestOptions.clone().optionalCenterInside();
          break;
        case FIT_CENTER:
        case FIT_START:
        case FIT_END:
          requestOptions = requestOptions.clone().optionalFitCenter();
          break;
        case FIT_XY:
          requestOptions = requestOptions.clone().optionalCenterInside();
          break;
        case CENTER:
        case MATRIX:
        default:
          // Do nothing.
      }
    }

    return into(
        glideContext.buildImageViewTarget(view, transcodeClass),
        /*targetListener=*/ null,
        requestOptions);
  }

  /**
   * Returns a future that can be used to do a blocking get on a background thread.
   *
   * @param width  The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link RequestOptions#override(int, int)} if
   *               previously called.
   * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link RequestOptions#override(int, int)}} if
   *               previously called).
   * @see RequestManager#clear(Target)
   *
   * @deprecated Use {@link #submit(int, int)} instead.
   */
  @Deprecated
  public FutureTarget<TranscodeType> into(int width, int height) {
    return submit(width, height);
  }

  /**
   * Returns a future that can be used to do a blocking get on a background thread.
   *
   * <p>This method defaults to {@link Target#SIZE_ORIGINAL} for the width and the height. However,
   * since the width and height will be overridden by values passed to {@link
   * RequestOptions#override(int, int)}, this method can be used whenever {@link RequestOptions}
   * with override values are applied, or whenever you want to retrieve the image in its original
   * size.
   *
   * @see #submit(int, int)
   * @see #into(Target)
   */
  @NonNull
  public FutureTarget<TranscodeType> submit() {
    return submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  /**
   * Returns a future that can be used to do a blocking get on a background thread.
   *
   * @param width  The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link RequestOptions#override(int, int)} if
   *               previously called.
   * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link RequestOptions#override(int, int)}} if
   *               previously called).
   */
  @NonNull
  public FutureTarget<TranscodeType> submit(int width, int height) {
    final RequestFutureTarget<TranscodeType> target =
        new RequestFutureTarget<>(glideContext.getMainHandler(), width, height);

    if (Util.isOnBackgroundThread()) {
      glideContext.getMainHandler().post(new Runnable() {
        @Override
        public void run() {
          if (!target.isCancelled()) {
            into(target, target);
          }
        }
      });
    } else {
      into(target, target);
    }

    return target;
  }

  /**
   * Preloads the resource into the cache using the given width and height.
   *
   * <p> Pre-loading is useful for making sure that resources you are going to to want in the near
   * future are available quickly. </p>
   *
   * @param width  The desired width in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link RequestOptions#override(int, int)} if
   *               previously called.
   * @param height The desired height in pixels, or {@link Target#SIZE_ORIGINAL}. This will be
   *               overridden by
   *               {@link RequestOptions#override(int, int)}} if
   *               previously called).
   * @return A {@link Target} that can be used to cancel the load via
   * {@link RequestManager#clear(Target)}.
   * @see ListPreloader
   */
  @NonNull
  public Target<TranscodeType> preload(int width, int height) {
    final PreloadTarget<TranscodeType> target = PreloadTarget.obtain(requestManager, width, height);
    return into(target);
  }

  /**
   * Preloads the resource into the cache using {@link Target#SIZE_ORIGINAL} as the target width and
   * height. Equivalent to calling {@link #preload(int, int)} with {@link Target#SIZE_ORIGINAL} as
   * the width and height.
   *
   * @return A {@link Target} that can be used to cancel the load via
   * {@link RequestManager#clear(Target)}
   * @see #preload(int, int)
   */
  @NonNull
  public Target<TranscodeType> preload() {
    return preload(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL);
  }

  /**
   * Loads the original unmodified data into the cache and calls the given Target with the cache
   * File.
   *
   * @param target The Target that will receive the cache File when the load completes
   * @param <Y>    The type of Target.
   * @return The given Target.
   *
   * @deprecated Use {@link RequestManager#downloadOnly()} and {@link #into(Target)}.
   */
  @Deprecated
  @CheckResult
  public <Y extends Target<File>> Y downloadOnly(@NonNull Y target) {
    return getDownloadOnlyRequest().into(target);
  }

  /**
   * Loads the original unmodified data into the cache and returns a
   * {@link java.util.concurrent.Future} that can be used to retrieve the cache File containing the
   * data.
   *
   * @param width  The width in pixels to use to fetch the data.
   * @param height The height in pixels to use to fetch the data.
   * @return A {@link java.util.concurrent.Future} that can be used to retrieve the cache File
   * containing the data.
   *
   * @deprecated Use {@link RequestManager#downloadOnly()} and {@link #into(int, int)}.
   */
  @Deprecated
  @CheckResult
  public FutureTarget<File> downloadOnly(int width, int height) {
    return getDownloadOnlyRequest().submit(width, height);
  }

  @NonNull
  @CheckResult
  protected RequestBuilder<File> getDownloadOnlyRequest() {
    return new RequestBuilder<>(File.class, this).apply(DOWNLOAD_ONLY_OPTIONS);
  }

  @NonNull
  private Priority getThumbnailPriority(@NonNull Priority current) {
    switch (current) {
      case LOW:
        return Priority.NORMAL;
      case NORMAL:
        return Priority.HIGH;
      case HIGH:
      case IMMEDIATE:
        return Priority.IMMEDIATE;
      default:
        throw new IllegalArgumentException("unknown priority: " + requestOptions.getPriority());
    }
  }

  private Request buildRequest(
      Target<TranscodeType> target,
      @Nullable RequestListener<TranscodeType> targetListener,
      RequestOptions requestOptions) {
    return buildRequestRecursive(
        target,
        targetListener,
        /*parentCoordinator=*/ null,
        transitionOptions,
        requestOptions.getPriority(),
        requestOptions.getOverrideWidth(),
        requestOptions.getOverrideHeight(),
        requestOptions);
  }

  private Request buildRequestRecursive(
      Target<TranscodeType> target,
      @Nullable RequestListener<TranscodeType> targetListener,
      @Nullable RequestCoordinator parentCoordinator,
      TransitionOptions<?, ? super TranscodeType> transitionOptions,
      Priority priority,
      int overrideWidth,
      int overrideHeight,
      RequestOptions requestOptions) {

    // Build the ErrorRequestCoordinator first if necessary so we can update parentCoordinator.
    ErrorRequestCoordinator errorRequestCoordinator = null;
    if (errorBuilder != null) {
      errorRequestCoordinator = new ErrorRequestCoordinator(parentCoordinator);
      parentCoordinator = errorRequestCoordinator;
    }

    Request mainRequest =
        buildThumbnailRequestRecursive(
            target,
            targetListener,
            parentCoordinator,
            transitionOptions,
            priority,
            overrideWidth,
            overrideHeight,
            requestOptions);

    if (errorRequestCoordinator == null) {
      return mainRequest;
    }

    int errorOverrideWidth = errorBuilder.requestOptions.getOverrideWidth();
    int errorOverrideHeight = errorBuilder.requestOptions.getOverrideHeight();
    if (Util.isValidDimensions(overrideWidth, overrideHeight)
        && !errorBuilder.requestOptions.isValidOverride()) {
      errorOverrideWidth = requestOptions.getOverrideWidth();
      errorOverrideHeight = requestOptions.getOverrideHeight();
    }

    Request errorRequest = errorBuilder.buildRequestRecursive(
        target,
        targetListener,
        errorRequestCoordinator,
        errorBuilder.transitionOptions,
        errorBuilder.requestOptions.getPriority(),
        errorOverrideWidth,
        errorOverrideHeight,
        errorBuilder.requestOptions);
    errorRequestCoordinator.setRequests(mainRequest, errorRequest);
    return errorRequestCoordinator;
  }

  private Request buildThumbnailRequestRecursive(
      Target<TranscodeType> target,
      RequestListener<TranscodeType> targetListener,
      @Nullable RequestCoordinator parentCoordinator,
      TransitionOptions<?, ? super TranscodeType> transitionOptions,
      Priority priority,
      int overrideWidth,
      int overrideHeight,
      RequestOptions requestOptions) {
    if (thumbnailBuilder != null) {
      // Recursive case: contains a potentially recursive thumbnail request builder.
      if (isThumbnailBuilt) {
        throw new IllegalStateException("You cannot use a request as both the main request and a "
            + "thumbnail, consider using clone() on the request(s) passed to thumbnail()");
      }

      TransitionOptions<?, ? super TranscodeType> thumbTransitionOptions =
          thumbnailBuilder.transitionOptions;

      // Apply our transition by default to thumbnail requests but avoid overriding custom options
      // that may have been applied on the thumbnail request explicitly.
      if (thumbnailBuilder.isDefaultTransitionOptionsSet) {
        thumbTransitionOptions = transitionOptions;
      }

      Priority thumbPriority = thumbnailBuilder.requestOptions.isPrioritySet()
          ? thumbnailBuilder.requestOptions.getPriority() : getThumbnailPriority(priority);

      int thumbOverrideWidth = thumbnailBuilder.requestOptions.getOverrideWidth();
      int thumbOverrideHeight = thumbnailBuilder.requestOptions.getOverrideHeight();
      if (Util.isValidDimensions(overrideWidth, overrideHeight)
          && !thumbnailBuilder.requestOptions.isValidOverride()) {
        thumbOverrideWidth = requestOptions.getOverrideWidth();
        thumbOverrideHeight = requestOptions.getOverrideHeight();
      }

      ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
      Request fullRequest =
          obtainRequest(
              target,
              targetListener,
              requestOptions,
              coordinator,
              transitionOptions,
              priority,
              overrideWidth,
              overrideHeight);
      isThumbnailBuilt = true;
      // Recursively generate thumbnail requests.
      Request thumbRequest =
          thumbnailBuilder.buildRequestRecursive(
              target,
              targetListener,
              coordinator,
              thumbTransitionOptions,
              thumbPriority,
              thumbOverrideWidth,
              thumbOverrideHeight,
              thumbnailBuilder.requestOptions);
      isThumbnailBuilt = false;
      coordinator.setRequests(fullRequest, thumbRequest);
      return coordinator;
    } else if (thumbSizeMultiplier != null) {
      // Base case: thumbnail multiplier generates a thumbnail request, but cannot recurse.
      ThumbnailRequestCoordinator coordinator = new ThumbnailRequestCoordinator(parentCoordinator);
      Request fullRequest =
          obtainRequest(
              target,
              targetListener,
              requestOptions,
              coordinator,
              transitionOptions,
              priority,
              overrideWidth,
              overrideHeight);
      RequestOptions thumbnailOptions = requestOptions.clone()
          .sizeMultiplier(thumbSizeMultiplier);

      Request thumbnailRequest =
          obtainRequest(
              target,
              targetListener,
              thumbnailOptions,
              coordinator,
              transitionOptions,
              getThumbnailPriority(priority),
              overrideWidth,
              overrideHeight);

      coordinator.setRequests(fullRequest, thumbnailRequest);
      return coordinator;
    } else {
      // Base case: no thumbnail.
      return obtainRequest(
          target,
          targetListener,
          requestOptions,
          parentCoordinator,
          transitionOptions,
          priority,
          overrideWidth,
          overrideHeight);
    }
  }

  private Request obtainRequest(
      Target<TranscodeType> target,
      RequestListener<TranscodeType> targetListener,
      RequestOptions requestOptions,
      RequestCoordinator requestCoordinator,
      TransitionOptions<?, ? super TranscodeType> transitionOptions,
      Priority priority,
      int overrideWidth,
      int overrideHeight) {
    return SingleRequest.obtain(
        context,
        glideContext,
        model,
        transcodeClass,
        requestOptions,
        overrideWidth,
        overrideHeight,
        priority,
        target,
        targetListener,
        requestListener,
        requestCoordinator,
        glideContext.getEngine(),
        transitionOptions.getTransitionFactory());
  }
}
