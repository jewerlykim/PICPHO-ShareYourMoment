package com.picpho.pic_pho.WifiDirect.UI

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class HorizontalItemDecoration(private val horizontalSpaceWidth:Int) :
    RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = horizontalSpaceWidth
        outRect.right = horizontalSpaceWidth
    }

}