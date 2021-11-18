package bowser.misc;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.Maps;

import bowser.handler.StaticContentHandler;

public class SCSSProcessor {

  private final Map<String, byte[]> cache = Maps.newConcurrentMap();
  private final SassFunction sass;

  private boolean enableCaching;

  public SCSSProcessor(StaticContentHandler staticContentHandler, boolean enableCaching) {
    this.enableCaching = enableCaching;

    if ("aarch64".equals(System.getProperty("os.arch"))) { // new Apple M1 processor
      this.sass = new NpxSass(staticContentHandler);
    } else {
      this.sass = new LibSass(staticContentHandler);
    }
  }

  public byte[] process(String path, byte[] data) {
    if (this.enableCaching) {
      byte[] ret = cache.get(path);
      if (ret != null) {
        return ret;
      }
    }
    String input = new String(data, StandardCharsets.UTF_8);
    byte[] ret = sass.apply(path, input);
    if (enableCaching) {
      cache.put(path, ret);
    }
    return ret;
  }

  public void setCachingEnabled(boolean b) {
    this.enableCaching = b;
    if (!b) {
      cache.clear();
    }
  }

  public static interface SassFunction {
    public byte[] apply(String path, String data);
  }

}