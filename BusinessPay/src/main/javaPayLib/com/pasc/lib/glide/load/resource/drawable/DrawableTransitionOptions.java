package com.pasc.lib.glide.load.resource.drawable;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import com.pasc.lib.glide.TransitionOptions;
import com.pasc.lib.glide.request.transition.DrawableCrossFadeFactory;
import com.pasc.lib.glide.request.transition.TransitionFactory;

/**
 * Contains {@link Drawable} specific animation options.
 */
// Public API.
@SuppressWarnings("WeakerAccess")
public final class DrawableTransitionOptions extends
    TransitionOptions<DrawableTransitionOptions, Drawable> {

  /**
   * Returns a {@link DrawableTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade()
   */
  @NonNull
  public static DrawableTransitionOptions withCrossFade() {
    return new DrawableTransitionOptions().crossFade();
  }

  /**
   * Returns a {@link DrawableTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(int)
   */
  @NonNull
  public static DrawableTransitionOptions withCrossFade(int duration) {
    return new DrawableTransitionOptions().crossFade(duration);
  }

  /**
   * Returns a {@link DrawableTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(DrawableCrossFadeFactory)
   */
  @NonNull
  public static DrawableTransitionOptions withCrossFade(
      @NonNull DrawableCrossFadeFactory drawableCrossFadeFactory) {
    return new DrawableTransitionOptions().crossFade(drawableCrossFadeFactory);
  }

  /**
   * Returns a {@link DrawableTransitionOptions} object that enables a cross fade animation.
   *
   * @see #crossFade(DrawableCrossFadeFactory.Builder)
   */
  @NonNull
  public static DrawableTransitionOptions withCrossFade(
      @NonNull DrawableCrossFadeFactory.Builder builder) {
    return new DrawableTransitionOptions().crossFade(builder);
  }

  /**
   * Returns a {@link DrawableTransitionOptions} object that uses the given transition factory.
   *
   * @see com.pasc.lib.glide.GenericTransitionOptions#with(TransitionFactory)
   */
  @NonNull
  public static DrawableTransitionOptions with(
      @NonNull TransitionFactory<Drawable> transitionFactory) {
    return new DrawableTransitionOptions().transition(transitionFactory);
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   */
  @NonNull
  public DrawableTransitionOptions crossFade() {
    return crossFade(new DrawableCrossFadeFactory.Builder());
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   *
   * @param duration The duration of the animation, see
   *     {@code DrawableCrossFadeFactory.Builder(int)}
   * @see com.pasc.lib.glide.request.transition.DrawableCrossFadeFactory.Builder
   */
  @NonNull
  public DrawableTransitionOptions crossFade(int duration) {
    return crossFade(new DrawableCrossFadeFactory.Builder(duration));
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   */
  @NonNull
  public DrawableTransitionOptions crossFade(
      @NonNull DrawableCrossFadeFactory drawableCrossFadeFactory) {
    return transition(drawableCrossFadeFactory);
  }

  /**
   * Enables a cross fade animation between both the placeholder and the first resource and between
   * subsequent resources (if thumbnails are used).
   */
  @NonNull
  public DrawableTransitionOptions crossFade(@NonNull DrawableCrossFadeFactory.Builder builder) {
    return crossFade(builder.build());
  }
}

