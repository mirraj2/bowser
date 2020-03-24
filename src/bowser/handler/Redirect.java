package bowser.handler;

import bowser.model.Handler;
import bowser.model.Request;
import bowser.model.Response;

public class Redirect implements Handler {

  private final String url;

  public Redirect(String url) {
    this.url = url;
  }

  @Override
  public void handle(Request request, Response response) {
    response.redirect(url);
  }

}
