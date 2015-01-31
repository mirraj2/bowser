package bowser.template;

import java.util.List;
import java.util.Map;
import bowser.handler.StaticContentHandler;
import bowser.node.DomNode;
import bowser.node.Head;
import bowser.node.StaticContentNode;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Imports {

  public static String appName;

  private static final Map<String, String> jsNicknames = Maps.newLinkedHashMap();
  private static final Map<String, String> cssNicknames = Maps.newLinkedHashMap();

  public static DomNode createHead(DomNode headNode) {
    Head head = new Head(appName);

    for (String jsImport : split(headNode.getAttribute("js", ""))) {
      jsImport = jsNicknames.getOrDefault(jsImport.toLowerCase(), "/" + jsImport);
      head.javascript(jsImport);
    }

    for (String cssImport : split(headNode.getAttribute("css", ""))) {
      cssImport = cssNicknames.getOrDefault(cssImport.toLowerCase(), "/" + cssImport);
      head.css(cssImport);
    }

    return head;
  }

  public static List<DomNode> createImport(DomNode importNode, StaticContentHandler loader) {
    List<DomNode> ret = Lists.newArrayList();

    for (String jsImport : split(importNode.getAttribute("js", ""))) {
      jsImport = jsNicknames.getOrDefault(jsImport.toLowerCase(), "/" + jsImport);
      ret.add(new DomNode("script").attribute("src", jsImport));
    }
    for (String htmlImport : split(importNode.getAttribute("html", ""))) {
      String comment = "\n<!-- BEGIN " + htmlImport + " -->\n";
      String html = new String(loader.getData(htmlImport), Charsets.UTF_8);
      String endComment = "<!-- END " + htmlImport + " -->\n\n";
      html = comment + html + endComment;

      ret.add(new StaticContentNode(html));
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
