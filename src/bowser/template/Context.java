package bowser.template;

import java.util.Map;
import bowser.Request;
import bowser.Response;
import com.google.common.collect.Maps;

public class Context {

  public final Request request;
  public final Response response;
  public final Map<String, Object> data = Maps.newHashMap();

  public Context(Request request, Response response) {
    this.request = request;
    this.response = response;
  }

  public <T> T get(String key) {
    return request.get(key);
  }

  public Object resolve(String key) {
    if (data.containsKey(key)) {
      return data.get(key);
    }
    return request.get(key);
  }

  public void put(String key, Object value) {
    data.put(key, value);
  }

}
