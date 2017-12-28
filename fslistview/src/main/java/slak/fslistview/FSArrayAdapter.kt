package slak.fslistview

import android.content.Context
import android.support.annotation.LayoutRes
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import java.io.File
import java.util.ArrayList

internal class FSArrayAdapter(context: Context,
                              @LayoutRes resource: Int,
                              private val nodes: ArrayList<SelectableAdapterModel<File>>,
                              private val lv: FSListView
) : ArrayAdapter<SelectableAdapterModel<File>>(context, resource, nodes) {

  override fun getViewTypeCount(): Int = 2

  override fun getItemViewType(position: Int): Int = when (true) {
    nodes[position].thing.isDirectory -> 0
    nodes[position].thing.isFile -> 1
    else -> 1
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val v = lv.onChildViewPrepare(context, nodes[position].thing, convertView, parent)
    v.text = nodes[position].thing.name
    v.type = when (getItemViewType(position)) {
      0 -> FSItemType.FOLDER
      1 -> FSItemType.FILE
      else -> FSItemType.NONE
    }
    return v
  }
}
