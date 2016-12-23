package slak.gitforandroid.filesystem

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import slak.gitforandroid.R

enum class FSItemType {
  FILE, FOLDER, NONE
}

class FSListItemView : TextView {
  constructor(context: Context) : super(context) {}
  constructor(context: Context, set: AttributeSet) : super(context, set) {}
  constructor(context: Context, set: AttributeSet, defStyle: Int) : super(context, set, defStyle) {}

  var type: FSItemType = FSItemType.NONE
    get
    set(type) {
      field = type
      val drawable = context.getDrawable(when (type) {
        FSItemType.FILE -> R.drawable.black_ic_file_24dp
        FSItemType.FOLDER -> R.drawable.black_ic_folder_24dp
        FSItemType.NONE -> 0
      })
      setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
    }
}