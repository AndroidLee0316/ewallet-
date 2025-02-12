package com.pasc.lib.glide.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import com.pasc.lib.glide.ListPreloader;
import com.pasc.lib.glide.request.target.SizeReadyCallback;
import com.pasc.lib.glide.request.target.ViewTarget;
import com.pasc.lib.glide.request.transition.Transition;
import java.util.Arrays;

/**
 * A {@link ListPreloader.PreloadSizeProvider} that will extract the preload size
 * from a given {@link View}.
 *
 * @param <T> The type of the model the size should be provided for.
 */
public class ViewPreloadSizeProvider<T> implements ListPreloader.PreloadSizeProvider<T>,
    SizeReadyCallback {
  private int[] size;
  // We need to keep a strong reference to the Target so that it isn't garbage collected due to a
  // weak reference
  // while we're waiting to get its size.
  @SuppressWarnings("unused")
  private SizeViewTarget viewTarget;

  /**
   * Constructor that does nothing by default and requires users to call {@link
   * #setView(View)} when a View is available to registerComponents the dimensions
   * returned by this class.
   */
  public ViewPreloadSizeProvider() {
    // This constructor is intentionally empty. Nothing special is needed here.
  }

  /**
   * Constructor that will extract the preload size from a given {@link View}.
   *
   * @param view A not null View the size will be extracted from async using an {@link
   *             android.view.ViewTreeObserver .OnPreDrawListener}
   */
  // Public API.
  @SuppressWarnings("WeakerAccess")
  public ViewPreloadSizeProvider(@NonNull View view) {
    viewTarget = new SizeViewTarget(view, this);
  }

  @Nullable
  @Override
  public int[] getPreloadSize(@NonNull T item, int adapterPosition, int itemPosition) {
    if (size == null) {
      return null;
    } else {
      return Arrays.copyOf(size, size.length);
    }
  }

  @Override
  public void onSizeReady(int width, int height) {
    size = new int[]{width, height};
    viewTarget = null;
  }

  /**
   * Sets the {@link View} the size will be extracted.
   *
   * <p> Note - only the first call to this method will be obeyed, subsequent requests will be
   * ignored. </p>
   *
   * @param view A not null View the size will be extracted async with an {@link
   *             android.view.ViewTreeObserver .OnPreDrawListener}
   */
  public void setView(@NonNull View view) {
    if (size != null || viewTarget != null) {
      return;
    }
    viewTarget = new SizeViewTarget(view, this);
  }

  private static final class SizeViewTarget extends ViewTarget<View, Object> {
    SizeViewTarget(@NonNull View view, @NonNull SizeReadyCallback callback) {
      super(view);
      getSize(callback);
    }

    @Override
    public void onResourceReady(@NonNull Object resource,
        @Nullable Transition<? super Object> transition) {
      // Do nothing
    }
  }
}
