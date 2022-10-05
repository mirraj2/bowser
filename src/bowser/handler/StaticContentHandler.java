package bowser.handler;

import static ox.util.Utils.propagate;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.simpleframework.http.Status;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

import bowser.BowserWebServer;
import bowser.misc.SCSSProcessor;
import bowser.model.Controller;
import bowser.model.Request;
import bowser.model.RequestHandler;
import bowser.model.Response;
import bowser.template.Template;
import ox.IO;
import ox.Log;
import ox.Pair;

public class StaticContentHandler implements RequestHandler {

  private static final byte[] NO_DATA = new byte[0];

  private BowserWebServer server;
  private final Map<String, byte[]> cache = Maps.newConcurrentMap();
  private final SCSSProcessor scssProcessor;
  private boolean enableCaching = true;

  public StaticContentHandler(BowserWebServer server) {
    this.server = server;
    this.scssProcessor = new SCSSProcessor(this, enableCaching);
  }

  @Override
  public boolean process(Request request, Response response) {
    String path = request.path;
    boolean jsWrappedCss = path.endsWith(".scss.js");

    if (jsWrappedCss) {
      path = server.getCacheBuster().unhashPath(request.path);
      path = path.substring(0, path.length() - 3);
    }

    byte[] data = getData(path);

    if (data == null) {
      return false;
    }

    if (path.endsWith(".css") || path.endsWith(".scss")) {
      response.contentType("text/css");
    } else if (path.endsWith(".js")) {
      response.contentType("text/javascript");
    } else if (path.endsWith(".mjs")) {
      data = server.getCacheBuster().hashMJSImports(data).getBytes(StandardCharsets.UTF_8);
      response.contentType("text/javascript");
    } else if (path.endsWith(".mp4")) {
      response.setCompressed(false);
      response.contentType("video/mp4");
    } else if (path.endsWith(".svg")) {
      response.contentType("image/svg+xml");
    }

    if (path.endsWith(".jpg") || path.endsWith(".png") || path.endsWith(".webp") || path.endsWith(".woff2") || path.endsWith(".ttf")
        || path.endsWith(".gif") || path.endsWith(".otf") || path.endsWith(".woff")) {
      response.setCompressed(false);
      if (enableCaching) {
        response.cacheFor(1, TimeUnit.DAYS);
      }
    } else if (path.endsWith(".svg")) {
      if (enableCaching) {
        response.cacheFor(1, TimeUnit.DAYS);
      }
    } else if (path.endsWith(".js") || path.endsWith(".mjs") || path.endsWith(".css") || path.endsWith(".scss")) {
      if (enableCaching) {
        // because we have cache busting for these files, we can set the longest possible cache duration
        response.cacheFor(365, TimeUnit.DAYS);
      }
    }

    if (path.endsWith(".scss")) {
      data = scssProcessor.process(request.getOriginalPath(), data);
      if (jsWrappedCss) {
        String wrapped = "window.importCSS(`/* " + path + " */\n" + new String(data, StandardCharsets.UTF_8) + "`);";
        data = wrapped.getBytes(StandardCharsets.UTF_8);
        response.contentType("text/javascript");
      }
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
    path = server.getCacheBuster().unhashPath(path);

    byte[] data = cache.get(path);

    if (data == NO_DATA) {
      return null;
    }

    if (data == null) {
      data = load(path, controller);
      if (enableCaching) {
        cache.put(path, data);
      }
    }

    if (data == NO_DATA) {
      return null;
    }

    return data;
  }

  private byte[] load(String path, Controller controller) {
    if (path.charAt(0) == '/') {
      path = path.substring(1);
    }

    // first try to look for the resource in the directory of the given Controller
    if (controller != null) {
      URL url = controller.getResource(path);
      if (url != null) {
        return IO.from(url).toByteArray();
      }
    }

    URL url = pathToUrl(path);
    if (url != null) {
      return IO.from(url).toByteArray();
    }

    if (path.equals("bowser.js")) {
      return IO.from(Template.class, "bowser.js").toByteArray();
    }

    Log.debug("Couldn't find: " + path);
    return NO_DATA;
  }

  public URL pathToUrl(String path) {
    if (path.charAt(0) == '/') {
      path = path.substring(1);
    }

    for (Controller c : server.controllers) {
      URL url = c.getResource(path);
      if (url != null) {
        return url;
      }
    }
    return null;
  }

  public StaticContentHandler setCachingEnabled(boolean b) {
    this.enableCaching = b;
    scssProcessor.setCachingEnabled(b);
    if (!b) {
      cache.clear();
    }
    return this;
  }

  public BowserWebServer getServer() {
    return server;
  }

  public SCSSProcessor getScssProcessor() {
    return scssProcessor;
  }

}
