package bowser.handler;

import jasonlib.IO;
import java.net.URL;
import java.util.Map;
import bowser.Controller;
import bowser.Request;
import bowser.RequestHandler;
import bowser.Response;
import bowser.WebServer;
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

    if (request.path.endsWith(".css")) {
      response.contentType("text/css");
    } else if (request.path.endsWith(".js")) {
      response.contentType("text/javascript");
    }

    IO.from(data).to(response.getOutputStream());

    return true;
  }

  public byte[] getData(String path) {
    byte[] data = cache.get(path);

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
      URL url = c.getClass().getResource(path);
      if (url != null) {
        return IO.from(url).toByteArray();
      }
    }
    return null;
  }

}
