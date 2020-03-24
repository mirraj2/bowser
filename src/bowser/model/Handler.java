package bowser.model;

@FunctionalInterface
public interface Handler extends RequestHandler {

  @Override
  public default boolean process(Request request, Response response) {
    handle(request, response);
    return true;
  }

  public abstract void handle(Request request, Response response);

}
