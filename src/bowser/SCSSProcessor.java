package bowser;

import static ox.util.Utils.propagate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.google.common.collect.Maps;

import io.bit3.jsass.CompilationException;
import io.bit3.jsass.Compiler;
import io.bit3.jsass.Options;
import io.bit3.jsass.Output;

public class SCSSProcessor {

  private final boolean enableCaching;
  private final Compiler compiler = new Compiler();
  private final Options options = new Options();
  private final Map<String, byte[]> cache = Maps.newConcurrentMap();

  public SCSSProcessor(boolean enableCaching) {
    this.enableCaching = enableCaching;
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

}
