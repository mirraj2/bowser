package bowser.handler;

import jasonlib.IO;
import jasonlib.Log;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import bowser.Controller;
import bowser.Request;
import bowser.RequestHandler;
import bowser.Response;
import bowser.WebServer;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

public class StaticContentHandler implements RequestHandler {

  private static final byte[] NO_DATA = new byte[0];

  private WebServer server;
  private final Map<String, byte[]> cache = Maps.newConcurrentMap();

  public StaticContentHandler(WebServer server) {
    this.server = server;
  }

  @Override
  public boolean process(Request request, Response response) {
    byte[] data = getData(request.path);

    if (data == null || data == NO_DATA) {
      return false;
    }

    String path = request.path;

    if (path.endsWith(".css")) {
      response.contentType("text/css");
    } else if (path.endsWith(".js")) {
      response.contentType("text/javascript");
    }

    if (!server.developerMode) {
      if (path.endsWith(".jpg") || path.endsWith(".png")) {
        response.cacheFor(1, TimeUnit.DAYS);
      } else if (path.endsWith(".css") || path.endsWith(".js")) {
        response.cacheFor(20, TimeUnit.MINUTES);
      }
    }

    try {
      IO.from(data).to(response.getOutputStream());
    } catch (Throwable e) {
      e = Throwables.getRootCause(e);
      if (e.getMessage().equals("Stream has been closed")) {
        // ignore this
      } else {
        throw Throwables.propagate(e);
      }
    }

    return true;
  }

  public byte[] getData(String path) {
    byte[] data = cache.get(path);

    if (data == NO_DATA) {
      return null;
    }

    if (data == null) {
      data = load(path);
      if (!server.developerMode) {
        cache.put(path, data);
      }
    }

    return data;
  }

  private byte[] load(String path) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    for (Controller c : server.controllers) {
      URL url = c.getResource(path);
      if (url != null) {
        return IO.from(url).toByteArray();
      }
    }
    Log.debug("Couldn't find: " + path);
    return NO_DATA;
  }

}
