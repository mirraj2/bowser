package bowser.template;

import static com.google.common.base.Preconditions.checkNotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import bowser.handler.StaticContentHandler;
import bowser.node.DomNode;
import bowser.node.DomParser;
import bowser.node.TextNode;

public class Imports {

  public static void appendToHead(DomNode head, DomNode headNode, StaticContentHandler loader, boolean embedCSS) {
    for (String jsImport : split(headNode.getAttribute("js", ""))) {
      String s;
        if (jsImport.startsWith("/") || jsImport.startsWith("http")) {
          s = jsImport;
        } else {
          s = "/" + jsImport;
        }
      head.javascript(s);
    }

    for (String cssImport : split(headNode.getAttribute("css", ""))) {
      String s;
        if (cssImport.startsWith("/") || cssImport.startsWith("http")) {
          s = cssImport;
        } else {
          s = "/" + cssImport;
        }
      if (embedCSS) {
        String data = new String(loader.getData(cssImport), Charsets.UTF_8);
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

  public static List<DomNode> createImport(DomNode importNode, StaticContentHandler loader, DomParser parser) {
    List<DomNode> ret = Lists.newArrayList();

    for (String jsImport : split(importNode.getAttribute("js", ""))) {
      if (!jsImport.startsWith("/")) {
        jsImport = "/" + jsImport;
      }
      byte[] jsBytes = loader.getData(jsImport);
      checkNotNull(jsBytes, "Could not find: " + jsImport);
      String s = new String(jsBytes, StandardCharsets.UTF_8);
      ret.add(new DomNode("script").add(new TextNode("\n" + s)));
      // ret.add(new DomNode("script").attribute("src", jsImport));
    }
    for (String htmlImport : split(importNode.getAttribute("html", ""))) {
      if (loader.getServer().showImportComments) {
        String comment = "\n\n<!-- BEGIN " + htmlImport + " -->\n";
        ret.add(new TextNode(comment));
      }
      byte[] data = loader.getData(htmlImport);
      checkNotNull(data, "Couldn't find: " + htmlImport);
      String html = new String(data, StandardCharsets.UTF_8);
      DomNode n = parser.parse(html, false);
      ret.addAll(n.getChildren());
      if (loader.getServer().showImportComments) {
        String endComment = "\n<!-- END " + htmlImport + " -->";
        ret.add(new TextNode(endComment));
      }
    }

    return ret;
  }

  private static Iterable<String> split(String s) {
    return Splitter.on(' ').omitEmptyStrings().trimResults().split(s);
  }

}
