package bowser;

import static java.lang.Integer.parseInt;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

public class Request {

  public final org.simpleframework.http.Request request;
  public List<String> segments;
  public String path;
  private Map<String, Object> userData = Maps.newHashMap();

  public Request(org.simpleframework.http.Request request) {
    this.request = request;
    this.path = request.getPath().getPath().toLowerCase();
    this.segments = ImmutableList.copyOf(Splitter.on('/').omitEmptyStrings().split(path));
  }

  public Path getPath() {
    return request.getPath();
  }

  public String getSegment(int index) {
    return segments.get(index);
  }

  public int getInt(int index) {
    return parseInt(getSegment(index));
  }

  public String getMethod() {
    return request.getMethod();
  }

  public String param(String key) {
    return request.getQuery().get(key);
  }

  public Query getQuery() {
    return request.getQuery();
  }

  public String cookie(String key) {
    Cookie cookie = request.getCookie(key);
    return cookie == null ? null : cookie.getValue();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(String key) {
    return (T) userData.get(key);
  }

  public Request put(String key, Object value) {
    userData.put(key, value);
    return this;
  }

  public String getContent() {
    try {
      return request.getContent();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Override
  public String toString() {
    return getMethod() + " " + path;
  }

  public boolean isStaticResource() {
    if (path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".jpg")
        || path.endsWith(".ico") || path.endsWith(".ttf")) {
      return true;
    }
    return false;
  }

}
