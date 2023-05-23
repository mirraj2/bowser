package bowser;

import static com.google.common.base.Preconditions.checkNotNull;
import static ox.util.Utils.checkNotEmpty;
import static ox.util.Utils.normalize;
import static ox.util.Utils.propagate;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
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
import com.google.common.collect.Maps;

import bowser.handler.ExceptionHandler;
import bowser.handler.PortRedirect;
import bowser.handler.RouteHandler;
import bowser.handler.StaticContentHandler;
import bowser.misc.CacheBuster;
import bowser.misc.DefaultWebLogger;
import bowser.misc.UserReadableError;
import bowser.misc.WebLogger;
import bowser.model.Controller;
import bowser.model.Request;
import bowser.model.RequestHandler;
import bowser.model.Response;
import bowser.model.Route;
import bowser.node.DomNode;
import bowser.node.Head;
import bowser.template.Template;

import ox.Log;
import ox.Threads;
import ox.x.XList;
import ox.x.XMultimap;
import ox.x.XOptional;

public class BowserWebServer {

  public static boolean debugHandlers = false;
  private static InheritableThreadLocal<Request> currentRequest = new InheritableThreadLocal<>();

  public final int port;
  public final boolean enableCaching;
  public boolean showImportComments = false;
  public boolean includeRouteDebugInfo = false;
  public final List<Controller> controllers = Lists.newArrayList();

  private final List<RequestHandler> handlers = Lists.newArrayList();
  private final Map<String, Controller> routeControllers = Maps.newHashMap();

  private StaticContentHandler staticContentHandler;

  private SSLContext sslContext;

  private WebLogger logger = new DefaultWebLogger();

  private RequestHandler notFoundHandler = null;
  private ExceptionHandler exceptionHandler = (a, b, c) -> false;

  private final Head head;

  private final CacheBuster cacheBuster;

  public BowserWebServer(String appName, int port, boolean enableCaching) {
    this.port = port;
    this.enableCaching = enableCaching;
    this.staticContentHandler = new StaticContentHandler(this);
    this.cacheBuster = new CacheBuster(staticContentHandler);
    head = Head.defaults(appName);
  }

  public BowserWebServer mobileDisplay() {
    Template.mobileDisplay = true;
    return this;
  }

  public BowserWebServer controller(Controller controller) {
    controllers.add(controller);
    controller.init(this);
    return this;
  }

  public BowserWebServer controller(Controller... controllers) {
    for (Controller c : controllers) {
      controller(c);
    }
    return this;
  }

  public BowserWebServer add(RequestHandler handler) {
    handlers.add(handler);
    return this;
  }

  public BowserWebServer add(RequestHandler... handlers) {
    for (RequestHandler handler : handlers) {
      add(handler);
    }
    return this;
  }

  /**
   * Add an exception handler whose handle() function will be called if any handlers throw an exception.
   */
  public BowserWebServer exceptionHandler(ExceptionHandler handler) {
    this.exceptionHandler = checkNotNull(handler);
    return this;
  }

  public BowserWebServer notFoundHandler(RequestHandler handler) {
    this.notFoundHandler = handler;
    return this;
  }

  public void add(Route route) {
    add(new RouteHandler(route));
  }

