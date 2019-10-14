package bowser.handler;

import bowser.Request;
import bowser.RequestHandler;
import bowser.Response;

public interface ExceptionHandler {

  public boolean handle(Request request, Response response, RequestHandler handler, Exception e);

}
