package bowser;

import static ox.util.Utils.propagate;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.simpleframework.http.Cookie;
import org.simpleframework.http.Status;

import ox.IO;
import ox.Json;

public class Response {

  public final org.simpleframework.http.Response response;

  public String responseBody;

  public Exception exception = null;

  private boolean gzip = false;

  public Response(org.simpleframework.http.Response response) {
    this.response = response;
  }

  public Response contentType(String type) {
    response.setValue("Content-Type", type);
    return this;
  }

  public Response redirect(String url) {
    response.setValue("Location", url);
    return status(Status.TEMPORARY_REDIRECT).close();
  }

  public Response cacheFor(int n, TimeUnit units) {
    response.setValue("Cache-Control", "max-age=" + TimeUnit.SECONDS.convert(n, units));
    return this;
  }

  public Response noCache() {
    header("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1
    header("Pragma", "no-cache"); // HTTP 1.0
    header("Expires", "0"); // Proxies
    return this;
  }

  public Response setCompressed(boolean b) {
    gzip = b;
    if (b) {
      header("Content-Encoding", "gzip");
    } else {
      header("Content-Encoding", null);
    }
    return this;
  }

  public boolean isCompressed() {
    return gzip;
  }

  public Response header(String key, String value) {
    response.setValue(key, value);
    return this;
  }

  public Response removeCookie(String key) {
    return cookie(key, null);
  }

  public Response cookie(String key, String value) {
    return cookie(key, value, 7, TimeUnit.DAYS);
  }

  public Response cookie(String key, String value, int expiry, TimeUnit units) {
    return cookie(key, value, expiry, units, "");
  }

  public Response cookie(String key, String value, int expiry, TimeUnit units, String domain) {
    Cookie cookie = new Cookie(key, value);
    if (!domain.isEmpty()) {
      cookie.setDomain(domain);
    }
    if (value == null) {
      cookie.setExpiry(0);
    } else {
      cookie.setExpiry((int) TimeUnit.SECONDS.convert(expiry, units));
    }
    return cookie(cookie);
  }

  public Response cookie(Cookie cookie) {
    response.setCookie(cookie);
    return this;
  }

  public OutputStream getOutputStream() {
    try {
      OutputStream os = response.getOutputStream();
      if (gzip) {
        os = new GZIPOutputStream(os);
      }
      return os;
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  public int status() {
    return response.getStatus().code;
  }

  public Response status(Status status) {
    response.setStatus(status);
    return this;
  }

  public Response status(int code) {
    response.setStatus(Status.getStatus(code));
    return this;
  }

  public Response write(Json json) {
    contentType("application/json");
    return write(json.toString());
  }

  public Response write(String text) {
    this.responseBody = text;
    IO.from(text).to(getOutputStream());
    return this;
  }

  public Response sendAttachment(File file) {
    header("Content-Disposition", "attachment; filename='" + file.getName() + "';");
    IO.from(file).to(getOutputStream());
    return this;
  }

  public Response close() {
    try {
      response.close();
      return this;
    } catch (IOException e) {
      throw propagate(e);
    }
  }

}