  public BowserWebServer ssl(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  public BowserWebServer logger(WebLogger logger) {
    this.logger = logger;
    return this;
  }

  public BowserWebServer showImportComments() {
    this.showImportComments = true;
    return this;
  }

  public BowserWebServer includeRouteDebugInfo() {
    this.includeRouteDebugInfo = true;
    return this;
  }

  public BowserWebServer googleAnalytics(String googleAnalyticsId) {
    return googleAnalytics(googleAnalyticsId, "");
  }

  public BowserWebServer googleAnalytics(String googleAnalyticsId, String adwordsTagId) {
    checkNotEmpty(normalize(googleAnalyticsId));

    head.add(new DomNode("script").attribute("async").attribute("src",
        "https://www.googletagmanager.com/gtag/js?id=" + googleAnalyticsId));

    String script = "window.dataLayer = window.dataLayer || [];\n" +
        "function gtag(){dataLayer.push(arguments);}\n" +
        "gtag('js', new Date());\n" +
        "gtag('config', '" + googleAnalyticsId + "');\n";

    if (!adwordsTagId.isEmpty()) {
      script += "gtag('config', '" + adwordsTagId + "');\n";
    }

    head.add(new DomNode("script").text(script));

    return this;
  }

  public StaticContentHandler getResourceLoader() {
    return staticContentHandler;
  }

  public CacheBuster getCacheBuster() {
    return cacheBuster;
  }

  public Head getHead() {
    return head;
  }

  public void processRequest(Request request, Response response) {
    Stopwatch watch = Stopwatch.createStarted();
    try {
      currentRequest.set(request);
      routeToHandler(request, response);
    } catch (final Throwable e) {
      Throwable root = Throwables.getRootCause(e);
      if (!"Stream has been closed".equals(root.getMessage()) && !"Broken pipe".equals(root.getMessage())) {
        e.printStackTrace();
        response.status(Status.INTERNAL_SERVER_ERROR);
        try {
          String message;
          if (e instanceof UserReadableError) {
            message = e.getMessage();
          } else if (!Strings.isNullOrEmpty(root.getMessage())) {
            message = root.getMessage();
          } else {
            message = "Server Error";
          }
          response.contentType("text/plain");
          response.write(message);
        } catch (Exception e1) {
          e1.printStackTrace();
        }
      }
    } finally {
      currentRequest.set(null);
      try {
        response.close();
      } catch (Throwable t) {
        Log.debug("Problem closing output: " + request);
        t.printStackTrace();
      }
      cacheBuster.onRequestFinished();
    }
    try {
      logger.log(request, response, watch);
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public void routeToHandler(Request request, Response response) {
    RequestHandler lastHandler = null;
    try {
      boolean handled = false;

      response.header("Access-Control-Allow-Origin", request.request.getValue("Origin"));
      response.header("Access-Control-Allow-Credentials", "true");
      response.header("Access-Control-Expose-Headers", "Content-Disposition");
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
        lastHandler = handler;
        if (handler.process(request, response)) {
          handled = true;
          if (debugHandlers) {
            if (handler instanceof RouteHandler) {
              RouteHandler route = (RouteHandler) handler;
              Controller controller = route.getRoute().controller;
              Log.debug("request handled by " + controller + " :: " + route);
            } else {
              Log.debug("request handled by " + handler + " :: " + handler.getClass());
            }
          }
          break;
        }
      }
      lastHandler = null;

      if (!handled) {
        if (request.isStaticResource()) {
          handled = staticContentHandler.process(request, response);
        }
        if (!handled) {
          Log.info("Not found: " + request);
          response.status(Status.NOT_FOUND);
          if (notFoundHandler != null) {
            handled = notFoundHandler.process(request, response);
          }
          if (!handled) {
            response.write("Not found: " + request);
            handled = true;
          }
        }
      }

    } catch (Throwable t) {
      response.exception = t;
      if (!exceptionHandler.handle(request, response, lastHandler)) {
        throw t;
      }
    }
  }



  private final Container container = new Container() {
    @Override
    public void handle(org.simpleframework.http.Request request, org.simpleframework.http.Response response) {
      processRequest(new Request(request), new Response(response));
    }
  };

  @SuppressWarnings("resource")
  public BowserWebServer start() {
    checkForDuplicatePaths();

    if (enableCaching) {
      // warm the cache
      preloadHandlers();
    } else {
      Threads.run(() -> {
        // we are only running this to surface any errors loading any of the routes.
        preloadHandlers();
      });
    }

    if (!this.enableCaching) {
      getCacheBuster().requestBasedCache();
      staticContentHandler.setCachingEnabled(false);
    }

    try {
      Server server = new ContainerServer(container);
      new SocketConnection(server).connect(new InetSocketAddress(port), sslContext);
    } catch (Exception e) {
      throw propagate(e);
    }

    return this;
  }

  private void preloadHandlers() {
    XMultimap<String, RequestHandler> resourceHandlers = XList.create(handlers).indexMultimap(handler -> {
      if (handler instanceof RouteHandler) {
        RouteHandler rh = (RouteHandler) handler;
        return rh.getRoute().resource;
      }
      return null;
    });

    Threads.get(8).input(resourceHandlers.keySet().toList().shuffleSelf()).run(resource -> {
      XList<RequestHandler> handlers = resourceHandlers.get(resource);
      handlers.forEach(handler -> {
        handler.load();
      });
      handlers.forEach(handler -> handler.load());
    });
  }

  private void checkForDuplicatePaths() {
    for (RequestHandler handler : handlers) {
      if (handler instanceof RouteHandler) {
        Route route = ((RouteHandler) handler).getRoute();
        String path = route.method + " " + route.path.toLowerCase();
        if (!route.host.isEmpty()) {
          path = route.host + " " + path;
        }
        Controller existingController = routeControllers.put(path, route.controller);
        if (existingController != null) {
          if (existingController == route.controller) {
            throw new RuntimeException(
                existingController.getClass().getSimpleName() + " tried to register the same path twice: " + path);
          }
          throw new RuntimeException(String.format("Multiple controllers tried to register path [%s]: %s and %s",
              path, existingController.getClass().getSimpleName(), route.controller.getClass().getSimpleName()));
        }
      }
    }
  }

  /**
   * Gets the Request associated with the current Thread.
   */
  public static XOptional<Request> getCurrentRequest() {
    return XOptional.ofNullable(currentRequest.get());
  }

  public static BowserWebServer redirectToHttps() {
    return redirect(80, 443);
  }

  public static BowserWebServer redirect(int fromPort, int toPort) {
    return new BowserWebServer("Redirect", fromPort, false)
        .add(new PortRedirect(toPort))
        .start();
  }

}
