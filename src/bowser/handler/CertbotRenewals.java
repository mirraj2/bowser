package bowser.handler;

import java.io.File;

import bowser.Controller;
import bowser.Handler;
import ox.Log;
import ox.OS;

public class CertbotRenewals extends Controller {

  @Override
  public void init() {
    route("GET", "/.well-known/acme-challenge/*").to(handler);
  }

  private final Handler handler = (request, response) -> {
    String key = request.getSegment(2);
    Log.debug("Got certbot request: " + key);

    File dir = new File(OS.getAppFolder("ender"), "certbot");
    for (File child : dir.listFiles()) {
      Log.debug(child);
    }

    Log.debug("done.");
  };

}
