package bowser;

import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import ox.Pair;
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
    String s = request.getPath().getPath();
    this.path = s.toLowerCase();
    this.segments = ImmutableList.copyOf(Splitter.on('/').omitEmptyStrings().split(s));
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

  public String getHeader(String key) {
    return request.getValue(key);
  }

  public Map<String, String> getHeaders() {
    Map<String, String> ret = Maps.newLinkedHashMap();
    request.getNames().forEach(name -> {
      ret.put(name, request.getValue(name));
    });
    return ret;
  }

  public Pair<Long, Long> getRange() {
    String s = request.getValue("Range");
    if (s == null || !s.startsWith("bytes=")) {
      return null;
    }
    s = s.substring(6);
    int index = s.indexOf('-');
    long start = parseLong(s.substring(0, index));
    Long end = null;
    if (index < s.length() - 1) {
      end = parseLong(s.substring(index + 1));
    }
    return Pair.of(start, end);
  }

  public boolean isStaticResource() {
    if (path.endsWith(".css") || path.endsWith(".js") || path.endsWith(".png") || path.endsWith(".jpg")
        || path.endsWith(".ico") || path.endsWith(".ttf") || path.endsWith(".mp4") || path.endsWith(".map")) {
      return true;
    }
    return false;
  }

}
