package bowser.handler;

import bowser.model.Controller;
import bowser.template.Data;
import ox.Json;

/**
 * A webpage that lets you see which Controllers have registered which routes.
 */
public class RouteViewer extends Controller {

  private final String route;

  public RouteViewer(String route) {
    this.route = route;
  }

  @Override
  public void init() {
    route("GET", route).to("route-viewer.html").data(data);
  }

  private final Data data = context -> {
    context.put("controllers", Json.array(getServer().controllers, controller->{
      return Json.object()
          .with("name", controller.getClass().getSimpleName() + ".java");
    }));
  };

}
