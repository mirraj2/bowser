package bowser.handler;

import java.io.IOException;
import java.util.List;

import com.google.common.base.Splitter;

import bowser.Controller;
import bowser.Handler;
import ox.Json;
import ox.Log;

public class GitHooks extends Controller {

  private String updatePath;

  public GitHooks(String updatePath) {
    this.updatePath = updatePath;
  }

  @Override
  public void init() {
    route("POST", "/updateServer").to(updateServer);
  }

  // TODO secure this route to verify it is coming from github
  private Handler updateServer = (request, response) -> {
    Json json = new Json(request.getContent());

    String ref = json.get("ref"); // "ref": "refs/heads/test"

    if (ref == null || !ref.equals("refs/heads/master")) {
      Log.info("Not a master branch update: " + ref);
      return;
    }

    String repo = json.getJson("repository").get("name");
    List<Json> commits = json.getJson("commits").asJsonArray();

    boolean restart = false;

    // see if any commit message has the deployment keyword
    for (Json commit : commits) {
      restart |= handleCommit(repo, commit);
    }

    if (restart) {
      restart();
    } else {
      Log.info("Not a #deploy commit.");
    }
  };

  protected boolean handleCommit(String repo, Json commit) {
    boolean restart = false;
    String message = commit.get("message");

    for (String word : Splitter.on(" ").split(message)) {
      if (word.startsWith("#")) {
        String tag = word.substring(1);
        if (tag.equalsIgnoreCase("deploy")) {
          restart = true;
        }
      }
    }

    return restart;
  }

  private void restart() {
    Log.info("Restarting server.");
    try {
      // File out = new File(OS.getHomeFolder(), "restart-log.txt");
      // new ProcessBuilder(updatePath)
      // .redirectOutput(out)
      // .redirectError(out)
      // .start();
      Runtime.getRuntime().exec(updatePath);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
