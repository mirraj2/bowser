package bowser.handler;

import bowser.Request;
import bowser.Response;

public interface ExceptionHandler {

  public boolean handle(Request request, Response response, Exception e);

}
