package ca.barrenechea.widget.recyclerview.decoration;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
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
        public boolean onSingleTapUp(MotionEvent e) {
            long headerId = mDecor.findHeaderIdUnder((int) e.getX(), (int) e.getY());
            if (headerId != -1L) {
                View headerView = mDecor.getHeaderView(headerId);
                mOnHeaderClickListener.onHeaderClick(headerView, headerId);
                mRecyclerView.playSoundEffect(SoundEffectConstants.CLICK);
                headerView.onTouchEvent(e);
                return true;
            }
            return false;
        }
    }

    public interface OnHeaderClickListener {

        void onHeaderClick(View header, long headerId);
    }

    private final StickyHeaderDecoration mDecor;

    private OnHeaderClickListener mOnHeaderClickListener;

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
        if (this.mOnHeaderClickListener != null) {
            boolean tapDetectorResponse = this.mTapDetector.onTouchEvent(e);
            if (tapDetectorResponse) {
                // Don't return false if a single tap is detected
                return true;
            }
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                return mDecor.findHeaderIdUnder((int) e.getX(), (int) e.getY()) != -1;
            }
        }
        return false;
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // do nothing
    }

    @Override
    public void onTouchEvent(RecyclerView view, MotionEvent e) { /* do nothing? */ }

    public void setOnHeaderClickListener(OnHeaderClickListener listener) {
        mOnHeaderClickListener = listener;
    }
}