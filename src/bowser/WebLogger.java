package bowser;

import com.google.common.base.Stopwatch;

public interface WebLogger {

  /**
   * Response objects have an exception field for exception logging.
   */
  public void log(Request request, Response response, Stopwatch watch);
  
}
