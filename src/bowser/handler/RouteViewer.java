package bowser.handler;

import java.util.List;

import bowser.model.Controller;
import bowser.model.Route;
import bowser.template.Data;
import ox.Json;
import ox.XList;

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
    List<Controller> controllers = XList.create(getServer().controllers).sortSelf((a,b)->{
      return a.getClass().getSimpleName().compareTo(b.getClass().getSimpleName());
    });
    context.put("controllers", Json.array(controllers, controller -> {
      return Json.object()
          .with("name", controller.getClass().getSimpleName() + ".java")
          .with("routes", Json.array(controller.getRoutes(), Route::toJson));
    }));
  };

}
