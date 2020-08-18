package bowser.handler;

import static com.google.common.base.Preconditions.checkState;

import bowser.model.Controller;
import bowser.model.Handler;
import ox.File;
import ox.IO;
import ox.Log;

public class CertbotRenewals extends Controller {

  private File rootFolder;

  public CertbotRenewals() {
    this(File.home());
  }

  public CertbotRenewals(File rootFolder) {
    this.rootFolder = rootFolder;
  }

  @Override
  public void init() {
    route("GET", "/.well-known/acme-challenge/*").to(handler);
  }

  // certbot certonly --webroot -w ~/ -d files.ender.com
  private final Handler handler = (request, response) -> {
    String key = request.getSegment(2);

    checkState(!key.contains("/") && !key.contains("."));

    File dir = rootFolder.child(".well-known/acme-challenge");
    File file = findFile(dir, key);
    if (!file.exists()) {
      Log.debug(key);
      Log.debug(dir + " exists: " + dir.exists());
      Log.debug("All files in dir:");
      for (File f : dir.children()) {
        Log.debug(f);
      }
      throw new RuntimeException("Could not find certbot renewal file: " + file);
    }

    IO.from(file).to(response.getOutputStream());
  };

  private File findFile(File dir, String name) {
    for (File file : dir.children()) {
      if (file.getName().equalsIgnoreCase(name)) {
        return file;
      }
    }
    return dir.child(name);
  }

}
