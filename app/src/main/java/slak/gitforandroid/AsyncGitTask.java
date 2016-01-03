package slak.gitforandroid;

import android.os.AsyncTask;

public class AsyncGitTask extends AsyncTask<String, Integer, Integer> {
  private Exception ex = null;

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
  public void safelyDoInBackground() throws Exception {}

  @Override
  protected void onPostExecute(Integer integer) {

  }

  public Exception getException() {
    return ex;
  }
}
