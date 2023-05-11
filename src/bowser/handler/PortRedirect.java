package bowser.handler;

import bowser.model.Request;
import bowser.model.RequestHandler;
import bowser.model.Response;

public class PortRedirect implements RequestHandler {

  public final int toPort;

  public PortRedirect(int toPort) {
    this.toPort = toPort;
  }

  @Override
  public boolean process(Request request, Response response) {
    String host = request.getHost();
    String path = request.request.getTarget();
    if (toPort == 443) {
      response.redirect("https://" + host + path);
    } else if (toPort == 80) {
      response.redirect("http://" + host + path);
    } else {
      response.redirect("http://" + host + toPort + ":" + toPort + path);
    }
    return true;
  }

}
