package bowser.node;

import bowser.template.Template;

public class Head extends DomNode {

  public Head(String title) {
    super("head");

    add(new DomNode("meta").attribute("charset", "utf-8"));
    add(new DomNode("meta").attribute("http-equiv", "X-UA-Compatible").attribute("content", "IE=edge"));
    
    String viewport = "width=device-width, initial-scale=1";
    if(Template.mobileDisplay){
      viewport += ", maximum-scale=1";
    }
    
    add(new DomNode("meta").attribute("name", "viewport").attribute("content", viewport));
    add(new DomNode("title").text(title));
    add(new DomNode("link").attribute("rel", "icon").attribute("type", "image/png")
        .attribute("href", "/favicon.png"));

    this.generateWhitespace = true;
  }

}
