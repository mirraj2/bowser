package bowser.template;

import java.util.Map;
import bowser.Request;
import com.google.common.collect.Maps;

public class Context {

  public final Request request;
  public final Map<String, Object> data = Maps.newHashMap();

  public Context(Request request) {
    this.request = request;
  }

  public <T> T get(String key) {
    return request.get(key);
  }

  public void put(String key, Object value) {
    data.put(key.toLowerCase(), value);
  }

}
