package bowser;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.Part;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import ox.IO;
import ox.Pair;

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

  public String getIP() {
    return request.getClientAddress().getAddress().getHostAddress();
  }

  public String getOriginalPath() {
    return request.getPath().getPath();
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

  public BufferedImage getImage() {
    Part part = getOnlyElement(request.getParts());
    try {
      return IO.from(part.getInputStream()).toImage();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public String getFileName() {
    Part part = getOnlyElement(request.getParts());
    return part.getFileName();
  }

  public byte[] getBytes() {
    Part part = getOnlyElement(request.getParts());
    try {
      return IO.from(part.getInputStream()).toByteArray();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
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
        || path.endsWith(".gif") || path.endsWith(".ico") || path.endsWith(".ttf") || path.endsWith(".otf")
        || path.endsWith("woff") || path.endsWith("woff2") || path.endsWith(".mp4") || path.endsWith(".map")
        || path.endsWith(".pdf") || path.endsWith(".cur")) {
      return true;
    }
    return false;
  }


  public boolean isFromMobile() {
    String userAgent = getHeader("User-Agent");
    if (userAgent == null) {
      return false;
    }
    userAgent = userAgent.toLowerCase();
    String[] mobileUserAgents = { "android", "webos", "iphone", "ipad", "blackberry", "iemobile", "opera mini" };
    for (String s : mobileUserAgents) {
      if (userAgent.contains(s)) {
        return true;
      }
    }
    return false;
  }
}
