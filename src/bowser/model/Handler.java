package bowser.model;

import java.util.function.Consumer;

@FunctionalInterface
public interface Handler extends RequestHandler {

  @Override
  public default boolean process(Request request, Response response) {
    handle(request, response);
    return true;
  }

  public abstract void handle(Request request, Response response);

  /**
   * Returns a Handler which wraps it's execution with the given callback
   */
  public default Handler wrapWith(Consumer<Runnable> wrapper) {
    return (request, response) -> {
      wrapper.accept(() -> {
        handle(request, response);
      });
    };
  }


}
