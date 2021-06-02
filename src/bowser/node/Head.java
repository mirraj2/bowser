package bowser.node;

import java.util.Map;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Maps;

import bowser.template.Template;
import ox.x.XList;

public class Head extends DomNode {

  private static final XList<String> TAG_ORDERING = XList.of("meta", "title", "link", "script");

  private DomNode titleNode;

  public Head() {
    super("head");
  }

  public void sortChildren() {
    Map<DomNode, Integer> originalIndices = Maps.newIdentityHashMap();
    for (int i = 0; i < children.size(); i++) {
      originalIndices.put(children.get(i), i);
    }

    this.children.sort((a, b) -> {
      return ComparisonChain.start()
          .compare(TAG_ORDERING.indexOf(a.tag.toLowerCase()), TAG_ORDERING.indexOf(b.tag.toLowerCase()))
          .compare(originalIndices.get(a), originalIndices.get(b))
          .result();
    });
  }

  @Override
  public DomNode add(DomNode child) {
    if (child.tag.equalsIgnoreCase("title")) {
      if (titleNode == null) {
        titleNode = child;
        super.add(titleNode);
      } else {
        titleNode.text(child.text() + " - " + titleNode.text());
      }
    } else {
      super.add(child);
    }
    return this;
  }

  @Override
  public Head copy() {
    Head ret = new Head();
    copyInto(ret);
    return ret;
  }

  public static Head defaults(String title) {
    Head ret = new Head();

    ret.add(new DomNode("meta").attribute("charset", "utf-8"));

    String viewport = "width=device-width, initial-scale=1";
    if (Template.mobileDisplay) {
      viewport += ", maximum-scale=1";
    }

    ret.add(new DomNode("title").text(title));
    ret.add(new DomNode("meta").attribute("name", "viewport").attribute("content", viewport));

    ret.generateWhitespace = true;

    return ret;
  }

}
