package bowser.handler;

import bowser.WebServer;
import bowser.model.Request;
import bowser.model.RequestHandler;
import bowser.model.Response;
import bowser.model.Route;
import bowser.template.Context;
import bowser.template.Template;
import ox.IO;
import ox.Json;

public class RouteHandler implements RequestHandler {

  private final Route route;
  private final WebServer server;

  public RouteHandler(Route route) {
    this.route = route;
    this.server = route.controller.getServer();
  }

  @Override
  public boolean process(Request request, Response response) {
    if (!route.matches(request)) {
      return false;
    }

    if (route.handler != null) {
      route.handler.handle(request, response);
      return true;
    }
    Template template = route.getTemplate();
    if (template != null) {
      Context context = new Context(request, response);
      route.data.fill(context);
      if (server.includeRouteDebugInfo) {
        context.put("bowser", Json.object()
            .with("method", route.method)
            .with("path", route.path)
            .with("controller", route.controller.getClass().getSimpleName() + ".java")
            .with("resource", route.resource));
      }
      if (!response.response.isCommitted()) {
        response.noCache();
        response.write(template.render(context));
      }
      return true;
    }

    if (route.resourceData != null) {
      IO.from(route.resourceData).to(response.getOutputStream());
      return true;
    }

    return false;
  }

  public Route getRoute() {
    return route;
  }

  @Override
  public String toString() {
    return route.toString();
  }

  @Override
  public void load() {
    route.load();
  }

}
