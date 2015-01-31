package bowser.node;

public class StaticContentNode extends DomNode {

  private final String content;

  public StaticContentNode(String content) {
    this.content = content;
  }

  @Override
  public void render(StringBuilder sb, int depth) {
    sb.append(content);
  }

}
