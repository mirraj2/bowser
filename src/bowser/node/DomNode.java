package bowser.node;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class DomNode {

  public final String tag;
  private final List<String> attributes = new ArrayList<>(0);
  private final List<DomNode> children = Lists.newArrayList();
  public String text = "";

  public DomNode() {
    this("");
  }

  public DomNode(String tag) {
    this.tag = tag;
  }

  public DomNode(DomNode n) {
    this.tag = n.tag;
    this.attributes.addAll(n.attributes);
    this.children.addAll(n.children);
    this.text = n.text;
  }

  public DomNode replace(DomNode child, DomNode replacement) {
    return replace(child, ImmutableList.of(replacement));
  }

  public DomNode replace(DomNode child, List<DomNode> replacements) {
    checkState(children.contains(child));
    for (DomNode replacement : replacements) {
      checkState(!children.contains(replacement));
    }
    int index = children.indexOf(child);
    children.remove(index);
    children.addAll(index, replacements);
    return this;
  }

  public DomNode add(DomNode... children) {
    return add(Arrays.asList(children));
  }

  public DomNode add(Iterable<DomNode> children) {
    for (DomNode child : children) {
      checkNotNull(child);
      this.children.add(child);
    }
    return this;
  }

  public List<DomNode> getChildren() {
    return children;
  }

  public List<String> getAttributes() {
    return attributes;
  }

  public String getAttribute(String key) {
    return getAttribute(key, null);
  }

  public String getAttribute(String key, String defaultValue) {
    int i = attributeIndex(key);
    return i == -1 ? defaultValue : attributes.get(i + 1);
  }

  private int attributeIndex(String key) {
    for (int i = 0; i < attributes.size(); i += 2) {
      if (attributes.get(i).equals(key)) {
        return i;
      }
    }
    return -1;
  }

  public DomNode removeAttribute(String key) {
    int index = attributeIndex(key);
    attributes.remove(index);
    attributes.remove(index);
    return this;
  }

  public DomNode text(String text) {
    this.text = text;
    return this;
  }

  public DomNode id(String id) {
    return attribute("id", id);
  }

  /**
   * Sets the tooltip.
   */
  public DomNode title(String title) {
    return attribute("title", title);
  }

  public DomNode withClass(String clazz) {
    return attribute("class", clazz);
  }

  public DomNode attribute(String key) {
    return attribute(key, null);
  }

  public DomNode attribute(String key, Object value) {
    attributes.add(key);
    attributes.add(value == null ? null : value.toString());
    return this;
  }

  public DomNode style(String key, String value) {
    return attribute("style", key + ": " + value + ";");
  }

  public DomNode data(String key, Object value) {
    attribute("data-" + key, value);
    return this;
  }

  public DomNode javascript(String name) {
    return add(new DomNode("script").attribute("src", name));
  }

  public void render(StringBuilder sb, int depth) {
    renderStartTag(sb, depth);
    renderContent(sb, depth);
    renderEndTag(sb, depth);
    sb.append("\n");
  }

  public void renderStartTag(StringBuilder sb, int depth) {
    for (int i = 0; i < depth; i++) {
      sb.append("  ");
    }
    sb.append('<').append(tag);
    for (int i = 0; i < attributes.size(); i += 2) {
      String key = attributes.get(i);
      String value = attributes.get(i + 1);
      sb.append(' ').append(key);
      if (value != null) {
        sb.append("=\"").append(value).append("\"");
      }
    }
    sb.append('>');

    if (!children.isEmpty()) {
      sb.append('\n');
    }
  }

  public void renderContent(StringBuilder sb, int depth) {
    if (text.length() > 0) {
      sb.append(text);
    }

    for (DomNode child : children) {
      child.render(sb, depth + 1);
    }
  }

  public void renderEndTag(StringBuilder sb, int depth) {
    if (!text.isEmpty() || !children.isEmpty() || !tagsWithNoContent.contains(tag)) {
      if (!children.isEmpty()) {
        for (int i = 0; i < depth; i++) {
          sb.append("  ");
        }
      }
      sb.append("</").append(tag).append(">");
    }
    sb.append('\n');
  }

  private static final Set<String> tagsWithNoContent = ImmutableSet.of("meta", "br", "img", "link", "import");

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    render(sb, 0);
    return sb.toString();
  }

}
