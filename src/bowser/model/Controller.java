package bowser.model;

import static ox.util.Utils.getExtension;

import java.net.URL;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import bowser.BowserWebServer;

public abstract class Controller {

  protected BowserWebServer server;
  private final Map<String, String> folders = Maps.newHashMap();
  private final List<Route> routes = Lists.newArrayList();

  public final void init(BowserWebServer server) {
    this.server = server;
    init();
  }

  public abstract void init();

  protected Route route(String method, String path) {
    return addRoute(new Route(this, method, path, server.enableCaching));
  }

  protected Route addRoute(Route route) {
    routes.add(route);
    server.add(route);
    return route;
  }

  public BowserWebServer getServer() {
    return server;
  }

  public void mapFolders(String... extensions) {
    for (String s : extensions) {
      mapFolder(s, s);
    }
  }

  public void mapFolder(String extension, String folder) {
    folders.put(extension, folder);
  }

  public byte[] getData(String path) {
    return server.getResourceLoader().getData(path, this);
  }

  public URL getResource(String path) {
    String folder = folders.get(getExtension(path));
    if (folder != null) {
      path = folder + "/" + path;
    }
    return getClass().getResource(path);
  }

  public List<Route> getRoutes() {
    return routes;
  }

}
