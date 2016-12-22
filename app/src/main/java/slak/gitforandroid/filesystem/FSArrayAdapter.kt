package slak.gitforandroid.filesystem

import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

import java.io.File
import java.util.ArrayList

import slak.gitforandroid.R

internal class FSArrayAdapter(
    private val context: AppCompatActivity,
    @LayoutRes resource: Int,
    private val nodes: ArrayList<SelectableAdapterModel<File>>
) : ArrayAdapter<SelectableAdapterModel<File>>(context, resource, nodes) {

  override fun getViewTypeCount(): Int = 3

  override fun getItemViewType(position: Int): Int = when (true) {
    nodes.isEmpty() -> 2
    nodes[position].thing.isDirectory -> 0
    nodes[position].thing.isFile -> 1
    else -> 1
  }

  override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
    if (this.getItemViewType(position) == 2) {
      return context.layoutInflater.inflate(R.layout.list_element_folder_empty, parent, false)
    }
    val nodeName = nodes[position].thing.name
    val nodeView: TextView
    val whatNode = if (getItemViewType(position) == 0)
      R.id.list_element_folder
    else
      R.id.list_element_file
    // If convertView has the correct layout and type, reuse it
    // Otherwise, inflate new layout
    if (convertView != null && convertView is TextView && whatNode == convertView.id) {
      nodeView = convertView
    } else {
      val layoutToUse = if (getItemViewType(position) == 0)
        R.layout.list_element_folder
      else
        R.layout.list_element_file
      nodeView = context.layoutInflater.inflate(layoutToUse, parent, false) as TextView
    }
    nodeView.text = nodeName
    return nodeView
  }

  override fun getCount(): Int {
    if (nodes.size == 0)
      return 1
    else
      return super.getCount()
  }
}
