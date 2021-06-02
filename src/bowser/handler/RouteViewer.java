package bowser.handler;

import static com.google.common.base.Preconditions.checkNotNull;
import static ox.util.Utils.checkNotEmpty;
import static ox.util.Utils.normalize;

import java.util.List;
import java.util.function.Consumer;

import bowser.model.Controller;
import bowser.model.Request;
import bowser.model.Route;
import bowser.template.Data;
import ox.Json;
import ox.util.Functions;
import ox.x.XList;

/**
 * A webpage that lets you see which Controllers have registered which routes.
 */
public class RouteViewer extends Controller {

  private final String route;
  private Consumer<Request> authenticator;

  public RouteViewer(String route) {
    this(route, Functions.emptyConsumer());
  }

  public RouteViewer(String route, Consumer<Request> authenticator) {
    this.route = checkNotEmpty(normalize(route));
    this.authenticator = checkNotNull(authenticator);
  }

  @Override
  public void init() {
    route("GET", route).to("route-viewer.html").data(data);
  }

  private final Data data = context -> {
    authenticator.accept(context.request);

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
