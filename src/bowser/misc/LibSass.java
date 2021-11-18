package bowser.misc;

import static ox.util.Utils.normalize;
import static ox.util.Utils.propagate;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;

import com.google.common.collect.ImmutableList;

import bowser.handler.StaticContentHandler;
import bowser.misc.SCSSProcessor.SassFunction;
import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;
import io.bit3.jsass.importer.Import;

/**
 * This code is based off of:
 * https://github.com/bit3/jsass/blob/master/example/webapp/src/main/java/io/bit3/jsass/example/webapp/JsassServlet.java
 */
public class LibSass implements SassFunction {

  private final Compiler compiler = new Compiler();
  private final Options options = new Options();
  private final StaticContentHandler staticContentHandler;

  public LibSass(StaticContentHandler staticContentHandler) {
    this.staticContentHandler = staticContentHandler;
    this.options.setImporters(ImmutableList.of(this::doImport));
  }
  
  @Override
  public byte[] apply(String path, String data) {
    Output output;
    try {
      output = compiler.compileString(data, null, null, options);
    } catch (CompilationException e) {
      throw propagate(e);
    }
    byte[] ret = normalize(output.getCss()).getBytes(StandardCharsets.UTF_8);
    return ret;
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
