package bowser.node;

import bowser.template.Template;

public class Head extends DomNode {

  public Head(String title) {
    super("head");

  }

  public static Head defaults(String title) {
    Head ret = new Head(title);

    ret.add(new DomNode("meta").attribute("charset", "utf-8"));
    
    String viewport = "width=device-width, initial-scale=1";
    if(Template.mobileDisplay){
      viewport += ", maximum-scale=1";
    }
    
    ret.add(new DomNode("meta").attribute("name", "viewport").attribute("content", viewport));
    ret.add(new DomNode("title").text(title));
    ret.add(new DomNode("link").attribute("rel", "icon").attribute("type", "image/png")
        .attribute("href", "/favicon.png"));

    ret.generateWhitespace = true;

    return ret;
  }

}
