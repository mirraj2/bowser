package bowser.handler;

import bowser.Request;
import bowser.RequestHandler;
import bowser.Response;

public interface ExceptionHandler {

  /**
   * When an exception is thrown by any of the handlers on a Bowser server, this handle method is called.
   * 
   * If it returns false, then an exception is propagated, causing Bowser's default exception handling code to be
   * called. This results in rendering a plaintext error message to the response. If it returns true, then no further
   * action takes place.
   */
  public boolean handle(Request request, Response response, RequestHandler handler);

}
