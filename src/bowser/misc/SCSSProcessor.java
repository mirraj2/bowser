package bowser.misc;

import static ox.util.Utils.propagate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import bowser.handler.StaticContentHandler;
import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.importer.Import;

/**
 * This code is based off of:
 * https://github.com/bit3/jsass/blob/master/example/webapp/src/main/java/io/bit3/jsass/example/webapp/JsassServlet.java
 */
public class SCSSProcessor {

  private final StaticContentHandler staticContentHandler;
  private final boolean enableCaching;
  private final Compiler compiler = new Compiler();
  private final Options options = new Options();
  private final Map<String, byte[]> cache = Maps.newConcurrentMap();

  public SCSSProcessor(StaticContentHandler staticContentHandler, boolean enableCaching) {
    this.staticContentHandler = staticContentHandler;
    this.enableCaching = enableCaching;
    this.options.setImporters(ImmutableList.of(this::doImport));
  }

  public byte[] process(String path, byte[] data) {
    if (this.enableCaching) {
      byte[] ret = cache.get(path);
      if (ret != null) {
        return ret;
      }
    }
    String input = new String(data, StandardCharsets.UTF_8);
    try {
      final Output output = compiler.compileString(input, null, null, options);
      byte[] ret = output.getCss().getBytes(StandardCharsets.UTF_8);
      if (enableCaching) {
        cache.put(path, ret);
      }
      return ret;
    } catch (CompilationException e) {
      throw propagate(e);
    }
  }

  private Collection<Import> doImport(String url, Import previous) {
    byte[] data = staticContentHandler.getData(url);
    String source = new String(data, StandardCharsets.UTF_8);

    URI uri;
    try {
      uri = new URI(url);
    } catch (URISyntaxException e) {
      throw propagate(e);
    }

    Import ret = new Import(uri, uri, source);
    return Collections.singleton(ret);
  }

}