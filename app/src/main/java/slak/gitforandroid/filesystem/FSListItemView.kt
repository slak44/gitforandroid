package slak.gitforandroid.filesystem

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView

enum class FSItemType {
  FILE, FOLDER, NONE
}

abstract class FSAbstractListItem : TextView {
  constructor(context: Context) : super(context) {}
  constructor(context: Context, set: AttributeSet) : super(context, set) {}
  constructor(context: Context, set: AttributeSet, defStyle: Int) : super(context, set, defStyle) {}

  abstract var type: FSItemType
}
