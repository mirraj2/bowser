package bowser;

import jasonlib.IO;
import jasonlib.Log;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
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
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class WebServer {

  public final int port;
  public final boolean developerMode;
  public final List<Controller> controllers = Lists.newArrayList();

  private final List<RequestHandler> handlers = Lists.newArrayList();
  private StaticContentHandler staticContentHandler;

  private SSLContext sslContext;

  public WebServer(String appName, int port, boolean developerMode) {
    Template.appName = appName;
    this.port = port;
    this.developerMode = developerMode;
    this.staticContentHandler = new StaticContentHandler(this);
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

  public WebServer add(RequestHandler authenticator) {
    handlers.add(authenticator);
    return this;
  }

  public void add(Route route) {
    handlers.add(new RouteHandler(route));
  }

  public WebServer ssl(SSLContext sslContext) {
    this.sslContext = sslContext;
    return this;
  }

  public StaticContentHandler getResourceLoader() {
    return staticContentHandler;
  }

  private void handle(Request request, Response response) {
    Stopwatch watch = Stopwatch.createStarted();

    try {
      boolean handled = false;

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
      try {
        WebServer.this.handle(new Request(request), new Response(response));
      } catch (Throwable e) {
        e.printStackTrace();
        response.setStatus(Status.INTERNAL_SERVER_ERROR);
        try {
          Throwable root = Throwables.getRootCause(e);
          if (root instanceof IllegalStateException) {
            IO.from(root.getMessage()).to(response.getOutputStream());
          } else {
            response.close();
          }
        } catch (IOException e1) {
          e1.printStackTrace();
        }
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

}
