package bowser;

import java.util.Optional;
import com.google.common.base.Stopwatch;

public interface WebLogger {

  public void log(Request request, Response response, Optional<Throwable> exception, Stopwatch watch);
  
}
