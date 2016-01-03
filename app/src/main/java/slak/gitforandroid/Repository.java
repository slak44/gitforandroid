package slak.gitforandroid;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import slak.gitforandroid.activities.SomethingTerribleActivity;

public class Repository {
  private static final String TAG = "Repository";

  private Activity context;
  private String name;
  private boolean alreadyExists;
  private File thisRepo;
  private Git git;

  public Repository(Activity context, String name) {
    this.context = context;
    this.name = name;
    // The storage dir is a folder called repositories in the private external storage
    File storageDir = new File(context.getExternalFilesDir(null), "repositories");
    thisRepo = new File(storageDir, name);
    if (!thisRepo.isDirectory()) thisRepo.delete(); // Delete whatever was blocking the repo
    alreadyExists = thisRepo.exists();
    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    try {
      builder.setGitDir(new File(thisRepo, ".git"));
      org.eclipse.jgit.lib.Repository internalRepo = builder
          .readEnvironment()
          .findGitDir()
          .build();
      git = new Git(internalRepo);
    } catch (IOException ioEx) {
      SomethingTerribleActivity
          .runATerribleActivity(context, ioEx.toString(), "WTF Error");
    }
  }

  public File getRepoFolder() {
    return thisRepo;
  }

  public void gitInit() throws GitAPIException, IOException {
    if (alreadyExists)
      throw new WrongRepositoryStateException("Failed to init: Repository " + name + " already exists");
    git.getRepository().create();
    alreadyExists = true;
  }

  public void gitClone(final String uri) throws GitAPIException, RuntimeException {
    if (alreadyExists)
      throw new WrongRepositoryStateException("Failed to clone: Repository " + name + " already exists");
    AsyncGitTask clone = new AsyncGitTask() {
      @Override
      public void safelyDoInBackground() throws Exception {
        CloneCommand clCom = new CloneCommand();
        clCom.setCloneAllBranches(true);
        clCom.setDirectory(thisRepo);
        clCom.setURI(uri);
        clCom.call();
      }

      @Override
      protected void onPostExecute(Integer integer) {
        if (getException() == null) return;
      }
    };
    clone.execute();
    alreadyExists = true;
  }

  public void gitAdd(ArrayList<String> filePatterns) throws GitAPIException, IOException {
    AddCommand aCom = git.add();
    for (String pattern : filePatterns) aCom.addFilepattern(pattern);
    aCom.call();
  }

  private class AsyncCommitTask extends AsyncTask<String, Integer, Integer> {
    public GitAPIException ex;
    private PersonIdent author;
    private PersonIdent committer;
    private String message;

    AsyncCommitTask(PersonIdent author, PersonIdent committer, String message) {
      this.author = author;
      this.committer = committer;
      this.message = message;
    }

    @Override
    // The return value is the status code: non-0 is trouble
    protected Integer doInBackground(String... params) {
      try {
        git.commit().setAuthor(author).setCommitter(committer).setMessage(message).call();
      } catch (GitAPIException gitEx) {
        this.ex = gitEx;
        return 1;
      }
      return 0;
    }
  }

  public void gitCommit(PersonIdent author, PersonIdent committer, String message) throws RuntimeException {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    PersonIdent stored = new PersonIdent(
        prefs.getString("git_name", ""), prefs.getString("git_email", ""));
    if (author == null) author = stored;
    if (committer == null) committer = stored;
    AsyncCommitTask commitTask = new AsyncCommitTask(author, committer, message);
    commitTask.execute();
    try {
      if (commitTask.get() == 1) throw commitTask.ex;
    } catch (Exception ex) {
      throw new RuntimeException("Exception while committing", ex);
    }
  }

  public void gitCommit(PersonIdent committer, String message) throws RuntimeException {
    gitCommit(committer, committer, message);
  }

  public void gitCommit(String name, String email, String message) throws RuntimeException {
    PersonIdent pi;
    if (name == null || email == null) pi = null;
    else pi = new PersonIdent(name, email);
    gitCommit(pi, pi, message);
  }

  private class AsyncPushTask extends AsyncTask<String, Integer, Integer> {
    public GitAPIException ex;
    private String remote;
    private String password;

    AsyncPushTask(String remote, String password) {
      this.remote = remote;
      this.password = password;
    }

    @Override
    // The return value is the status code: non-0 is trouble
    protected Integer doInBackground(String... params) {
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
      UsernamePasswordCredentialsProvider upcp = new UsernamePasswordCredentialsProvider(
          prefs.getString("git_username", ""),
          password);
      try {
        git.push().setRemote(remote).setCredentialsProvider(upcp).setPushAll().call();
      } catch (GitAPIException gitEx) {
        this.ex = gitEx;
        return 1;
      }
      return 0;
    }
  }

  public void gitPush(String remote, String password) throws RuntimeException {
    AsyncPushTask pushTask = new AsyncPushTask(remote, password);
    pushTask.execute();
    try {
      if (pushTask.get() == 1) throw pushTask.ex;
    } catch (Exception ex) {
      throw new RuntimeException("Exception while pushing", ex);
    }
  }

  public static File getRepoDirectory(Context currentActivity) {
    File f = new File(currentActivity.getExternalFilesDir(null), "repositories");
    f.mkdirs();
    return f;
  }

  public static boolean isAvailable() {
    if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) return true;
    else return false;
  }
}
