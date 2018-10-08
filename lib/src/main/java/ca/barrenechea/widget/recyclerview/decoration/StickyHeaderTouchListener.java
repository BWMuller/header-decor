package ca.barrenechea.widget.recyclerview.decoration;

import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A sticky header item click listener for sticky header decorator.
 */
public class StickyHeaderTouchListener implements RecyclerView.OnItemTouchListener {

    private class SingleTapDetector extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            return true;
        }

        @Override
        public void onLongPress(final MotionEvent e) {
            long headerId = mDecor.findHeaderIdUnder((int) e.getX(), (int) e.getY());
            if (headerId != -1L) {
                View headerView = mDecor.getHeaderView(headerId);
                if (!headerView.onTouchEvent(e)) {
                    super.onLongPress(e);
                }
            } else {
                super.onLongPress(e);
            }
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            long headerId = mDecor.findHeaderIdUnder((int) e.getX(), (int) e.getY());
            if (headerId != -1L) {
                View headerView = mDecor.getHeaderView(headerId);
                if (headerView instanceof ViewGroup) {
                    View clickableView = findFirstClickableView(findViewAt((ViewGroup) headerView, mDecor.getHeaderTop(headerId), (int) e.getX(), (int) e.getY()));
                    if (clickableView != null) {
                        return clickableView.performClick();
                    } else {
                        return headerView.onTouchEvent(e);
                    }
                } else {
                    View clickableView = findFirstClickableView(headerView);
                    if (clickableView != null) {
                        return clickableView.performClick();
                    } else {
                        return headerView.onTouchEvent(e);
                    }
                }
            }
            return false;
        }

        private View findFirstClickableView(View view) {
            if (view != null) {
                if (view.isClickable() && view.hasOnClickListeners()) {
                    return view;
                }
                if (view.getParent() instanceof View) {
                    return findFirstClickableView((View) view.getParent());
                }
            }
            return null;
        }

        private View findViewAt(ViewGroup viewGroup, int top, int x, int y) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof ViewGroup) {
                    View foundView = findViewAt((ViewGroup) child, top, x, y);
                    if (foundView != null && foundView.isShown()) {
                        return foundView;
                    }
                } else {
                    Rect rect = new Rect(child.getLeft(), top + child.getTop(), child.getRight(), top + child.getBottom());
                    if (rect.contains(x, y)) {
                        return child;
                    }
                }
            }

            return null;
        }
    }

    private final StickyHeaderDecoration mDecor;

    private final RecyclerView mRecyclerView;

    private final GestureDetector mTapDetector;

    public StickyHeaderTouchListener(final RecyclerView recyclerView,
            final StickyHeaderDecoration decor) {
        mTapDetector = new GestureDetector(recyclerView.getContext(), new SingleTapDetector());
        mRecyclerView = recyclerView;
        mDecor = decor;
    }

    public StickyHeaderAdapter getAdapter() {
        if (mRecyclerView.getAdapter() instanceof StickyHeaderAdapter) {
            return (StickyHeaderAdapter) mRecyclerView.getAdapter();
        } else {
            throw new IllegalStateException("A RecyclerView with " +
                    StickyHeaderTouchListener.class.getSimpleName() +
                    " requires a " + StickyHeaderAdapter.class.getSimpleName());
        }
    }

    @Override
    public boolean onInterceptTouchEvent(RecyclerView view, MotionEvent e) {
        boolean tapDetectorResponse = this.mTapDetector.onTouchEvent(e);
        if (tapDetectorResponse) {
            // Don't return false if a single tap is detected
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            return mDecor.findHeaderIdUnder((int) e.getX(), (int) e.getY()) != -1;
        }
        return false;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // do nothing
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent e) { /* do nothing? */ }
}