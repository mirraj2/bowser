package bowser.template;

import java.util.List;
import java.util.Map;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import bowser.handler.StaticContentHandler;
import bowser.node.DomNode;
import bowser.node.DomParser;
import bowser.node.TextNode;

public class Imports {

  private static final Map<String, String> jsNicknames = Maps.newLinkedHashMap();
  private static final Map<String, String> cssNicknames = Maps.newLinkedHashMap();

  public static void appendToHead(DomNode head, DomNode headNode) {
    for (String jsImport : split(headNode.getAttribute("js", ""))) {
      String s = jsNicknames.get(jsImport.toLowerCase());
      if (s == null) {
        if (jsImport.startsWith("/") || jsImport.startsWith("http")) {
          s =  jsImport;
        } else{
          s = "/" + jsImport;
        }
      }
      head.javascript(s);
    }

    for (String cssImport : split(headNode.getAttribute("css", ""))) {
      String s = cssNicknames.get(cssImport.toLowerCase());
      if (s == null) {
        if (cssImport.startsWith("/") || cssImport.startsWith("http")) {
          s = cssImport;
        } else {
          s = "/" + cssImport;
        }
      }
      head.css(s);
    }
  }

  public static List<DomNode> createImport(DomNode importNode, StaticContentHandler loader, DomParser parser) {
    List<DomNode> ret = Lists.newArrayList();

    for (String jsImport : split(importNode.getAttribute("js", ""))) {
      jsImport = jsNicknames.getOrDefault(jsImport.toLowerCase(), "/" + jsImport);

      String s = new String(loader.getData(jsImport), Charsets.UTF_8);
      ret.add(new DomNode("script").add(new TextNode("\n" + s)));
      // ret.add(new DomNode("script").attribute("src", jsImport));
    }
    for (String htmlImport : split(importNode.getAttribute("html", ""))) {
      String comment = "\n\n<!-- BEGIN " + htmlImport + " -->\n";
      ret.add(new TextNode(comment));
      String html = new String(loader.getData(htmlImport), Charsets.UTF_8);
      DomNode n = parser.parse(html, false);
      ret.addAll(n.getChildren());
      // ret.add(n);
      String endComment = "\n<!-- END " + htmlImport + " -->";
      ret.add(new TextNode(endComment));
    }

    return ret;
  }

  private static Iterable<String> split(String s) {
    return Splitter.on(' ').omitEmptyStrings().trimResults().split(s);
  }

  public static void shortcut(String nickname, String fullName) {
    nickname = nickname.toLowerCase();
    if (fullName.endsWith(".js")) {
      jsNicknames.put(nickname, fullName);
    } else if (fullName.endsWith(".css")) {
      cssNicknames.put(nickname, fullName);
    } else {
      throw new RuntimeException("Don't know type of: " + fullName);
    }
  }

}
