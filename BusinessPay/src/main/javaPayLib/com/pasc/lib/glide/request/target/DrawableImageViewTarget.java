package com.pasc.lib.glide.request.target;

import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.widget.ImageView;

/**
 * A target for display {@link Drawable} objects in {@link ImageView}s.
 */
public class DrawableImageViewTarget extends ImageViewTarget<Drawable> {

  public DrawableImageViewTarget(ImageView view) {
    super(view);
  }

  /**
   * @deprecated Use {@link #waitForLayout()} instead.
   */
  // Public API.
  @SuppressWarnings({"unused", "deprecation"})
  @Deprecated
  public DrawableImageViewTarget(ImageView view, boolean waitForLayout) {
    super(view, waitForLayout);
  }

  @Override
  protected void setResource(@Nullable Drawable resource) {
    view.setImageDrawable(resource);
  }
}
