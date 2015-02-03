package bowser.node;

public class TextNode extends DomNode {

  public final String content;

  public TextNode(String content) {
    this.content = content;
  }

  @Override
  public void render(StringBuilder sb, int depth) {
    sb.append(content);
  }

}
