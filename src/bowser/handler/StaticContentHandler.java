package bowser.handler;

import static ox.util.Utils.propagate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.simpleframework.http.Status;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import bowser.Controller;
import bowser.Request;
import bowser.RequestHandler;
import bowser.Response;
import bowser.SCSSProcessor;
import bowser.WebServer;
import ox.IO;
import ox.Log;
import ox.Pair;

public class StaticContentHandler implements RequestHandler {

  private static final byte[] NO_DATA = new byte[0];

  private WebServer server;
  private final Map<String, byte[]> cache = Maps.newConcurrentMap();
  private final SCSSProcessor scssProcessor;

  public StaticContentHandler(WebServer server) {
    this.server = server;
    this.scssProcessor = new SCSSProcessor(this, server.enableCaching);
  }

  @Override
  public boolean process(Request request, Response response) {
    byte[] data = getData(request.getOriginalPath());

    if (data == null) {
      return false;
    }

    String path = request.path;

    if (path.endsWith(".css") || path.endsWith(".scss")) {
      response.contentType("text/css");
    } else if (path.endsWith(".js")) {
      response.contentType("text/javascript");
    } else if (path.endsWith(".mp4")) {
      response.setCompressed(false);
      response.contentType("video/mp4");
    } else if (path.endsWith(".svg")) {
      response.contentType("image/svg+xml");
    }

    if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".woff2") || path.endsWith(".ttf")
        || path.endsWith(".gif") || path.endsWith(".otf") || path.endsWith(".woff")) {
      response.setCompressed(false);
      if (server.enableCaching) {
        response.cacheFor(1, TimeUnit.DAYS);
      }
    } else if (path.endsWith(".svg")) {
      if (server.enableCaching) {
        response.cacheFor(1, TimeUnit.DAYS);
      }
    } else if (path.endsWith(".css") || path.endsWith(".scss") || path.endsWith(".js") || path.endsWith(".min.map")) {
      if (server.enableCaching) {
        response.cacheFor(20, TimeUnit.MINUTES);
      }
    }

    if (path.endsWith(".scss")) {
      data = scssProcessor.process(request.getOriginalPath(), data);
    }

    Pair<Long, Long> range = request.getRange();
    InputStream is;

    response.header("Accept-Ranges", "bytes");
    if (range == null) {
      if (!response.isCompressed()) {
        response.header("Content-Length", data.length + "");
      }
      is = new ByteArrayInputStream(data);
    } else {
      response.status(Status.PARTIAL_CONTENT);
      Long end = range.b;
      if (end == null) {
        end = (long) data.length - 1;
      }
      long len = end - range.a + 1;
      if (!response.isCompressed()) {
        response.header("Content-Length", len + "");
      }
      response.header("Content-Range", "bytes " + range.a + "-" + end + "/" + data.length);
      is = new ByteArrayInputStream(data, range.a.intValue(), (int) len);
    }

    try {
      IO.from(is).to(response.getOutputStream());
    } catch (Throwable e) {
      e = Throwables.getRootCause(e);
      if (e instanceof ClosedChannelException || "Stream has been closed".equals(e.getMessage())
          || "Socket closed".equals(e.getMessage())) {
        // ignore
      } else if ("Response content complete".equals(e.getMessage())) {
        // ignore
      } else {
        Log.debug("Problem processing: " + path);
        Log.debug("type: " + e.getClass());
        Log.debug("message: " + e.getMessage());
        throw propagate(e);
      }
    }

    return true;
  }

  public byte[] getData(String path) {
    return getData(path, null);
  }

  public byte[] getData(String path, Controller controller) {
    byte[] data = cache.get(path);

    if (data == NO_DATA) {
      return null;
    }

    if (data == null) {
      data = load(path, controller);
      if (server.enableCaching) {
        cache.put(path, data);
      }
    }

    if (data == NO_DATA) {
      return null;
    }

    return data;
  }

  private byte[] load(String path, Controller controller) {
    if (path.startsWith("/")) {
      path = path.substring(1);
    }

    // first try to look for the resource in the directory of the given Controller
    if (controller != null) {
      URL url = controller.getResource(path);
      if (url != null) {
        return IO.from(url).toByteArray();
      }
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

  public WebServer getServer() {
    return server;
  }

}
