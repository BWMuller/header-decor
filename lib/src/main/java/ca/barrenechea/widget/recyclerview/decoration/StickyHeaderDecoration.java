/*
 * Copyright 2014 Eduardo Barrenechea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ca.barrenechea.widget.recyclerview.decoration;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import java.util.HashMap;
import java.util.Map;

/**
 * A sticky header decoration for android's RecyclerView.
 */
public class StickyHeaderDecoration extends RecyclerView.ItemDecoration {

    public static final long NO_HEADER_ID = -1L;

    private StickyHeaderAdapter adapter;

    private Map<Long, RecyclerView.ViewHolder> headerCache;

    private Map<Long, Rect> headerDrawHistory;

    private boolean renderInline;

    private boolean reversed;

    /**
     * @param adapter the sticky header adapter to use
     */
    public StickyHeaderDecoration(@NonNull StickyHeaderAdapter adapter) {
        this(adapter, false);
    }

    /**
     * @param adapter the sticky header adapter to use
     */
    public StickyHeaderDecoration(@NonNull StickyHeaderAdapter adapter, boolean renderInline) {
        this.adapter = adapter;
        this.headerCache = new HashMap<>();
        this.headerDrawHistory = new HashMap<>();
        this.renderInline = renderInline;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    /**
     * Clears the header view cache. Headers will be recreated and
     * rebound on list scroll after this method has been called.
     */
    public void clearHeaderCache() {
        headerCache.clear();
        headerDrawHistory.clear();
    }

    /**
     * Gets the id of the header under the specified (x, y) coordinates.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return id of header, or -1 if not found
     */
    public long findHeaderIdUnder(int x, int y) {
        for (Long key : headerDrawHistory.keySet()) {
            Rect holderRect = headerDrawHistory.get(key);
            if (holderRect != null && holderRect.contains(x, y)) {
                return key;
            }
        }
        return -1L;
    }

    public int getHeaderTop(long headerId) {
        return headerDrawHistory.get(headerId).top;
    }

    @Nullable
    public View findHeaderViewUnder(float x, float y) {
        for (RecyclerView.ViewHolder holder : headerCache.values()) {
            final View child = holder.itemView;
            final float translationX = child.getTranslationX();
            final float translationY = child.getTranslationY();

            if (x >= child.getLeft() + translationX &&
                    x <= child.getRight() + translationX &&
                    y >= child.getTop() + translationY &&
                    y <= child.getBottom() + translationY) {
                return child;
            }
        }

        return null;
    }

    public View getHeaderView(long headerId) {
        if (headerCache.containsKey(headerId)) {
            return headerCache.get(headerId).itemView;
        } else {
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
            @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

        int position = parent.getChildAdapterPosition(view);
        int headerHeight = 0;

        if (position != RecyclerView.NO_POSITION
                && hasHeader(position)
                && showHeaderAboveItem(position)) {

            View header = getHeader(parent, position).itemView;
            headerHeight = getHeaderHeightForLayout(header);
        }

        outRect.set(0, headerHeight, 0, 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onDrawOver(@NonNull Canvas canvas, @NonNull RecyclerView parent,
            @NonNull RecyclerView.State state) {

        final int count = parent.getChildCount();
        long previousHeaderId = -1;

        headerDrawHistory.clear();
        for (int layoutPos = 0; layoutPos < count; layoutPos++) {
            final View child = parent.getChildAt(layoutPos);
            final int adapterPos = parent.getChildAdapterPosition(child);

            if (adapterPos != RecyclerView.NO_POSITION && hasHeader(adapterPos)) {
                long headerId = adapter.getStickyHeaderId(adapterPos);

                if (headerId != previousHeaderId) {
                    previousHeaderId = headerId;
                    View header = getHeader(parent, adapterPos).itemView;
                    canvas.save();

                    final int left = child.getLeft();
                    final int top = getHeaderTop(parent, child, header, adapterPos, layoutPos);
                    canvas.translate(left, top);

                    header.setTranslationX(left);
                    header.setTranslationY(top);
                    header.draw(canvas);

                    headerDrawHistory.put(headerId, new Rect(left, top, canvas.getWidth(), top + header.getHeight()));

                    canvas.restore();
                }
            }
        }
    }

    @NonNull
    private RecyclerView.ViewHolder getHeader(@NonNull RecyclerView parent, int position) {
        final long key = adapter.getStickyHeaderId(position);

        if (headerCache.containsKey(key)) {
            return headerCache.get(key);
        } else {
            final RecyclerView.ViewHolder holder = adapter.onCreateStickyHeaderViewHolder(parent);
            final View header = holder.itemView;

            //noinspection unchecked
            adapter.onBindStickyHeaderViewHolder(holder, position);

            int widthSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredWidth(),
                    View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(parent.getMeasuredHeight(),
                    View.MeasureSpec.UNSPECIFIED);

            int childWidth = ViewGroup.getChildMeasureSpec(widthSpec,
                    parent.getPaddingLeft() + parent.getPaddingRight(),
                    header.getLayoutParams().width);
            int childHeight = ViewGroup.getChildMeasureSpec(heightSpec,
                    parent.getPaddingTop() + parent.getPaddingBottom(),
                    header.getLayoutParams().height);

            header.measure(childWidth, childHeight);
            header.layout(0, 0, header.getMeasuredWidth(), header.getMeasuredHeight());

            headerCache.put(key, holder);

            return holder;
        }
    }

    private int getHeaderHeightForLayout(@NonNull View header) {
        return renderInline ? 0 : header.getHeight();
    }

    private int getHeaderTop(@NonNull RecyclerView parent, @NonNull View child,
            @NonNull View header, int adapterPos, int layoutPos) {

        int headerHeight = getHeaderHeightForLayout(header);
        int top = ((int) child.getY()) - headerHeight;
        if (layoutPos == 0) {
            final int count = parent.getChildCount();
            final long currentId = adapter.getStickyHeaderId(adapterPos);
            // find next view with header and compute the offscreen push if needed
            for (int i = 1; i < count; i++) {
                int adapterPosHere = parent.getChildAdapterPosition(parent.getChildAt(i));
                if (adapterPosHere != RecyclerView.NO_POSITION) {
                    long nextId = adapter.getStickyHeaderId(adapterPosHere);
                    if (nextId != currentId) {
                        final View next = parent.getChildAt(i);
                        final int offset = ((int) next.getY()) - (headerHeight +
                                getHeader(parent, adapterPosHere).itemView.getHeight());
                        if (offset < 0) {
                            return offset;
                        } else {
                            break;
                        }
                    }
                }
            }

            top = Math.max(0, top);
        }

        return top;
    }

    private boolean hasHeader(int position) {
        return adapter.getStickyHeaderId(position) != NO_HEADER_ID;
    }

    private boolean showHeaderAboveItem(int itemAdapterPosition) {
        if (itemAdapterPosition == adapter.getItemCount() - 1) {
            return true;
        }
        if (reversed) {
            return adapter.getStickyHeaderId(itemAdapterPosition + 1) != adapter.getStickyHeaderId(itemAdapterPosition);
        } else {
            return adapter.getStickyHeaderId(itemAdapterPosition - 1) != adapter.getStickyHeaderId(itemAdapterPosition);
        }
    }
}
