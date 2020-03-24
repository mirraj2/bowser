package bowser.misc;

import com.google.common.base.Stopwatch;

import bowser.model.Request;
import bowser.model.Response;

public interface WebLogger {

  /**
   * Response objects have an exception field for exception logging.
   */
  public void log(Request request, Response response, Stopwatch watch);
  
}
