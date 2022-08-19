package bowser.model;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static ox.util.Utils.getExtension;
import static ox.util.Utils.only;
import static ox.util.Utils.propagate;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.simpleframework.http.Cookie;
import org.simpleframework.http.Part;
import org.simpleframework.http.Path;
import org.simpleframework.http.Query;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import bowser.handler.MobileDetector;
import ox.Json;
import ox.Pair;
import ox.util.Images;
import ox.x.XList;
import ox.x.XMap;

public class Request {

  public final org.simpleframework.http.Request request;
  public List<String> segments;
  public String path;
  private Map<String, Object> userData = Maps.newHashMap();

  private String host = null;

  public Request(org.simpleframework.http.Request request) {
    this.request = request;
    setPath(request.getPath().getPath());
  }

  public void setHost(String s) {
    this.host = s;
  }

  public String getHost() {
    if (host == null) {
      String s = getHeader("Host");
      if (s != null) {
        int i = s.indexOf(':');
        if (i != -1) {
          s = s.substring(0, i);
        }
        host = s;
      }
    }
    return host;
  }

  public String getIP() {
    String ret = request.getClientAddress().getAddress().getHostAddress();
    if (ret.equals("127.0.0.1")) {
      String proxyIP = getHeader("X-Real-IP");
      if (proxyIP != null) {
        return proxyIP;
      }
    }
    return ret;
  }

  public String getOriginalPath() {
    return request.getPath().getPath();
  }

  public Path getPath() {
    return request.getPath();
  }

  public void setPath(String path) {
    this.segments = Splitter.on('/').omitEmptyStrings().splitToList(path);
    this.path = path.toLowerCase();
  }

  public String getSegment(int index) {
    return segments.get(index);
  }

  public String getString(int index) {
    return getSegment(index);
  }

  public int getInt(int index) {
    return parseInt(getSegment(index));
  }

  public long getLong(int index) {
    return parseLong(getSegment(index));
  }

  public boolean isPost() {
    return request.getMethod().equalsIgnoreCase("POST");
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
    if (value != null) {
      userData.put(key, value);
    }
    return this;
  }

  public Json getJson() {
    String s = getContent();
    Json ret;
    if (s.startsWith("{") || s.startsWith("[")) {
      ret = new Json(s);
    } else {
      ret = Json.object();
    }
    if (ret.isObject()) {
      getQuery().forEach((k, v) -> {
        // for some reason, when the json is stringified, it shows up like this:
        // { "string": "a", "{\"string\":\"a\"}": "" }
        if (!v.isEmpty() || k.charAt(0) != '{') {
          ret.with(k, v);
        }
      });
    }
    return ret;
  }

  public String getContent() {
    try {
      return request.getContent();
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  public InputStream getInputStream() {
    try {
      return request.getInputStream();
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  @Override
  public String toString() {
    return getMethod() + " " + path;
  }

  public String getHeader(String key) {
    return request.getValue(key);
  }

  public boolean isAjax() {
    if (!getMethod().equals("GET")) {
      return true;
    }
    if ("XMLHttpRequest".equals(getHeader("X-Requested-With"))) {
      return true;
    }
    String contentType = getHeader("Content-Type");
    if (contentType != null && contentType.startsWith("application/x-www-form")) {
      return true;
    }
    return false;
  }

  public HttpPart getPart() {
    return only(getParts());
  }

  public HttpPart getFile(String partName) {
    HttpPart part = getPart(partName);
    checkState(part.isFile());
    return part;
  }

  public HttpPart getPart(String partName) {
    Part part = request.getPart(partName);
    return part == null ? null : new HttpPart(part);
  }

  public XList<HttpPart> getParts() {
    XList<HttpPart> ret = XList.create();
    for (Part part : request.getParts()) {
      ret.add(new HttpPart(part));
    }
    return ret;
  }

  public XList<HttpPart> getFiles() {
    return getParts().filter(HttpPart::isFile);
  }

  public XMap<String, String> getHeaders() {
    XMap<String, String> ret = XMap.create();
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

  private static final Set<String> staticExtensions;
  static {
    Set<String> set = Sets.newHashSet("css", "scss", "js", "mjs", "jsx", "ico", "otf", "woff", "woff2", "eot", "ttf",
        "mp4", "map", "pdf", "cur", "txt", "mp3", "mov", "webm", "webp");
    set.addAll(Images.FORMATS);
    staticExtensions = ImmutableSet.copyOf(set);
  }

  public boolean isStaticResource() {
    String extension = getExtension(path);
    return staticExtensions.contains(extension);
  }

  public boolean isFromMobile() {
    return MobileDetector.isFromMobile(this);
  }
}
