package slak.gitforandroid

import android.content.Context
import android.preference.PreferenceManager
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View

fun reportError(snack: View, formatted: String, e: Throwable) {
  Log.e("GitForAndroid", formatted, e)
  // TODO: make 'more' button on snack, to lead to err text
  Snackbar.make(snack, formatted, Snackbar.LENGTH_LONG).show()
}

fun getStringSetting(context: Context, key: String): String {
  return PreferenceManager.getDefaultSharedPreferences(context).getString(key, "")
}
