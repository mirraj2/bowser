package bowser.template;

import java.util.Map;
import com.google.common.collect.Maps;
import bowser.Request;
import bowser.Response;

public class Context {

  public final Request request;
  public final Response response;
  public final Map<String, Object> data = Maps.newHashMap();

  public Context() {
    this(null, null);
  }

  public Context(Request request, Response response) {
    this.request = request;
    this.response = response;
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    if (request == null) {
      return (T) data.get(key);
    }
    return request.get(key);
  }

  public Object resolve(String key) {
    if (data.containsKey(key)) {
      return data.get(key);
    }
    return get(key);
  }

  public void put(String key, Object value) {
    data.put(key, value);
  }

}
