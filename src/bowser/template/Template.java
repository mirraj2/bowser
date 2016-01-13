package bowser.template;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.size;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Multimap;
import bowser.handler.StaticContentHandler;
import bowser.node.DomNode;
import bowser.node.DomParser;
import bowser.node.Head;
import bowser.node.TextNode;
import ox.Json;
import ox.Reflection;

public class Template {

  public static String appName;
  public static boolean mobileDisplay = false;

  private static final DomParser parser = new DomParser();

  private final DomNode root;
  private Head head = null;

  private boolean isRoot;

  private Template(String s) {
    isRoot = false;
    root = parser.parse(s, isRoot);
  }

  private Template(String s, StaticContentHandler loader) {
    isRoot = true;
    root = parser.parse(s, isRoot);

    init(root, loader);
  }

  private void init(DomNode root, StaticContentHandler loader) {
    for (DomNode node : root.getAllNodes()) {
      if (node instanceof Head) {
        head = (Head) node;
      } else if ("head".equals(node.tag)) {
        Imports.appendToHead(head, node);
        node.parent.remove(node);
      } else if ("import".equals(node.tag)) {
        List<DomNode> importedNodes = Imports.createImport(node, loader);
        node.parent.replace(node, importedNodes);
        for (DomNode importedNode : importedNodes) {
          init(importedNode, loader);
        }
      }
    }
  }

  public String render(Context context) {
    StringBuilder sb = new StringBuilder();
    if (isRoot) {
      sb.append("<!DOCTYPE html>\n");
    }
    render(root, sb, 0, context);
    return sb.toString();
  }

  private void render(DomNode node, StringBuilder sb, int depth, Context context) {
    String loop = node.getAttribute("loop");
    if (loop != null && !"video".equals(node.tag)) {
      renderLoop(node, sb, depth, context, loop);
      return;
    }

    String iff = node.getAttribute("if");
    if (iff != null) {
      boolean b = resolveBoolean(iff, context);
      if (b) {
        node = new DomNode(node).removeAttribute("if");
      } else {
        return;
      }
    }

    if (node instanceof TextNode) {
      renderText((TextNode) node, sb, depth, context);
    } else {
      node.renderStartTag(sb, depth, replacer(context));

      for (DomNode child : node.getChildren()) {
        render(child, sb, depth + 1, context);
      }

      node.renderEndTag(sb, depth);
    }
  }

  private boolean resolveBoolean(String s, Context context) {
    int i = s.indexOf(" && ");
    if (i != -1) {
      String a = s.substring(0, i);
      String b = s.substring(i + 4);
      return resolveBoolean(a, context) && resolveBoolean(b, context);
    }

    if (s.startsWith("!")) {
      return !resolveBoolean(s.substring(1), context);
    }

    Object o = resolve(s, context);
    if (o == null) {
      return false;
    }
    if (o instanceof Boolean) {
      Boolean b = (Boolean) o;
      return b;
    } else {
      return true;
    }
  }

  @SuppressWarnings("unchecked")
  private void renderLoop(DomNode node, StringBuilder sb, int depth, Context context, String loop) {
    List<String> m = Splitter.on(' ').splitToList(loop);
    String variableName = m.get(0);
    checkState(m.get(1).equalsIgnoreCase("in"));
    String collectionName = m.get(2);

    Object data = resolve(collectionName, context);

    if (data == null) {
      return;
    }

    if (data.getClass().isArray()) {
      data = Arrays.asList((Object[]) data);
    }

    if (data instanceof Json && ((Json) data).isArray()) {
      data = ((Json) data).asJsonArray();
    }

    if (data instanceof Map) {
      ((Map<?, ?>) data).entrySet().forEach((entry) -> {
        context.put(variableName, entry);
        render(new DomNode(node).removeAttribute("loop"), sb, depth, context);
        context.data.remove(variableName);
      });
    } else if (data instanceof Iterable) {
      for (Object o : (Iterable<?>) data) {
        context.put(variableName, o);
        render(new DomNode(node).removeAttribute("loop"), sb, depth, context);
        context.data.remove(variableName);
      }
    } else if (data instanceof Multimap) {
      Multimap<Object, Object> multimap = (Multimap<Object, Object>) data;
      for (Object key : multimap.keySet()) {
        context.put(variableName, key);
        context.put("values", multimap.get(key));
        render(new DomNode(node).removeAttribute("loop"), sb, depth, context);
        context.data.remove(variableName);
        context.data.remove("values");
      }
    } else {
      throw new RuntimeException("Unhandled data type: " + data.getClass());
    }
  }

