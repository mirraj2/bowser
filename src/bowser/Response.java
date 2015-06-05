package bowser;

import jasonlib.IO;
import jasonlib.Json;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;
import org.simpleframework.http.Cookie;
import org.simpleframework.http.Status;
import com.google.common.base.Throwables;

public class Response {

  public final org.simpleframework.http.Response response;

  public String responseBody;

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

  public Response header(String key, String value) {
    response.addValue(key, value);
    return this;
  }

  public Response cookie(String key, String value) {
    return cookie(key, value, 7, TimeUnit.DAYS);
  }

  public Response cookie(String key, String value, int expiry, TimeUnit units) {
    Cookie cookie = new Cookie(key, value);
    if (value == null) {
      cookie.setExpiry(0);
    } else {
      cookie.setExpiry((int) TimeUnit.SECONDS.convert(expiry, units));
    }
    response.setCookie(cookie);
    return this;
  }

  public OutputStream getOutputStream() {
    try {
      return response.getOutputStream();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  public Response status(Status status) {
    response.setStatus(status);
    return this;
  }

  public Response write(Json json) {
    return write(json.toString());
  }

  public Response write(String text) {
    this.responseBody = text;
    IO.from(text).to(getOutputStream());
    return this;
  }

  public Response close() {
    try {
      response.close();
      return this;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

}
