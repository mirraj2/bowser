package bowser;

import java.util.Optional;

public interface WebLogger {

  public void log(Request request, Response response, Optional<Throwable> exception, long responseTime);
  
}
