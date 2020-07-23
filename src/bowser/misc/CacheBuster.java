package bowser.misc;

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;

import bowser.handler.StaticContentHandler;
import bowser.model.Controller;

/**
 * Changes resource paths on the fly in order to bust caches for old files.
 * 
 * For example, foo.js -> foo-j2np21.js<br>
 * And then when that file is modified, it would become foo.js -> foo-zz93q.js
 */
public class CacheBuster {

  private final StaticContentHandler resourceLoader;
  private final Map<String, String> nameMap = Maps.newConcurrentMap();

  public CacheBuster(StaticContentHandler resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public String hashPath(String path, Controller controller) {
    if (path.startsWith("http:") || path.startsWith("https:")) {
      return path;
    }
    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    byte[] data = resourceLoader.getData(path, controller);
    if (data == null) {
      return path;
    }

    String hash = Hashing.murmur3_32().hashBytes(data).toString();
    int i = path.lastIndexOf('.');
    checkState(i != -1, path);
    String ret = path.substring(0, i) + "-" + hash + path.substring(i);
    nameMap.put(ret, path);
    return ret;
  }

  public String unhashPath(String path) {
    return nameMap.getOrDefault(path, path);
  }

}
