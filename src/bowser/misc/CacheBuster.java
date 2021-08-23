package bowser.misc;

import static com.google.common.base.Preconditions.checkState;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;

import bowser.handler.StaticContentHandler;
import bowser.model.Controller;
import ox.util.Regex;

/**
 * Changes resource paths on the fly in order to bust caches for old files.
 * 
 * For example, foo.js -> foo-j2np21.js<br>
 * And then when that file is modified, it would become foo.js -> foo-zz93q.js
 */
public class CacheBuster {

  private final StaticContentHandler resourceLoader;

  // used for unhashing a path
  private final Map<String, String> nameMap = Maps.newConcurrentMap();

  // speed up the hashPath() method
  private final Map<String, String> globalCache = Maps.newConcurrentMap();
  private final ThreadLocal<Map<String, String>> threadCache = new ThreadLocal<>();

  private CachePolicy cacheEvictionPolicy = CachePolicy.GLOBAL_CACHE;

  public CacheBuster(StaticContentHandler resourceLoader) {
    this.resourceLoader = resourceLoader;
  }

  public String hashPath(String path, Controller controller) {
    return hashPath(path, controller, 0);
  }

  private String hashPath(String path, Controller controller, int depth) {
    if (path.startsWith("http:") || path.startsWith("https:")) {
      return path;
    }

    String key = controller == null ? path : controller.getClass().getSimpleName() + ":" + path;
    Map<String, String> cache = getOrCreateCache();

    String ret = cache.get(key);

    if (ret != null) {
      return ret;
    }

    // Log.debug(Thread.currentThread() + " LOAD: " + key);

    if (!path.startsWith("/")) {
      path = "/" + path;
    }
    byte[] data = resourceLoader.getData(path, controller);
    if (data == null) {
      ret = path;
    } else {
      if (path.endsWith(".mjs") || path.endsWith(".jsx")) {
        data = hashMJSImports(data, depth).getBytes(StandardCharsets.UTF_8);
      }

      String hash = Hashing.murmur3_32().hashBytes(data).toString();
      int i = path.lastIndexOf('.');
      checkState(i != -1, path);
      ret = path.substring(0, i) + "-" + hash + path.substring(i);
      nameMap.put(ret, path);
    }

    cache.put(key, ret);
    // Log.debug(Thread.currentThread() + " DONE: " + key);

    return ret;
  }

  public String unhashPath(String path) {
    return nameMap.getOrDefault(path, path);
  }

  static int counter = 0;

  public String hashMJSImports(byte[] data) {
    return hashMJSImports(data, 0);
  }

  /**
   * Goes through an mjs file and replaces paths with hashed ones.
   */
  private String hashMJSImports(byte[] data, int depth) {
    String s = new String(data, StandardCharsets.UTF_8);

    String ret = Regex.replaceAll("import (?:(?:\\{(?:.|\\n)*?\\}|\\w+) from )?\\\"(.*)\\\";|import\\(\"(.*)\"\\)", s,
        match -> {
          String fullMatch = match.group(0);
          // Log.debug(Joiner.on(' ').join(Collections.nCopies(depth, " ")) + fullMatch);
          int start = match.start();

          int groupIndex = match.group(1) != null ? 1 : 2;

          int i = match.start(groupIndex) - start;
          int j = match.end(groupIndex) - start;

          String path = hashPath(match.group(groupIndex), null, depth + 1);
          if (path.endsWith(".scss")) {
            path += ".js";
          }
          return fullMatch.substring(0, i) + path + fullMatch.substring(j);
        });

    return ret;
  }

  private Map<String, String> getOrCreateCache() {
    if (cacheEvictionPolicy == CachePolicy.GLOBAL_CACHE) {
      return globalCache;
    }
    Map<String, String> ret = threadCache.get();
    if (ret == null) {
      ret = Maps.newHashMap();
      threadCache.set(ret);
    }
    return ret;
  }

  /**
   * Cache is temporary, only for a given request.
   */
  public CacheBuster requestBasedCache() {
    cacheEvictionPolicy = CachePolicy.REQUEST_BASED_CACHE;
    globalCache.clear();
    return this;
  }

  public void onRequestFinished() {
    if (cacheEvictionPolicy == CachePolicy.REQUEST_BASED_CACHE) {
      threadCache.set(null);
    }
  }

  private static enum CachePolicy {
    GLOBAL_CACHE, REQUEST_BASED_CACHE;
  }

}
