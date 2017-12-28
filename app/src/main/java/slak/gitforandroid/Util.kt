package slak.gitforandroid

import android.content.Context
import android.content.res.Resources
import android.graphics.PorterDuff
import android.preference.PreferenceManager
import android.support.annotation.ColorRes
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.MenuItem
import android.view.View
import kotlinx.coroutines.experimental.*
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Wraps [async], except it also rethrows exceptions synchronously.
 */
fun <T> async2(
    context: CoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> T
): Deferred<T> {
  val c = async(context, start, block)
  c.invokeOnCompletion { e -> if (e != null) throw e }
  return c
}

inline fun Job.withSnackResult(view: View,
                               @StringRes success: Int,
                               @StringRes failure: Int,
                               crossinline callback: (t: Throwable?) -> Unit = {}): Job {
  return this.withSnackResult(view, view.context.resources.getString(success),
      view.context.resources.getString(failure), callback)
}

inline fun Job.withSnackResult(view: View,
                               success: String,
                               failure: String,
                               crossinline callback: (t: Throwable?) -> Unit = {}): Job {
  invokeOnCompletion { throwable ->
    if (throwable == null) {
      Snackbar.make(view, success, Snackbar.LENGTH_LONG)
    } else {
      Snackbar.make(view, failure, Snackbar.LENGTH_LONG)
      Log.e("withSnackResult", "caught", throwable)
    }
    callback(throwable)
  }
  return this
}

fun reportError(snack: View, formatted: String, e: Throwable) {
  Log.e("GitForAndroid", formatted, e)
  // TODO: make 'more' button on snack, to lead to err text
  Snackbar.make(snack, formatted, Snackbar.LENGTH_LONG).show()
}

// FIXME
fun getStringSetting(context: Context, key: String): String {
  return PreferenceManager.getDefaultSharedPreferences(context).getString(key, "")
}

/**
 * Emulates android:iconTint. Must be called in onPrepareOptionsMenu for each icon.
 */
fun MenuItem.iconTint(context: Context, @ColorRes colorRes: Int) {
  val color = context.resources.getColor(colorRes, context.theme)
  val drawable = this.icon
  drawable.setColorFilter(color, PorterDuff.Mode.SRC_IN)
  this.icon = drawable
}
