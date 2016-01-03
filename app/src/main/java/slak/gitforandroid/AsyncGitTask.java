package slak.gitforandroid;

import android.os.AsyncTask;

public abstract class AsyncGitTask extends AsyncTask<String, Integer, Integer> {
  private Exception ex = null;
  private AsyncTaskCallback onFinish;

  public AsyncGitTask(AsyncTaskCallback onFinish) {
    this.onFinish = onFinish;
  }

  @Override
  // The return value is the status code: non-0 is trouble
  protected Integer doInBackground(String... params) {
    try {
      safelyDoInBackground();
    } catch (Exception ex) {
      this.ex = ex;
      return 1;
    }
    return 0;
  }

  public abstract void safelyDoInBackground() throws Exception;

  @Override
  protected void onPostExecute(Integer integer) {
    onFinish.onFinish(this);
  }

  public Exception getException() {
    return ex;
  }

  public abstract class AsyncTaskCallback {
    public abstract void onFinish(AsyncGitTask completedTask);
  }
}
