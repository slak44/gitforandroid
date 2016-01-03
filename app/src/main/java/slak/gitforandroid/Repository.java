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

  public void gitInit() throws GitAPIException, IOException {
    if (alreadyExists)
      throw new WrongRepositoryStateException("Failed to init: Repository " + name + " already exists");
    git.getRepository().create();
    alreadyExists = true;
  }

  public void gitClone(final String uri, AsyncGitTask.AsyncTaskCallback callback) {
    AsyncGitTask clone = new AsyncGitTask(callback) {
      @Override
      public void safelyDoInBackground() throws Exception {
        if (alreadyExists)
          throw new WrongRepositoryStateException("Failed to clone: Repository " + name + " already exists");
        CloneCommand clCom = new CloneCommand();
        clCom.setCloneAllBranches(true);
        clCom.setDirectory(thisRepo);
        clCom.setURI(uri);
        clCom.call();
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

  public void gitCommit(
      final PersonIdent author,
      final PersonIdent committer,
      final String message,
      AsyncGitTask.AsyncTaskCallback callback
  ) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    final PersonIdent stored = new PersonIdent(
        prefs.getString("git_name", ""), prefs.getString("git_email", ""));
    AsyncGitTask commit = new AsyncGitTask(callback) {
      @Override
      public void safelyDoInBackground() throws Exception {
        git.commit()
            .setAuthor(author == null ? stored : author)
            .setCommitter(committer == null ? stored : committer)
            .setMessage(message)
            .call();
      }
    };
    commit.execute();
  }

  public void gitCommit(
      PersonIdent committer,
      String message,
      AsyncGitTask.AsyncTaskCallback callback
  ) {
    gitCommit(committer, committer, message, callback);
  }

  public void gitCommit(
      String name,
      String email,
      String message,
      AsyncGitTask.AsyncTaskCallback callback
  ) {
    PersonIdent pi;
    if (name == null || email == null) pi = null;
    else pi = new PersonIdent(name, email);
    gitCommit(pi, pi, message, callback);
  }

  public void gitPush(
      final String remote,
      final String password,
      AsyncGitTask.AsyncTaskCallback callback
  ) {
    AsyncGitTask push = new AsyncGitTask(callback) {
      @Override
      public void safelyDoInBackground() throws Exception {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        UsernamePasswordCredentialsProvider upcp = new UsernamePasswordCredentialsProvider(
            prefs.getString("git_username", ""),
            password);
        git.push().setRemote(remote).setCredentialsProvider(upcp).setPushAll().call();
      }
    };
    push.execute();
  }

  public File getRepoFolder() {
    return thisRepo;
  }

  public static File getRepoDirectory(Context currentActivity) {
    File f = new File(currentActivity.getExternalFilesDir(null), "repositories");
    f.mkdirs();
    return f;
  }

  public static boolean isAvailable() {
    return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
  }
}
