package bowser.node;

import bowser.template.Template;

public class Head extends DomNode {

  private DomNode titleNode;

  public Head() {
    super("head");
  }

  @Override
  public DomNode add(DomNode child) {
    if (child.tag.equalsIgnoreCase("script")) {
      child.parent = this;
      int i = getIndexOfLastChild("script");
      if (i == -1) {
        i = getIndexOfLastChild("link"); // ensure the scripts go after the css
      }
      this.children.add(i + 1, child);
    } else if (child.tag.equalsIgnoreCase("link")) {
      child.parent = this;
      this.children.add(getIndexOfLastChild("link") + 1, child);
    } else if (child.tag.equalsIgnoreCase("title")) {
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

  private int getIndexOfLastChild(String tag) {
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).tag.equalsIgnoreCase(tag)) {
        return i;
      }
    }
    return -1;
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
    ret.add(new DomNode("link").attribute("rel", "icon").attribute("type", "image/png")
        .attribute("href", "/favicon.png"));

    ret.generateWhitespace = true;

    return ret;
  }

}
