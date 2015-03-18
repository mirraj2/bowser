package bowser;

import java.net.URL;

public abstract class Controller {

  private WebServer server;

  public final void init(WebServer server) {
    this.server = server;
    init();
  }

  public abstract void init();

  protected Route route(String method, String path) {
    Route ret = new Route(this, method, path, !server.developerMode);
    server.add(ret);
    return ret;
  }

  public WebServer getServer() {
    return server;
  }

  public URL getResource(String path) {
    if (path.endsWith(".js")) {
      path = getJsFolder() + path;
    }
    return getClass().getResource(path);
  }

  protected String getJsFolder() {
    return "";
  }

}
