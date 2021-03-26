package bowser.template;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import bowser.model.Controller;
import bowser.node.DomNode;
import bowser.node.DomParser;
import bowser.node.TextNode;
import ox.Log;

public class Imports {

  public static String normalizePath(String path) {
    if (path.startsWith("/") || path.startsWith("http")) {
      return path;
    }

    return "/" + path;
  }

  public static void appendToHead(DomNode head, DomNode headNode, Controller controller, boolean embedCSS) {
    for (String jsImport : split(headNode.getAttribute("js", ""))) {
      String s = normalizePath(jsImport);
      head.javascript(s, false);
    }

    for (String cssImport : split(headNode.getAttribute("css", ""))) {
      String s = normalizePath(cssImport);
      if (embedCSS) {
        String data = new String(controller.getData(cssImport), Charsets.UTF_8);
        head.add(new DomNode("style").add(new TextNode("\n" + data)));
      } else {
        head.css(s);
      }
    }

    for (DomNode child : headNode.getChildren()) {
      if (!(child instanceof TextNode)) {
        if (child.tag.equals("title")) {
          head.find("title").forEach(head::remove);
        }
        head.add(child);
      }
    }
  }

  public static void importJSToHead(Iterable<String> jsFiles, DomNode head, boolean defer) {
    for (String jsImport : jsFiles) {
      String s = normalizePath(jsImport);
      head.javascript(s, defer);
    }
  }

  public static void importCSSToHead(Iterable<String> cssFiles, DomNode head, MediaType mediaType) {
    for (String cssImport : cssFiles) {
      String s = normalizePath(cssImport);
      head.css(s, mediaType);
    }
  }

  public static List<DomNode> importJSInline(Iterable<String> jsFiles, Controller controller) {
    List<DomNode> ret = Lists.newArrayList();
    for (String jsImport : jsFiles) {
      if (!jsImport.startsWith("/")) {
        jsImport = "/" + jsImport;
      }
      byte[] jsBytes = controller.getData(jsImport);
      checkNotNull(jsBytes, "Could not find: " + jsImport);
      DomNode script = new DomNode("script");
      String s;
      if (jsImport.endsWith(".mjs")) {
        script.attribute("type", "module");
        s = controller.getServer().getCacheBuster().hashMJSImports(jsBytes);
      } else {
        s = new String(jsBytes, StandardCharsets.UTF_8);
      }
      script.add(new TextNode("\n" + s));
      ret.add(script);
    }
    return ret;
  }

  public static List<DomNode> createImport(DomNode importNode, Controller controller, DomParser parser) {
    List<DomNode> ret = Lists.newArrayList();

    ret.addAll(importJSInline(split(importNode.getAttribute("js", "")), controller));

    for (String htmlImport : split(importNode.getAttribute("html", ""))) {
      if (controller.getServer().showImportComments) {
        String comment = "\n\n<!-- BEGIN " + htmlImport + " -->\n";
        ret.add(new TextNode(comment));
      }
      byte[] data = controller.getData(htmlImport);
      checkNotNull(data, "Couldn't find: " + htmlImport);
      String html = new String(data, StandardCharsets.UTF_8);
      DomNode n;
      try {
        n = parser.parse(html, false);
      } catch (Throwable t) {
        Log.error("Problem importing: " + htmlImport);
        throw t;
      }
      ret.addAll(n.getChildren());
      if (controller.getServer().showImportComments) {
        String endComment = "\n<!-- END " + htmlImport + " -->";
        ret.add(new TextNode(endComment));
      }
    }

    return ret;
  }

  private static Iterable<String> split(String s) {
    return Splitter.on(' ').omitEmptyStrings().trimResults().split(s);
  }

  public static enum MediaType{
    SCREEN, PRINT;

    @Override
    public String toString() {
      return name().toLowerCase();
    }
  }

}
