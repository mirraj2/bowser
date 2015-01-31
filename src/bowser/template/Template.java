package bowser.template;

import static com.google.common.base.Preconditions.checkState;
import jasonlib.Json;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import bowser.handler.StaticContentHandler;
import bowser.node.DomNode;
import bowser.node.DomParser;
import bowser.node.StaticContentNode;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;

public class Template {

  private static final DomParser parser = new DomParser();

  private final DomNode root;

  private Template(String s, StaticContentHandler loader) {
    root = parser.parse(s);

    for (DomNode child : ImmutableList.copyOf(root.getChildren())) {
      if (child.tag.equals("head")) {
        root.replace(child, Imports.createHead(child));
      } else if (child.tag.equals("import")) {
        root.replace(child, Imports.createImport(child, loader));
      }
    }
  }

  public String render(Context context) {
    StringBuilder sb = new StringBuilder();
    sb.append("<!DOCTYPE html>\n");
    render(root, sb, 0, context);
    return sb.toString();
  }

  private void render(DomNode node, StringBuilder sb, int depth, Context context) {
    String loop = node.getAttribute("loop");
    if (loop != null) {
      renderLoop(node, sb, depth, context, loop);
      return;
    }

    String iff = node.getAttribute("if");
    if (iff != null) {
      boolean b = resolve(iff, context);
      if (b) {
        node.removeAttribute("if");
      } else {
        return;
      }
    }

    if (node instanceof StaticContentNode) {
      node.render(sb, depth);
    } else {
      node.renderStartTag(sb, depth);

      renderText(node, sb, context);
      for (DomNode child : node.getChildren()) {
        render(child, sb, depth + 1, context);
      }

      node.renderEndTag(sb, depth);
    }
  }

  private void renderLoop(DomNode node, StringBuilder sb, int depth, Context context, String loop) {
    List<String> m = Splitter.on(' ').splitToList(loop);
    String variableName = m.get(0);
    checkState(m.get(1).equalsIgnoreCase("in"));
    String collectionName = m.get(2);

    Collection<Object> collection = resolve(collectionName, context);
    if (collection != null) {
      for (Object o : collection) {
        context.put(variableName, o);
        render(new DomNode(node).removeAttribute("loop"), sb, depth, context);
      }
    }
  }

  private void renderText(DomNode node, StringBuilder sb, Context context) {
    String text = node.text;

    if (node.tag.equals("script")) {
      sb.append(text);
      return;
    }

    int variableStartIndex = -1;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (variableStartIndex >= 0) {
        if (c == '}') {
          String variableName = text.substring(variableStartIndex + 1, i);
          sb.append(evaluate(variableName, context));
          variableStartIndex = -1;
        }
      } else {
        if (c == '{') {
          variableStartIndex = i;
        } else {
          sb.append(c);
        }
      }
    }
  }

  private String evaluate(String variableName, Context context) {
    Object o = resolve(variableName, context);
    if (o == null) {
      return "";
      // return "{" + variableName + "}";
    }
    return String.valueOf(o);
  }

  @SuppressWarnings("unchecked")
  private <T> T resolve(String expression, Context context) {
    expression = expression.toLowerCase();

    Iterator<String> iter = Splitter.on('.').split(expression).iterator();

    Object reference = context.data.get(iter.next());

    while (iter.hasNext()) {
      String s = iter.next();
      if (s.endsWith("()")) {
        String method = s.substring(0, s.length() - 2);
        reference = invokeMethod(reference, method);
      } else {
        reference = dereference(reference, s);
      }
    }

    return (T) reference;
  }

  private Object invokeMethod(Object o, String method) {
    if (method.equals("isempty")) {
      if (o == null) {
        return true;
      }
      if (o instanceof Collection) {
        return ((Collection<?>) o).isEmpty();
      } else {
        return false;
      }
    } else if(method.equals("hasdata")){
      return !((boolean) invokeMethod(o, "isempty"));
    }
    else {
      return null;
    }
  }

  private Object dereference(Object o, String fieldName) {
    if (o == null) {
      return null;
    } else if (o instanceof Json) {
      Json json = (Json) o;
      return json.get(fieldName);
    } else {
      throw new RuntimeException("Don't know how to dereference a " + o.getClass() + " object.");
    }
  }

  public static Template compile(String source, StaticContentHandler loader) {
    return new Template(source, loader);
  }

}
