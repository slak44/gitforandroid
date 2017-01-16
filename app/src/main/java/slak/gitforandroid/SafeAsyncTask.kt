package slak.gitforandroid

import android.os.AsyncTask

class SafeAsyncTask(
    private val safelyDoInBackground: () -> Unit,
    private val onFinish: (SafeAsyncTask) -> Unit
): AsyncTask<String, Int, Int>() {
  var exception: Throwable? = null
    private set

  // The return value is the status code: non-0 is trouble
  override fun doInBackground(vararg params: String): Int? {
    try {
      safelyDoInBackground()
    } catch (ex: Throwable) {
      this.exception = ex
      return 1
    }
    return 0
  }

  override fun onPostExecute(integer: Int?) {
    onFinish(this)
  }
}
