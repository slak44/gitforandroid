package slak.gitforandroid.filesystem

import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView

import java.io.File
import java.util.ArrayList

import slak.gitforandroid.R

internal class FSArrayAdapter(
    private val context: AppCompatActivity,
    @LayoutRes resource: Int,
    private val nodes: ArrayList<SelectableAdapterModel<File>>
) : ArrayAdapter<SelectableAdapterModel<File>>(context, resource, nodes) {

  override fun getViewTypeCount(): Int = 2

  override fun getItemViewType(position: Int): Int = when (true) {
    nodes[position].thing.isDirectory -> 0
    nodes[position].thing.isFile -> 1
    else -> 1
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    val v =
        if (convertView != null && convertView is FSListItemView) convertView
        else context.layoutInflater.inflate(R.layout.list_element, parent, false) as FSListItemView
    v.text = nodes[position].thing.name
    v.type = when (getItemViewType(position)) {
      0 -> FSItemType.FOLDER
      1 -> FSItemType.FILE
      else -> FSItemType.FILE
    }
    return v
  }
}