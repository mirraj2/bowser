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
    Object ret = data.get(key);
    if (ret == null && request != null) {
      ret = request.get(key);
    }
    return (T) ret;
  }

  public Object put(String key, Object value) {
    return data.put(key, value);
  }

}
