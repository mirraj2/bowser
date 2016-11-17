package bowser.handler;

import bowser.Request;
import bowser.RequestHandler;
import bowser.Response;
import bowser.Route;
import bowser.template.Context;
import bowser.template.Template;
import ox.IO;

public class RouteHandler implements RequestHandler {

  private final Route route;

  public RouteHandler(Route route) {
    this.route = route;
  }

  @Override
  public boolean process(Request request, Response response) {
    if (!route.matches(request)) {
      return false;
    }

    if (route.beforeHandler != null) {
      if (route.beforeHandler.process(request, response)) {
        return true;
      }
    }
    if (route.handler != null) {
      route.handler.handle(request, response);
      return true;
    }
    Template template = route.getTemplate();
    if (template != null) {
      Context context = new Context(request, response);
      route.data.fill(context);
      if (!response.response.isCommitted()) {
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

  @Override
  public String toString() {
    return route.toString();
  }

  @Override
  public void load() {
    route.load();
  }

}
