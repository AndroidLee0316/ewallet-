package com.pasc.lib.glide.manager;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.view.View;
import com.pasc.lib.glide.Glide;
import com.pasc.lib.glide.RequestManager;
import com.pasc.lib.glide.util.Preconditions;
import com.pasc.lib.glide.util.Util;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * A collection of static methods for creating new {@link com.pasc.lib.glide.RequestManager}s or
 * retrieving existing ones from activities and fragment.
 */
public class RequestManagerRetriever implements Handler.Callback {
  @VisibleForTesting
  static final String FRAGMENT_TAG = "com.pasc.lib.glide.manager";
  private static final String TAG = "RMRetriever";

  private static final int ID_REMOVE_FRAGMENT_MANAGER = 1;
  private static final int ID_REMOVE_SUPPORT_FRAGMENT_MANAGER = 2;

  // Hacks based on the implementation of FragmentManagerImpl in the non-support libraries that
  // allow us to iterate over and retrieve all active Fragments in a FragmentManager.
  private static final String FRAGMENT_INDEX_KEY = "key";

  /**
   * The top application level RequestManager.
   */
  private volatile RequestManager applicationManager;

  /**
   * Pending adds for RequestManagerFragments.
   */
  @VisibleForTesting
  final Map<android.app.FragmentManager, RequestManagerFragment> pendingRequestManagerFragments =
      new HashMap<>();

  /**
   * Pending adds for SupportRequestManagerFragments.
   */
  @VisibleForTesting
  final Map<FragmentManager, SupportRequestManagerFragment> pendingSupportRequestManagerFragments =
      new HashMap<>();

  /**
   * Main thread handler to handle cleaning up pending fragment maps.
   */
  private final Handler handler;
  private final RequestManagerFactory factory;

  // Objects used to find Fragments and Activities containing views.
  private final ArrayMap<View, Fragment> tempViewToSupportFragment = new ArrayMap<>();
  private final ArrayMap<View, android.app.Fragment> tempViewToFragment = new ArrayMap<>();
  private final Bundle tempBundle = new Bundle();

  public RequestManagerRetriever(@Nullable RequestManagerFactory factory) {
    this.factory = factory != null ? factory : DEFAULT_FACTORY;
    handler = new Handler(Looper.getMainLooper(), this /* Callback */);
  }

  @NonNull
  private RequestManager getApplicationManager(@NonNull Context context) {
    // Either an application context or we're on a background thread.
    if (applicationManager == null) {
      synchronized (this) {
        if (applicationManager == null) {
          // Normally pause/resume is taken care of by the fragment we add to the fragment or
          // activity. However, in this case since the manager attached to the application will not
          // receive lifecycle events, we must force the manager to start resumed using
          // ApplicationLifecycle.

          // TODO(b/27524013): Factor out this Glide.get() call.
          Glide glide = Glide.get(context.getApplicationContext());
          applicationManager =
              factory.build(
                  glide,
                  new ApplicationLifecycle(),
                  new EmptyRequestManagerTreeNode(),
                  context.getApplicationContext());
        }
      }
    }

    return applicationManager;
  }

  @NonNull
  public RequestManager get(@NonNull Context context) {
    if (context == null) {
      throw new IllegalArgumentException("You cannot start a load on a null Context");
    } else if (Util.isOnMainThread() && !(context instanceof Application)) {
      if (context instanceof FragmentActivity) {
        return get((FragmentActivity) context);
      } else if (context instanceof Activity) {
        return get((Activity) context);
      } else if (context instanceof ContextWrapper) {
        return get(((ContextWrapper) context).getBaseContext());
      }
    }

    return getApplicationManager(context);
  }

  @NonNull
  public RequestManager get(@NonNull FragmentActivity activity) {
    if (Util.isOnBackgroundThread()) {
      return get(activity.getApplicationContext());
    } else {
      assertNotDestroyed(activity);
      FragmentManager fm = activity.getSupportFragmentManager();
      return supportFragmentGet(activity, fm, null /*parentHint*/);
    }
  }

  @NonNull
  public RequestManager get(@NonNull Fragment fragment) {
    Preconditions.checkNotNull(fragment.getActivity(),
          "You cannot start a load on a fragment before it is attached or after it is destroyed");
    if (Util.isOnBackgroundThread()) {
      return get(fragment.getActivity().getApplicationContext());
    } else {
      FragmentManager fm = fragment.getChildFragmentManager();
      return supportFragmentGet(fragment.getActivity(), fm, fragment);
    }
  }

  @NonNull
  public RequestManager get(@NonNull Activity activity) {
    if (Util.isOnBackgroundThread()) {
      return get(activity.getApplicationContext());
    } else {
      assertNotDestroyed(activity);
      android.app.FragmentManager fm = activity.getFragmentManager();
      return fragmentGet(activity, fm, null /*parentHint*/);
    }
  }

