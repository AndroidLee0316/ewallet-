package com.pasc.lib.glide.manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import com.pasc.lib.glide.Glide;
import com.pasc.lib.glide.RequestManager;
import com.pasc.lib.glide.util.Synthetic;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * A view-less {@link Fragment} used to safely store an {@link
 * com.pasc.lib.glide.RequestManager} that can be used to start, retry and manage Glide requests
 * started for targets within the fragment or activity this fragment is a child of.
 *
 * @see RequestManagerFragment
 * @see RequestManagerRetriever
 * @see com.pasc.lib.glide.RequestManager
 */
public class SupportRequestManagerFragment extends Fragment {
  private static final String TAG = "SupportRMFragment";
  private final ActivityFragmentLifecycle lifecycle;
  private final RequestManagerTreeNode requestManagerTreeNode =
      new SupportFragmentRequestManagerTreeNode();
  private final Set<SupportRequestManagerFragment> childRequestManagerFragments = new HashSet<>();

  @Nullable private SupportRequestManagerFragment rootRequestManagerFragment;
  @Nullable private RequestManager requestManager;
  @Nullable private Fragment parentFragmentHint;

  public SupportRequestManagerFragment() {
    this(new ActivityFragmentLifecycle());
  }

  @VisibleForTesting
  @SuppressLint("ValidFragment")
  public SupportRequestManagerFragment(@NonNull ActivityFragmentLifecycle lifecycle) {
    this.lifecycle = lifecycle;
  }

  /**
   * Sets the current {@link com.pasc.lib.glide.RequestManager}.
   *
   * @param requestManager The manager to put.
   */
  public void setRequestManager(@Nullable RequestManager requestManager) {
    this.requestManager = requestManager;
  }

  @NonNull
  ActivityFragmentLifecycle getGlideLifecycle() {
    return lifecycle;
  }

  /**
   * Returns the current {@link com.pasc.lib.glide.RequestManager} or null if none is put.
   */
  @Nullable
  public RequestManager getRequestManager() {
    return requestManager;
  }

  /**
   * Returns the {@link RequestManagerTreeNode} that provides tree traversal methods relative
   * to the
   * associated {@link RequestManager}.
   */
  @NonNull
  public RequestManagerTreeNode getRequestManagerTreeNode() {
    return requestManagerTreeNode;
  }

  private void addChildRequestManagerFragment(SupportRequestManagerFragment child) {
    childRequestManagerFragments.add(child);
  }

  private void removeChildRequestManagerFragment(SupportRequestManagerFragment child) {
    childRequestManagerFragments.remove(child);
  }

  /**
   * Returns the set of fragments that this RequestManagerFragment's parent is a parent to. (i.e.
   * our parent is the fragment that we are annotating).
   */
  @Synthetic
  @NonNull
  Set<SupportRequestManagerFragment> getDescendantRequestManagerFragments() {
    if (rootRequestManagerFragment == null) {
      return Collections.emptySet();
    } else if (equals(rootRequestManagerFragment)) {
      return Collections.unmodifiableSet(childRequestManagerFragments);
    } else {
      Set<SupportRequestManagerFragment> descendants = new HashSet<>();
      for (SupportRequestManagerFragment fragment : rootRequestManagerFragment
          .getDescendantRequestManagerFragments()) {
        if (isDescendant(fragment.getParentFragmentUsingHint())) {
          descendants.add(fragment);
        }
      }
      return Collections.unmodifiableSet(descendants);
    }
  }

  /**
   * Sets a hint for which fragment is our parent which allows the fragment to return correct
   * information about its parents before pending fragment transactions have been executed.
   */
  void setParentFragmentHint(@Nullable Fragment parentFragmentHint) {
    this.parentFragmentHint = parentFragmentHint;
    if (parentFragmentHint != null && parentFragmentHint.getActivity() != null) {
      registerFragmentWithRoot(parentFragmentHint.getActivity());
    }
  }

  @Nullable
  private Fragment getParentFragmentUsingHint() {
    Fragment fragment = getParentFragment();
    return fragment != null ? fragment : parentFragmentHint;
  }

  /**
   * Returns true if the fragment is a descendant of our parent.
   */
  private boolean isDescendant(@NonNull Fragment fragment) {
    Fragment root = getParentFragmentUsingHint();
    Fragment parentFragment;
    while ((parentFragment = fragment.getParentFragment()) != null) {
      if (parentFragment.equals(root)) {
        return true;
      }
      fragment = fragment.getParentFragment();
    }
    return false;
  }

  private void registerFragmentWithRoot(@NonNull FragmentActivity activity) {
    unregisterFragmentWithRoot();
    rootRequestManagerFragment = Glide.get(activity).getRequestManagerRetriever()
        .getSupportRequestManagerFragment(activity.getSupportFragmentManager(), null);
    if (!equals(rootRequestManagerFragment)) {
      rootRequestManagerFragment.addChildRequestManagerFragment(this);
    }
  }

  private void unregisterFragmentWithRoot() {
    if (rootRequestManagerFragment != null) {
      rootRequestManagerFragment.removeChildRequestManagerFragment(this);
      rootRequestManagerFragment = null;
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    try {
      registerFragmentWithRoot(getActivity());
    } catch (IllegalStateException e) {
      // OnAttach can be called after the activity is destroyed, see #497.
      if (Log.isLoggable(TAG, Log.WARN)) {
        Log.w(TAG, "Unable to register fragment with root", e);
      }
    }
  }

  @Override
  public void onDetach() {
    super.onDetach();
    parentFragmentHint = null;
    unregisterFragmentWithRoot();
  }

  @Override
  public void onStart() {
    super.onStart();
    lifecycle.onStart();
  }

  @Override
  public void onStop() {
    super.onStop();
    lifecycle.onStop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    lifecycle.onDestroy();
    unregisterFragmentWithRoot();
  }

  @Override
  public String toString() {
    return super.toString() + "{parent=" + getParentFragmentUsingHint() + "}";
  }

  private class SupportFragmentRequestManagerTreeNode implements RequestManagerTreeNode {

    @Synthetic
    SupportFragmentRequestManagerTreeNode() { }

    @NonNull
    @Override
    public Set<RequestManager> getDescendants() {
      Set<SupportRequestManagerFragment> descendantFragments =
          getDescendantRequestManagerFragments();
      Set<RequestManager> descendants = new HashSet<>(descendantFragments.size());
      for (SupportRequestManagerFragment fragment : descendantFragments) {
        if (fragment.getRequestManager() != null) {
          descendants.add(fragment.getRequestManager());
        }
      }
      return descendants;
    }

    @Override
    public String toString() {
      return super.toString() + "{fragment=" + SupportRequestManagerFragment.this + "}";
    }
  }
}