  private void renderText(TextNode node, StringBuilder sb, int depth, Context context) {
    Function<String, String> replacer = replacer(context);
    if (node.parent.tag.equals("script")) {
      replacer = replacer(context, "$$(", ")", false);
    }
    String text = replacer.apply(node.content);
    sb.append(text);
  }

  private Function<String, String> replacer(Context context) {
    return replacer(context, "{", "}", true);
  }

  private Function<String, String> replacer(Context context, String start, String end, boolean nullToEmpty) {
    return text -> {
      if (text == null) {
        return null;
      }

      StringBuilder sb = new StringBuilder();

      int variableStartIndex = -1;
      for (int i = 0; i < text.length(); i++) {
        if (variableStartIndex >= 0) {
          if (text.startsWith(end, i)) {
            String variableName = text.substring(variableStartIndex + start.length(), i);
            sb.append(evaluate(variableName, context, nullToEmpty));
            variableStartIndex = -1;
            i += end.length() - 1;
          }
        } else {
          if (text.startsWith(start, i)) {
            variableStartIndex = i;
          } else {
            sb.append(text.charAt(i));
          }
        }
      }

      return sb.toString();
    };
  }

  private String evaluate(String variableName, Context context, boolean nullToEmpty) {
    Object o = resolve(variableName, context);
    if (o == null && nullToEmpty) {
      return "";
    }
    return String.valueOf(o);
  }

  @SuppressWarnings("unchecked")
  private <T> T resolve(String expression, Context context) {
    Iterator<String> iter = Splitter.on('.').split(expression).iterator();

    Object reference = context.resolve(iter.next());

    while (iter.hasNext()) {
      String s = iter.next();
      if (s.endsWith("()")) {
        String method = s.substring(0, s.length() - 2);
        if (reference == null) {
          throw new NullPointerException("Tried to invoke a method on a null reference: " + expression);
        }
        reference = invokeMethod(reference, method);
      } else {
        reference = dereference(reference, s);
      }
    }

    return (T) reference;
  }

  private Object invokeMethod(Object o, String method) {
    if (method.equals("isEmpty")) {
      if (o == null) {
        return true;
      }
      if (o instanceof Json) {
        return ((Json) o).isEmpty();
      } else if (o instanceof Iterable) {
        return !((Iterable<?>) o).iterator().hasNext();
      } else if (o instanceof Multimap) {
        return ((Multimap<?, ?>) o).isEmpty();
      } else {
        return false;
      }
    } else if (method.equals("hasData")) {
      return !((boolean) invokeMethod(o, "isEmpty"));
    } else if (method.equals("size")) {
      if (o == null) {
        return 0;
      } else if (o instanceof Json) {
        return ((Json) o).size();
      } else if (o instanceof Iterable) {
        return size((Iterable<?>) o);
      } else {
        throw new RuntimeException("Don't know how to get size from a " + o.getClass());
      }
    } else {
      return Reflection.call(o, method);
    }
  }

  private Object dereference(Object o, String fieldName) {
    if (o == null) {
      return null;
    } else if (o instanceof Json) {
      Json json = (Json) o;
      return json.getObject(fieldName);
    } else if (o instanceof Entry) {
      Entry<?, ?> e = (Entry<?, ?>) o;
      if (fieldName.equals("key")) {
        return e.getKey();
      } else if (fieldName.equals("value")) {
        return e.getValue();
      } else {
        throw new RuntimeException("Invalid map.entry accessor: " + fieldName);
      }
    } else if (o instanceof Map) {
      return ((Map<?, ?>) o).get(fieldName);
    } else {
      return Reflection.get(o, fieldName);
    }
  }

  public static Template compile(String source, StaticContentHandler loader) {
    return new Template(source, loader);
  }

  public static Template compile(String source) {
    return new Template(source);
  }

}
