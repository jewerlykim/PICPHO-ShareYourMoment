package com.picpho.pic_pho.WifiDirect.UI

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class VerticalItemDecoration(private val verticalSpaceWidth:Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.top = verticalSpaceWidth
        outRect.bottom = verticalSpaceWidth
    }

}