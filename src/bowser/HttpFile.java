package bowser;

import static ox.util.Utils.propagate;

import java.io.IOException;
import java.io.InputStream;

import org.simpleframework.http.ContentType;
import org.simpleframework.http.Part;

import ox.IO;
import ox.Json;

public class HttpFile {

  private final Part delegate;

  HttpFile(Part part) {
    this.delegate = part;
  }

  public String getName() {
    return delegate.getFileName();
  }

  public String getContent() {
    try {
      return delegate.getContent();
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  public InputStream getInputStream() {
    try {
      return delegate.getInputStream();
    } catch (IOException e) {
      throw propagate(e);
    }
  }

  public ContentType getContentType() {
    return delegate.getContentType();
  }

  public Json toJson() {
    return IO.from(getInputStream()).toJson();
  }

}
