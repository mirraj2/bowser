package bowser.model;

public interface RequestHandler {

  /**
   * Returns true if the request is handled and should not be processed any further.
   */
  public boolean process(Request request, Response response);

  public default void load() {
  }

}
