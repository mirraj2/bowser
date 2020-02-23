package bowser.handler;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;

import bowser.Controller;
import bowser.Handler;
import ox.IO;
import ox.Log;
import ox.OS;

public class CertbotRenewals extends Controller {

  private static final File appDir = OS.getAppFolder("ender");

  @Override
  public void init() {
    route("GET", "/.well-known/acme-challenge/*").to(handler);
  }

  // certbot certonly --webroot -w /root/.ender -d files.ender.com
  private final Handler handler = (request, response) -> {
    String key = request.getSegment(2);
    Log.debug("Got certbot request: " + key);

    checkState(!key.contains("/") && !key.contains("."));

    File file = new File(appDir, ".well-known/acme-challenge/" + key);
    checkState(file.exists());

    IO.from(file).to(response.getOutputStream());
  };

}