  @NonNull
  public RequestManager get(@NonNull View view) {
    if (Util.isOnBackgroundThread()) {
      return get(view.getContext().getApplicationContext());
    }

    Preconditions.checkNotNull(view);
    Preconditions.checkNotNull(view.getContext(),
        "Unable to obtain a request manager for a view without a Context");
    Activity activity = findActivity(view.getContext());
    // The view might be somewhere else, like a service.
    if (activity == null) {
      return get(view.getContext().getApplicationContext());
    }

    // Support Fragments.
    // Although the user might have non-support Fragments attached to FragmentActivity, searching
    // for non-support Fragments is so expensive pre O and that should be rare enough that we
    // prefer to just fall back to the Activity directly.
    if (activity instanceof FragmentActivity) {
      Fragment fragment = findSupportFragment(view, (FragmentActivity) activity);
      return fragment != null ? get(fragment) : get(activity);
    }

    // Standard Fragments.
    android.app.Fragment fragment = findFragment(view, activity);
    if (fragment == null) {
      return get(activity);
    }
    return get(fragment);
  }

  private static void findAllSupportFragmentsWithViews(
      @Nullable Collection<Fragment> topLevelFragments,
      @NonNull Map<View, Fragment> result) {
    if (topLevelFragments == null) {
      return;
    }
    for (Fragment fragment : topLevelFragments) {
      // getFragment()s in the support FragmentManager may contain null values, see #1991.
      if (fragment == null || fragment.getView() == null) {
        continue;
      }
      result.put(fragment.getView(), fragment);
      findAllSupportFragmentsWithViews(fragment.getChildFragmentManager().getFragments(), result);
    }
  }

  @Nullable
  private Fragment findSupportFragment(@NonNull View target, @NonNull FragmentActivity activity) {
    tempViewToSupportFragment.clear();
    findAllSupportFragmentsWithViews(
        activity.getSupportFragmentManager().getFragments(), tempViewToSupportFragment);
    Fragment result = null;
    View activityRoot = activity.findViewById(android.R.id.content);
    View current = target;
    while (!current.equals(activityRoot)) {
      result = tempViewToSupportFragment.get(current);
      if (result != null) {
        break;
      }
      if (current.getParent() instanceof View) {
        current = (View) current.getParent();
      } else {
        break;
      }
    }

    tempViewToSupportFragment.clear();
    return result;
  }

  @Nullable
  private android.app.Fragment findFragment(@NonNull View target, @NonNull Activity activity) {
    tempViewToFragment.clear();
    findAllFragmentsWithViews(activity.getFragmentManager(), tempViewToFragment);

    android.app.Fragment result = null;

    View activityRoot = activity.findViewById(android.R.id.content);
    View current = target;
    while (!current.equals(activityRoot)) {
      result = tempViewToFragment.get(current);
      if (result != null) {
        break;
      }
      if (current.getParent() instanceof View) {
        current = (View) current.getParent();
      } else {
        break;
      }
    }
    tempViewToFragment.clear();
    return result;
  }

