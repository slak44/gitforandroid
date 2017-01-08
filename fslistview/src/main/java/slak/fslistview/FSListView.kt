package slak.fslistview

import android.content.Context
import android.support.annotation.ColorRes
import android.support.annotation.LayoutRes
import android.support.v7.app.AppCompatActivity
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView

import java.io.File
import java.util.ArrayList
import java.util.Stack

class FSListView : ListView {
  companion object {
    const val TAG = "FSListView"
  }

  private var nodes: ArrayList<SelectableAdapterModel<File>> = ArrayList()
  private var listElements: FSArrayAdapter? = null
  private var folderStack = Stack<SelectableAdapterModel<File>>()
  private var root: File? = null
  private @LayoutRes var listLayout: Int? = null

  private var isMultiSelecting = false

  val selectedPaths: ArrayList<String>
    get() = SelectableAdapterModel.getSelectedModels(nodes).mapTo(ArrayList<String>()) {
      nodes[it].thing.toURI().relativize(root!!.toURI()).toString()
    }

  /**
   * Set to false to disable multi-select functionality
   */
  var useMultiSelect = true
    get set
  /**
   * Called when an item is long-pressed.
   */
  var onMultiSelectStart: () -> Unit = {}
  /**
   * Called when no more items are long-pressed.
   */
  var onMultiSelectEnd: () -> Unit = {}
  /**
   * Called when the folder at the top of the stack changes.
   */
  var onFolderChange: (old: File, new: File) -> Unit = { old: File, new: File -> }
  /**
   * Called when a file is opened by the user.
   */
  var onFileOpen: (File) -> Unit = {}
  /**
   * Called by the FSArrayAdapter for each view it needs to prepare. Implementations of this
   * function should try to recycle the given View? if it is possible, and inflate a new layout only
   * if necessary.
   */
  var onChildViewPrepare: (AppCompatActivity, File, View?, ViewGroup) -> FSAbstractListItem
      = { context: AppCompatActivity, file: File, convertView: View?, parent: ViewGroup ->
    if (convertView != null && convertView is FSAbstractListItem) convertView
    else context.layoutInflater.inflate(listLayout!!, parent, false) as FSAbstractListItem
  }

  constructor(context: Context) : this(context, null)
  constructor(context: Context, set: AttributeSet?) : super(context, set)
  constructor(context: Context, set: AttributeSet, defStyle: Int) : super(context, set, defStyle)

  /**
   * Populate the list with files from the given root.
   * @param root the root of the navigation
   */
  fun init(activity: AppCompatActivity, root: File, @LayoutRes listElement: Int, @ColorRes selectedColor: Int) {
    this.listLayout = listElement
    this.root = root
    // Don't bother doing anything in the editor
    if (isInEditMode) return
    folderStack.push(SelectableAdapterModel(root))
    listElements = FSArrayAdapter(activity, listElement, nodes, this)
    adapter = listElements
    refresh()
    fireFolderChangeEvents()
    onItemClickListener = AdapterView.OnItemClickListener { parent, view, position, id ->
      // Directory empty, nothing to do here
      if (nodes.size == 0) return@OnItemClickListener
      if (useMultiSelect) {
        // Clicked on a selected item: deselect it
        if (nodes[position].selected && isMultiSelecting) {
          nodes[position].selected = false
          view.background = this.background
          // No more items are selected: reset the style of the toolbar
          if (SelectableAdapterModel.getSelectedModels(nodes).size == 0) {
            isMultiSelecting = false
            onMultiSelectEnd()
          }
          return@OnItemClickListener
          // Clicked on a unselected item in multi-select: select it
        } else if (isMultiSelecting) {
          nodes[position].selected = true
          view.setBackgroundColor(
              resources.getColor(selectedColor, activity.theme))
          return@OnItemClickListener
        }
      }
      if (view !is FSAbstractListItem) return@OnItemClickListener
      if (view.type == FSItemType.FOLDER) {
        folderStack.push(nodes[position])
        refresh()
        fireFolderChangeEvents()
      } else if (view.type == FSItemType.FILE) {
        onFileOpen(nodes[position].thing)
      } else {
        Log.w(TAG, "Unhandled FSItemType!")
      }
    }
    onItemLongClickListener = AdapterView.OnItemLongClickListener {
      parent, view, position, id ->
      if (useMultiSelect) {
        isMultiSelecting = true
        // No item was checked: style the toolbar for multi-select
        if (SelectableAdapterModel.getSelectedModels(nodes).size == 0) onMultiSelectStart()
        nodes[position].selected = true
        view.setBackgroundColor(
            resources.getColor(selectedColor, activity.theme))
        return@OnItemLongClickListener true
      }
      return@OnItemLongClickListener false
    }
  }

  private fun fireFolderChangeEvents() {
    // Little hack to get the old directory for the folder change
    if (folderStack.size > 1) {
      val topOfStack = folderStack.peek()
      folderStack.pop()
      onFolderChange(folderStack.peek().thing, topOfStack.thing)
      folderStack.push(topOfStack)
    } else if (folderStack.size == 1) {
      onFolderChange(folderStack.peek().thing, folderStack.peek().thing)
    }
  }

  /**
   * Get the currently displayed directory.
   */
  val currentDirectory: File
    get() = folderStack.peek().thing

  /**
   * Notify the adapter that the data was somehow changed externally.
   */
  fun update() {
    listElements!!.notifyDataSetChanged()
  }

  /**
   * Re-add the files in the current directory, and update the views.
   */
  fun refresh() {
    nodes.clear()
    nodes.addAll(SelectableAdapterModel.fromArray(folderStack.peek().thing.listFiles()))

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

    listElements!!.notifyDataSetChanged()
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
      refresh()
    }
    return true
  }
}
