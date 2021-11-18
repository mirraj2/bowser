package bowser.misc;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.propagate;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import com.google.common.collect.Lists;

import bowser.handler.StaticContentHandler;
import bowser.misc.SCSSProcessor.SassFunction;
import ox.IO;
import ox.Log;

public class NpxSass implements SassFunction {

  private final StaticContentHandler staticContentHandler;

  public NpxSass(StaticContentHandler staticContentHandler) {
    this.staticContentHandler = staticContentHandler;
  }

  @Override
  public byte[] apply(String path, String data) {
    URL url = staticContentHandler.pathToUrl(path);
    File file;
    try {
      file = new File(url.toURI());
    } catch (URISyntaxException e) {
      throw propagate(e);
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    run("npx sass " + file.getPath(), file.getParentFile(), baos);

    return baos.toByteArray();
  }

  /**
   * Redirects the outputstream of the process to the given one.
   */
  public static void run(String s, File dir, OutputStream out) {
    Log.debug(s);
    List<String> m = Lists.newArrayList("/bin/zsh", "-c", "--login", "source ~/.zshrc;" + s);
    // List<String> m = Lists.newArrayList("/bin/sh", "-c", s);
    int exitStatus;
    try {
      ProcessBuilder pb = new ProcessBuilder().command(m);
      Log.debug(dir);
      pb.directory(dir);
      pb.redirectError(Redirect.INHERIT);
      Process process = pb.start();
      IO.from(process.getInputStream()).to(out);
      exitStatus = process.waitFor();
    } catch (Exception e) {
      throw propagate(e);
    }
    checkState(exitStatus == 0, "Status: " + exitStatus);
  }

}
