package bowser;

import static com.google.common.base.Preconditions.checkNotNull;
import static ox.util.Utils.checkNotEmpty;
import static ox.util.Utils.normalize;
import static ox.util.Utils.propagate;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.SocketConnection;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

import bowser.handler.ExceptionHandler;
import bowser.handler.RouteHandler;
import bowser.handler.StaticContentHandler;
import bowser.node.DomNode;
import bowser.node.Head;
import bowser.template.Imports;
import bowser.template.Template;
import ox.Log;

public class WebServer {

  public final int port;
  public final boolean developerMode;
  public final List<Controller> controllers = Lists.newArrayList();

  private final List<RequestHandler> handlers = Lists.newArrayList();
  private StaticContentHandler staticContentHandler;

  private SSLContext sslContext;

  private WebLogger logger = new DefaultWebLogger();

  private RequestHandler notFoundHandler = null;
  private ExceptionHandler exceptionHandler = (a, b, c) -> false;

  private final Head head;

  public WebServer(String appName, int port, boolean developerMode) {
    this.port = port;
    this.developerMode = developerMode;
    this.staticContentHandler = new StaticContentHandler(this);
    head = Head.defaults(appName);
  }

  public WebServer mobileDisplay() {
    Template.mobileDisplay = true;
    return this;
  }

  public WebServer shortcut(String nickname, String fullName) {
    Imports.shortcut(nickname, fullName);
    return this;
  }

  public WebServer controller(Controller controller) {
    controllers.add(controller);
    controller.init(this);
    return this;
  }

  public WebServer add(RequestHandler handler) {
    handlers.add(handler);
    return this;
  }

  public WebServer exceptionHandler(ExceptionHandler handler) {
    this.exceptionHandler = checkNotNull(handler);
    return this;
  }

  public WebServer notFoundHandler(RequestHandler handler) {
    this.notFoundHandler = handler;
    return this;
  }

  public void add(Route route) {
    handlers.add(new RouteHandler(route));
  }

  public WebServer ssl(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  public WebServer logger(WebLogger logger) {
    this.logger = logger;
    return this;
  }

  public WebServer googleAnalytics(String googleAnalyticsId) {
    checkNotEmpty(normalize(googleAnalyticsId));

    head.add(new DomNode("script").attribute("src",
        "https://www.googletagmanager.com/gtag/js?id=" + googleAnalyticsId));
    head.add(new DomNode("script").text("window.dataLayer = window.dataLayer || [];\n" +
        "function gtag(){dataLayer.push(arguments);}\n" +
        "gtag('js', new Date());\n" +
        "gtag('config', '" + googleAnalyticsId + "');"));

    return this;
  }

  public StaticContentHandler getResourceLoader() {
    return staticContentHandler;
  }

  public Head getHead() {
    return head;
  }

  private void handle(Request request, Response response) {
    try {
      boolean handled = false;

      response.header("Access-Control-Allow-Origin", request.request.getValue("Origin"));
      response.header("X-Frame-Options", "SAMEORIGIN");
      if (sslContext != null) {
        long ONE_YEAR = TimeUnit.DAYS.toSeconds(365);
        response.header("Strict-Transport-Security", "max-age=" + ONE_YEAR + "; includeSubDomains");
      }

      String s = normalize(request.getHeader("Accept-Encoding"));
      if (s.contains("gzip")) {
        response.setCompressed(true);
      }

      for (RequestHandler handler : handlers) {
        if (handler.process(request, response)) {
          handled = true;
          break;
        }
      }

      if (!handled) {
        if (request.isStaticResource()) {
          handled = staticContentHandler.process(request, response);
        }
      }

      if (!handled) {
        Log.info("Not found: " + request);
        if (notFoundHandler == null) {
          response.status(Status.NOT_FOUND);
        } else {
          if (!notFoundHandler.process(request, response)) {
            response.status(Status.NOT_FOUND);
          }
        }
      }

      response.close();
    } catch (Exception e) {
      if (!exceptionHandler.handle(request, response, e)) {
        throw e;
      }
    }
  }

  private final Container container = new Container() {
    @Override
    public void handle(org.simpleframework.http.Request request, org.simpleframework.http.Response response) {
      Stopwatch watch = Stopwatch.createStarted();
      Request req = new Request(request);
      Response resp = new Response(response);
      Throwable t = null;
      try {
        WebServer.this.handle(req, resp);
      } catch (final Throwable e) {
        t = e;
        Throwable root = Throwables.getRootCause(e);
        if (!"Stream has been closed".equals(root.getMessage()) && !"Broken pipe".equals(root.getMessage())) {
          e.printStackTrace();
          response.setStatus(Status.INTERNAL_SERVER_ERROR);
          try {
            String message = "Server Error";
            if (!Strings.isNullOrEmpty(root.getMessage())) {
              message = root.getMessage();
            }
            resp.contentType("text/plain");
            resp.write(message);
          } catch (Exception e1) {
            e1.printStackTrace();
          }
        }
      }
      try {
        logger.log(req, resp, Optional.ofNullable(t), watch);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  };

  @SuppressWarnings("resource")
  public WebServer start() {
    for (RequestHandler handler : handlers) {
      handler.load();
    }

    try {
      Server server = new ContainerServer(container);
      new SocketConnection(server).connect(new InetSocketAddress(port), sslContext);
    } catch (Exception e) {
      throw propagate(e);
    }

    return this;
  }

  public static WebServer redirectToHttps() {
    return redirect(80, 443);
  }

  public static WebServer redirect(int fromPort, int toPort) {
    return new WebServer("Redirect", fromPort, false).add((request, response) -> {
      String host = request.getHost();
      String path = request.request.getTarget();
      if (toPort == 443) {
        response.redirect("https://" + host + path);
      } else if (toPort == 80) {
        response.redirect("http://" + host + path);
      } else {
        response.redirect("http://" + host + toPort + ":" + toPort + path);
      }
      return true;
    }).start();
  }

}
