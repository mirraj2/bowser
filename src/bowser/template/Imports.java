package bowser.template;

import java.util.List;
import java.util.Map;
import bowser.handler.StaticContentHandler;
import bowser.node.DomNode;
import bowser.node.DomParser;
import bowser.node.Head;
import bowser.node.TextNode;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Imports {

  private static final Map<String, String> jsNicknames = Maps.newLinkedHashMap();
  private static final Map<String, String> cssNicknames = Maps.newLinkedHashMap();

  private static final DomParser parser = new DomParser();

  public static void appendToHead(Head head, DomNode headNode) {
    for (String jsImport : split(headNode.getAttribute("js", ""))) {
      jsImport = jsNicknames.getOrDefault(jsImport.toLowerCase(), "/" + jsImport);
      head.javascript(jsImport);
    }

    for (String cssImport : split(headNode.getAttribute("css", ""))) {
      cssImport = cssNicknames.getOrDefault(cssImport.toLowerCase(), "/" + cssImport);
      head.css(cssImport);
    }
  }

  public static List<DomNode> createImport(DomNode importNode, StaticContentHandler loader) {
    List<DomNode> ret = Lists.newArrayList();

    for (String jsImport : split(importNode.getAttribute("js", ""))) {
      jsImport = jsNicknames.getOrDefault(jsImport.toLowerCase(), "/" + jsImport);
      ret.add(new DomNode("script").attribute("src", jsImport));
    }
    for (String htmlImport : split(importNode.getAttribute("html", ""))) {
      String comment = "\n\n<!-- BEGIN " + htmlImport + " -->\n";
      ret.add(new TextNode(comment));
      String html = new String(loader.getData(htmlImport), Charsets.UTF_8);
      ret.add(parser.parse(html, false));
      String endComment = "\n<!-- END " + htmlImport + " -->";
      ret.add(new TextNode(endComment));
    }

    return ret;
  }

  private static Iterable<String> split(String s) {
    return Splitter.on(' ').omitEmptyStrings().split(s);
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
