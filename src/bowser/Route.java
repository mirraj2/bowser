package bowser;

import jasonlib.IO;
import jasonlib.Log;
import java.util.regex.Pattern;
import bowser.template.Data;
import bowser.template.Template;
import com.google.common.base.Throwables;

public class Route {
  public final Controller controller;
  public final String method, path;
  private boolean enableCaching;
  public final Pattern regex;

  public String resource;
  public RequestHandler beforeHandler;
  public Handler handler;
  public Data data = context -> {
  };

  private Template template;

  public Route(Controller controller, String method, String path, boolean enableCaching) {
    this.controller = controller;
    this.method = method;
    this.path = path;
    this.enableCaching = enableCaching;

    path = path.toLowerCase().replace("*", "[0-9a-zA-Z\\-]*");
    path += "/?";
    regex = Pattern.compile(path);
  }

  public Template getTemplate() {
    if ((this.template == null || !enableCaching) && resource != null) {
      String source = IO.from(controller.getClass(), resource).toString();
      try {
        this.template = Template.compile(source, controller.getServer().getResourceLoader());
      } catch (Exception e) {
        Log.error("Problem compiling template: " + resource);
        throw Throwables.propagate(e);
      }
    }
    return this.template;
  }

  public Route first(RequestHandler beforeHandler) {
    this.beforeHandler = beforeHandler;
    return this;
  }

  public Route data(Data data) {
    this.data = data;
    return this;
  }

  public Route to(String resource) {
    this.resource = resource;
    getTemplate(); // warm the cache
    return this;
  }

  public Route to(Handler handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public String toString() {
    return method + " " + path;
  }
}
