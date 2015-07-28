package bowser;

import jasonlib.IO;
import jasonlib.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import org.simpleframework.http.Status;
import org.simpleframework.http.core.Container;
import org.simpleframework.http.core.ContainerServer;
import org.simpleframework.transport.Server;
import org.simpleframework.transport.connect.SocketConnection;
import bowser.handler.RouteHandler;
import bowser.handler.StaticContentHandler;
import bowser.template.Imports;
import bowser.template.Template;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class WebServer {

  public final int port;
  public final boolean developerMode;
  public final List<Controller> controllers = Lists.newArrayList();

  private final List<RequestHandler> handlers = Lists.newArrayList();
  private StaticContentHandler staticContentHandler;

  private SSLContext sslContext;

  private WebLogger logger = emptyLogger();

  public WebServer(String appName, int port, boolean developerMode) {
    Template.appName = appName;
    this.port = port;
    this.developerMode = developerMode;
    this.staticContentHandler = new StaticContentHandler(this);
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

  public StaticContentHandler getResourceLoader() {
    return staticContentHandler;
  }

  private void handle(Request request, Response response) {
    Stopwatch watch = Stopwatch.createStarted();

    try {
      boolean handled = false;

      response.header("Access-Control-Allow-Origin", request.request.getValue("Origin"));

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
        response.status(Status.NOT_FOUND);
      }

      response.close();
    } finally {
      Log.debug("Handled request: " + request + " in " + watch);
    }
  }

  private final Container container = new Container() {
    @Override
    public void handle(org.simpleframework.http.Request request, org.simpleframework.http.Response response) {
      Request req = new Request(request);
      Response resp = new Response(response);
      Throwable t = null;
      try {
        WebServer.this.handle(req, resp);
      } catch (Throwable e) {
        t = e;
        e.printStackTrace();
        response.setStatus(Status.INTERNAL_SERVER_ERROR);
        try {
          Throwable root = Throwables.getRootCause(e);
          String message = "Server Error";
          if (!Strings.isNullOrEmpty(root.getMessage())) {
            message = root.getMessage();
          }
          IO.from(message).to(response.getOutputStream());
        } catch (IOException e1) {
          e1.printStackTrace();
        }
      }
      try {
        logger.log(req, resp, Optional.ofNullable(t));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  };

  @SuppressWarnings("resource")
  public WebServer start() {
    try {
      Server server = new ContainerServer(container);
      new SocketConnection(server).connect(new InetSocketAddress(port), sslContext);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    return this;
  }

  private WebLogger emptyLogger() {
    return new WebLogger() {
      @Override
      public void log(Request request, Response response, Optional<Throwable> e) {
      }
    };
  }

  public static WebServer redirectToHttps() {
    return redirect(80, 443);
  }

  public static WebServer redirect(int fromPort, int toPort) {
    return new WebServer("Redirect", fromPort, false).add((request, response) -> {
      String host = request.getHeader("HOST");
      if (toPort == 443) {
        response.redirect("https://" + host + request.path);
      } else if (toPort == 80) {
        response.redirect("http://" + host + request.path);
      } else {
        response.redirect("http://" + host + toPort + ":" + toPort + request.path);
      }
      return true;
    }).start();
  }

}
