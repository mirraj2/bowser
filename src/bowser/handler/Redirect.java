package bowser.handler;

import bowser.Handler;
import bowser.Request;
import bowser.Response;

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
