package bowser.node;

import bowser.template.Template;

public class Head extends DomNode {

  private DomNode titleNode;

  public Head() {
    super("head");
  }

  @Override
  public DomNode add(DomNode child) {
    if (child.tag.equalsIgnoreCase("title")) {
      if (titleNode == null) {
        titleNode = child;
      } else {
        titleNode.text(child.text() + " - " + titleNode.text());
        return this;
      }
    }
    return super.add(child);
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
    if(Template.mobileDisplay){
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
