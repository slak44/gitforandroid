package slak.gitforandroid

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import org.eclipse.jgit.diff.DiffEntry
import slak.gitforandroid.filesystem.FSAbstractListItem
import slak.gitforandroid.filesystem.FSItemType

enum class GitStatus {
  ADDED, MODIFIED, RENAMED, COPIED, NONE;

  companion object {
    fun from(t: DiffEntry.ChangeType): GitStatus = when (t) {
      DiffEntry.ChangeType.ADD -> ADDED
      DiffEntry.ChangeType.MODIFY -> MODIFIED
      DiffEntry.ChangeType.RENAME -> RENAMED
      DiffEntry.ChangeType.COPY -> COPIED
      DiffEntry.ChangeType.DELETE -> NONE
    }
  }
}

class RepoListItemView : FSAbstractListItem {
  constructor(context: Context) : super(context) {}
  constructor(context: Context, set: AttributeSet) : super(context, set) {}
  constructor(context: Context, set: AttributeSet, defStyle: Int) : super(context, set, defStyle) {}

  private fun getTypeDrawable(): Drawable {
    val drawable = when (type) {
      FSItemType.FILE -> context.getDrawable(R.drawable.black_ic_file_24dp)
      FSItemType.FOLDER -> context.getDrawable(R.drawable.black_ic_folder_24dp)
      FSItemType.NONE -> ColorDrawable(Color.TRANSPARENT)
    }
    drawable.mutate()
    drawable.setColorFilter(resources.getColor(android.R.color.white, null), PorterDuff.Mode.SRC_ATOP)
    return drawable
  }

  private fun getStatusDrawable(): Drawable {
    val drawableId = when (gitStatus) {
      GitStatus.ADDED -> R.drawable.white_ic_add_24dp
      GitStatus.MODIFIED -> R.drawable.black_ic_create_24dp
      GitStatus.RENAMED, GitStatus.COPIED -> R.drawable.black_ic_content_copy_24dp
      GitStatus.NONE -> 0
    }
    val drawable =
        if (drawableId == 0) ColorDrawable(Color.TRANSPARENT)
        else context.getDrawable(drawableId)
    drawable.mutate()
    val color = when (gitStatus) {
      GitStatus.ADDED -> R.color.gitAdded
      GitStatus.MODIFIED -> R.color.gitModified
      GitStatus.RENAMED, GitStatus.COPIED -> R.color.gitMoved
      GitStatus.NONE -> R.color.transparent
    }
    drawable.setColorFilter(resources.getColor(color, null), PorterDuff.Mode.SRC_ATOP)
    return drawable
  }

  private fun redrawDrawables() {
    setCompoundDrawablesWithIntrinsicBounds(getTypeDrawable(), null, getStatusDrawable(), null)
  }

  var gitStatus: GitStatus = GitStatus.NONE
    get
    set(value) {
      field = value
      redrawDrawables()
    }

  override var type: FSItemType = FSItemType.NONE
    get
    set(type) {
      field = type
      redrawDrawables()
    }
}
