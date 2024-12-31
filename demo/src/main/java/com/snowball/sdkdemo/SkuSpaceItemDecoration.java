package com.snowball.sdkdemo;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class SkuSpaceItemDecoration extends RecyclerView.ItemDecoration {

    private final int mMargin;

    public SkuSpaceItemDecoration(int margin) {
        mMargin = margin;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        LinearLayoutManager layoutManager = (LinearLayoutManager) parent.getLayoutManager();
        if (layoutManager == null) {
            return;
        }
        int adapterPosition = parent.getChildAdapterPosition(view);
        if (adapterPosition != layoutManager.getItemCount() - 1) {
            outRect.bottom = mMargin;
        }
    }
}