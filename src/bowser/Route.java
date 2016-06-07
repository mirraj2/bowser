package bowser;

import java.util.regex.Pattern;
import com.google.common.base.Throwables;
import bowser.template.Data;
import bowser.template.Template;
import ox.IO;
import ox.Log;

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
  public byte[] resourceData;

  private boolean nonStatic = false;
  private String host = "";

  public Route(Controller controller, String method, String path, boolean enableCaching) {
    this.controller = controller;
    this.method = method;
    this.path = path;
    this.enableCaching = enableCaching;

    path = path.toLowerCase();
    path = path.replace("*", "[0-9a-zA-Z\\-_:@\\. ]*");
    path += "/?";
    regex = Pattern.compile(path);
  }

  public boolean matches(Request request) {
    if (!request.getMethod().equalsIgnoreCase(method)) {
      return false;
    }
    if (!regex.matcher(request.path).matches()) {
      return false;
    }
    if (request.isStaticResource() && nonStatic) {
      return false;
    }
    if (!host.isEmpty() && !host.equals(request.getHost())) {
      return false;
    }
    return true;
  }

  public Template getTemplate() {
    if ((this.template == null || !enableCaching) && resource != null && resource.endsWith(".html")) {
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

  public Route nonStatic() {
    nonStatic = true;
    return this;
  }

  public Route to(String resource) {
    this.resource = resource;
    if (resource.endsWith(".html")) {
      getTemplate(); // warm the cache
    } else {
      resourceData = IO.from(controller.getClass(), resource).toByteArray();
    }
    return this;
  }

  public Route host(String host) {
    this.host = host;
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