  // TODO: Consider using an accessor class in the support library package to more directly retrieve
  // non-support Fragments.
  @TargetApi(VERSION_CODES.O)
  private void findAllFragmentsWithViews(
      @NonNull android.app.FragmentManager fragmentManager,
      @NonNull ArrayMap<View, android.app.Fragment> result) {
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      for (android.app.Fragment fragment : fragmentManager.getFragments()) {
        if (fragment.getView() != null) {
          result.put(fragment.getView(), fragment);
          findAllFragmentsWithViews(fragment.getChildFragmentManager(), result);
        }
      }
    } else {
      findAllFragmentsWithViewsPreO(fragmentManager, result);
    }
  }

  private void findAllFragmentsWithViewsPreO(
      @NonNull android.app.FragmentManager fragmentManager,
      @NonNull ArrayMap<View, android.app.Fragment> result) {
    int index = 0;
    while (true) {
      tempBundle.putInt(FRAGMENT_INDEX_KEY, index++);
      android.app.Fragment fragment = null;
      try {
        fragment = fragmentManager.getFragment(tempBundle, FRAGMENT_INDEX_KEY);
      } catch (Exception e) {
        // This generates log spam from FragmentManager anyway.
      }
      if (fragment == null) {
        break;
      }
      if (fragment.getView() != null) {
        result.put(fragment.getView(), fragment);
        if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1) {
          findAllFragmentsWithViews(fragment.getChildFragmentManager(), result);
        }
      }
    }
  }

  @Nullable
  private Activity findActivity(@NonNull Context context) {
    if (context instanceof Activity) {
      return (Activity) context;
    } else if (context instanceof ContextWrapper) {
      return findActivity(((ContextWrapper) context).getBaseContext());
    } else {
      return null;
    }
  }

  @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
  private static void assertNotDestroyed(@NonNull Activity activity) {
    if (VERSION.SDK_INT >= VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed()) {
      throw new IllegalArgumentException("You cannot start a load for a destroyed activity");
    }
  }

  @NonNull
  @TargetApi(VERSION_CODES.JELLY_BEAN_MR1)
  public RequestManager get(@NonNull android.app.Fragment fragment) {
    if (fragment.getActivity() == null) {
      throw new IllegalArgumentException(
          "You cannot start a load on a fragment before it is attached");
    }
    if (Util.isOnBackgroundThread() || VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR1) {
      return get(fragment.getActivity().getApplicationContext());
    } else {
      android.app.FragmentManager fm = fragment.getChildFragmentManager();
      return fragmentGet(fragment.getActivity(), fm, fragment);
    }
  }

  @NonNull
  RequestManagerFragment getRequestManagerFragment(
      @NonNull final android.app.FragmentManager fm, @Nullable android.app.Fragment parentHint) {
    RequestManagerFragment current = (RequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (current == null) {
      current = pendingRequestManagerFragments.get(fm);
      if (current == null) {
        current = new RequestManagerFragment();
        current.setParentFragmentHint(parentHint);
        pendingRequestManagerFragments.put(fm, current);
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        handler.obtainMessage(ID_REMOVE_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
  }

  @NonNull
  private RequestManager fragmentGet(@NonNull Context context,
      @NonNull android.app.FragmentManager fm,
      @Nullable android.app.Fragment parentHint) {
    RequestManagerFragment current = getRequestManagerFragment(fm, parentHint);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      // TODO(b/27524013): Factor out this Glide.get() call.
      Glide glide = Glide.get(context);
      requestManager =
          factory.build(
              glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }

  @NonNull
  SupportRequestManagerFragment getSupportRequestManagerFragment(
      @NonNull final FragmentManager fm, @Nullable Fragment parentHint) {
    SupportRequestManagerFragment current =
        (SupportRequestManagerFragment) fm.findFragmentByTag(FRAGMENT_TAG);
    if (current == null) {
      current = pendingSupportRequestManagerFragments.get(fm);
      if (current == null) {
        current = new SupportRequestManagerFragment();
        current.setParentFragmentHint(parentHint);
        pendingSupportRequestManagerFragments.put(fm, current);
        fm.beginTransaction().add(current, FRAGMENT_TAG).commitAllowingStateLoss();
        handler.obtainMessage(ID_REMOVE_SUPPORT_FRAGMENT_MANAGER, fm).sendToTarget();
      }
    }
    return current;
  }

  @NonNull
  private RequestManager supportFragmentGet(@NonNull Context context, @NonNull FragmentManager fm,
      @Nullable Fragment parentHint) {
    SupportRequestManagerFragment current = getSupportRequestManagerFragment(fm, parentHint);
    RequestManager requestManager = current.getRequestManager();
    if (requestManager == null) {
      // TODO(b/27524013): Factor out this Glide.get() call.
      Glide glide = Glide.get(context);
      requestManager =
          factory.build(
              glide, current.getGlideLifecycle(), current.getRequestManagerTreeNode(), context);
      current.setRequestManager(requestManager);
    }
    return requestManager;
  }

  @Override
  public boolean handleMessage(Message message) {
    boolean handled = true;
    Object removed = null;
    Object key = null;
    switch (message.what) {
      case ID_REMOVE_FRAGMENT_MANAGER:
        android.app.FragmentManager fm = (android.app.FragmentManager) message.obj;
        key = fm;
        removed = pendingRequestManagerFragments.remove(fm);
        break;
      case ID_REMOVE_SUPPORT_FRAGMENT_MANAGER:
        FragmentManager supportFm = (FragmentManager) message.obj;
        key = supportFm;
        removed = pendingSupportRequestManagerFragments.remove(supportFm);
        break;
      default:
        handled = false;
        break;
    }
    if (handled && removed == null && Log.isLoggable(TAG, Log.WARN)) {
      Log.w(TAG, "Failed to remove expected request manager fragment, manager: " + key);
    }
    return handled;
  }

  /**
   * Used internally to create {@link RequestManager}s.
   */
  public interface RequestManagerFactory {
    @NonNull
    RequestManager build(
            @NonNull Glide glide,
            @NonNull Lifecycle lifecycle,
            @NonNull RequestManagerTreeNode requestManagerTreeNode,
            @NonNull Context context);
  }

  private static final RequestManagerFactory DEFAULT_FACTORY = new RequestManagerFactory() {
    @NonNull
    @Override
    public RequestManager build(@NonNull Glide glide, @NonNull Lifecycle lifecycle,
        @NonNull RequestManagerTreeNode requestManagerTreeNode, @NonNull Context context) {
      return new RequestManager(glide, lifecycle, requestManagerTreeNode, context);
    }
  };
}
