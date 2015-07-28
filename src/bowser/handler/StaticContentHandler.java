package bowser.handler;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.simpleframework.http.Status;
import ox.IO;
import ox.Log;
import ox.Pair;
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
    } else if (path.endsWith(".mp4")) {
      response.contentType("video/mp4");
    }

    if (!server.developerMode) {
      if (path.endsWith(".jpg") || path.endsWith(".png")) {
        response.cacheFor(1, TimeUnit.DAYS);
      } else if (path.endsWith(".css") || path.endsWith(".js")) {
        response.cacheFor(20, TimeUnit.MINUTES);
      }
    }

    Pair<Long, Long> range = request.getRange();
    InputStream is;

    response.header("Accept-Ranges", "bytes");
    if (range == null) {
      response.header("Content-Length", data.length + "");
      is = new ByteArrayInputStream(data);
    } else {
      response.status(Status.PARTIAL_CONTENT);
      Long end = range.b;
      if (end == null) {
        end = (long) data.length - 1;
      }
      long len = end - range.a + 1;
      response.header("Content-Length", len + "");
      response.header("Content-Range", "bytes " + range.a + "-" + end + "/" + data.length);
      is = new ByteArrayInputStream(data, range.a.intValue(), (int) len);
    }

    try {
      IO.from(is).to(response.getOutputStream());
    } catch (Throwable e) {
      e = Throwables.getRootCause(e);
      if (e.getMessage().equals("Stream has been closed")) {
        // ignore this
      } else if (e.getMessage().equals("Response content complete")) {
        //ignore
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
