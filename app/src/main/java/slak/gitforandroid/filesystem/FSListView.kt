package slak.gitforandroid.filesystem

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.widget.AdapterView
import android.widget.ListView

import java.io.File
import java.util.ArrayList
import java.util.Stack

import slak.gitforandroid.R
import slak.gitforandroid.activities.RepoViewActivity

class FSListView : ListView {
  private var nodes: ArrayList<SelectableAdapterModel<File>>
  private var listElements: FSArrayAdapter? = null
  private var folderStack = Stack<SelectableAdapterModel<File>>()
  private var root: File? = null

  private var multiSelectState = false

  internal val selectedPaths: ArrayList<String>
    get() = SelectableAdapterModel.getSelectedModels(nodes).mapTo(ArrayList<String>()) {
      nodes[it].thing.toURI().relativize(root!!.toURI()).toString()
    }

  /**
   * Called when an item is long-pressed.
   */
  internal var onMultiSelectStart: () -> Unit = {}
    set
  /**
   * Called when no more items are long-pressed.
   */
  internal var onMultiSelectEnd: () -> Unit = {}
    set
  /**
   * Called when the folder at the top of the stack changes.
   */
  internal var onFolderChange: (old: File, new: File) -> Unit = { old: File, new: File -> }
    set

  init {
    nodes = ArrayList<SelectableAdapterModel<File>>()
  }

  constructor(context: Context) : this(context, null) {}
  constructor(context: Context, set: AttributeSet?) : super(context, set) {}
  constructor(context: Context, set: AttributeSet, defStyle: Int) : super(context, set, defStyle) {}

  /**
   * Populate the list with files from the given root.
   * @param root the root of the navigation
   */
  fun init(activity: RepoViewActivity, root: File) {
    this.root = root
    // Don't bother doing anything in the editor
    if (isInEditMode) return
    folderStack.push(SelectableAdapterModel(root))
    updateNodes()
    listElements = FSArrayAdapter(activity, R.layout.list_element_main, nodes)
    super.setAdapter(listElements)
    super.setOnItemClickListener(AdapterView.OnItemClickListener { parent, view, position, id ->
      // Directory empty, nothing to do here
      if (nodes.size == 0) return@OnItemClickListener
      // Clicked on a selected item: deselect it
      if (nodes[position].selected && multiSelectState) {
        nodes[position].selected = false
        view.background = this.background
        // No more items are selected: reset the style of the toolbar
        if (SelectableAdapterModel.getSelectedModels(nodes).size == 0) {
          multiSelectState = false
          onMultiSelectEnd()
        }
        return@OnItemClickListener
        // Clicked on a unselected item in multi-select: select it
      } else if (multiSelectState) {
        nodes[position].selected = true
        view.setBackgroundColor(
            resources.getColor(R.color.colorSelected, activity.theme))
        return@OnItemClickListener
      }
      if (view !is FSListItemView) return@OnItemClickListener
      if (view.type == FSItemType.FOLDER) {
        folderStack.push(nodes[position])
        updateNodes()
        listElements!!.notifyDataSetChanged()
      } else if (view.type == FSItemType.FILE) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.parse("content://" + nodes[position].thing.absolutePath)
        intent.setDataAndType(uri, "text/plain")
        activity.startActivity(intent)
      }
    })
    this.onItemLongClickListener = AdapterView.OnItemLongClickListener {
      parent, view, position, id ->
      if (view !is FSListItemView) return@OnItemLongClickListener false
      multiSelectState = true
      // No item was checked: style the toolbar for multi-select
      if (SelectableAdapterModel.getSelectedModels(nodes).size == 0) onMultiSelectStart()
      nodes[position].selected = true
      view.setBackgroundColor(
          resources.getColor(R.color.colorSelected, activity.theme))
      return@OnItemLongClickListener true
    }
  }

  /**
   * Updates the nodes array based on the top of the folderStack.
   */
  private fun updateNodes() {
    nodes.clear()
    nodes.addAll(SelectableAdapterModel.fromArray(folderStack.peek().thing.listFiles()))

    // Little hack to get the old directory for the folder change
    if (folderStack.size > 1) {
      val topOfStack = folderStack.peek()
      folderStack.pop()
      onFolderChange(folderStack.peek().thing, topOfStack.thing)
      folderStack.push(topOfStack)
    } else if (folderStack.size == 1) {
      onFolderChange(folderStack.peek().thing, folderStack.peek().thing)
    }

    nodes.sort { lhs, rhs ->
      // Directories before files
      if (lhs.thing.isDirectory xor rhs.thing.isDirectory) {
        if (lhs.thing.isDirectory)
          return@sort -1
        else
          return@sort 1
      }
      // Lexicographic comparison of node names
      lhs.thing.name.toLowerCase().compareTo(rhs.thing.name.toLowerCase())
    }
  }

  /**
   * Goes one level up the filesystem tree.
   * @return false if we reached the top of the tree, true otherwise
   */
  fun goUp(): Boolean {
    if (folderStack.size <= 1) {
      folderStack = Stack<SelectableAdapterModel<File>>()
      return false
    } else {
      folderStack.pop()
      updateNodes()
      listElements!!.notifyDataSetChanged()
    }
    return true
  }
}
