package bowser.template;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.size;
import static ox.util.Utils.propagate;
import static ox.util.Utils.trim;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.html.HtmlEscapers;

import bowser.misc.CacheBuster;
import bowser.model.Controller;
import bowser.node.DomNode;
import bowser.node.DomParser;
import bowser.node.Head;
import bowser.node.TextNode;
import bowser.template.Imports.MediaType;
import ox.IO;
import ox.Json;
import ox.Log;
import ox.Money;
import ox.Reflection;

public class Template {

  public static boolean mobileDisplay = false;

  private final DomNode root;
  private DomNode head = null;

  private boolean isRoot;

  private DomParser parser;

  private Template(String s, Controller controller, DomParser parser, boolean embedCSS) {
    this.parser = parser;

    isRoot = true;
    root = parser.parse(s, isRoot);

    init(root, controller, embedCSS);
  }

  private void init(DomNode root, Controller controller, boolean embedCSS) {
    CacheBuster buster = controller.getServer().getCacheBuster();
    for (DomNode node : root.getAllNodes()) {
      if ("js".equals(node.tag)) {
        Iterable<String> jsFiles = Splitter.on(' ').split(node.getAttribute("src"));
        if (node.hasAttribute("inline")) {
          List<DomNode> jsNodes = Imports.importJSInline(jsFiles, controller);
          node.parent.replace(node, jsNodes);
        } else {
          jsFiles = Iterables.transform(jsFiles, jsFile -> buster.hashPath(jsFile, controller));
          Imports.importJSToHead(jsFiles, head, node.hasAttribute("defer"));
          node.parent.remove(node);
        }
      } else if ("css".equals(node.tag)) {
        Iterable<String> cssFiles = Splitter.on(' ').split(node.getAttribute("src"));
        cssFiles = Iterables.transform(cssFiles, cssFile -> buster.hashPath(cssFile, controller));
        MediaType mediaType = node.hasAttribute("print") ? MediaType.PRINT : MediaType.SCREEN;
        Imports.importCSSToHead(cssFiles, head, mediaType);
        node.parent.remove(node);
      } else if ("head".equals(node.tag)) {
        if (head == null) {
          head = node;
        } else {
          Imports.appendToHead(head, node, controller, embedCSS);
          node.parent.remove(node);
        }
      } else if ("import".equals(node.tag)) {
        List<DomNode> importedNodes = Imports.createImport(node, controller, parser);
        String iff = node.getAttribute("if");
        if (iff != null) {
          DomNode span = new DomNode("span");
          span.add(importedNodes);
          span.attribute("if", iff);
          importedNodes = ImmutableList.of(span);
        }
        node.parent.replace(node, importedNodes);
        for (DomNode importedNode : importedNodes) {
          init(importedNode, controller, embedCSS);
        }
      } else if (node.hasAttribute("css")) {
        String cssFileName = node.getAttribute("css");
        String cssData = IO.from(controller.getServer().getResourceLoader().getData(cssFileName, controller))
            .toString();
        String scopedCSS = CSSUtils.addScope(node, cssData, "[css='" + cssFileName + "']");
        node.add(new DomNode("style").text(scopedCSS));
      }
    }
  }

  public String render(Context context) {
    StringBuilder sb = new StringBuilder();
    if (isRoot) {
      sb.append("<!DOCTYPE html>\n");
    }
    render(root, sb, 0, false, context);
    return sb.toString();
  }

  private void render(DomNode node, StringBuilder sb, int depth, boolean insideTemplate, Context context) {
    if (!insideTemplate) {
      String loop = node.getAttribute("loop");
      if (loop != null && !"video".equals(node.tag)) {
        renderLoop(node, sb, depth, insideTemplate, context, loop);
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
    }

    if (node instanceof TextNode) {
      renderText((TextNode) node, sb, depth, insideTemplate, context);
    } else {
      if (node.tag.equalsIgnoreCase("template")) {
        insideTemplate = true;
      }

      Function<String, String> a = replacer(context, "{", "}", true, true);
      Function<String, String> b = replacer(context, "$$(", ")", true, true);
      node.renderStartTag(sb, depth, a.andThen(b));

      for (DomNode child : node.getChildren()) {
        render(child, sb, depth + 1, insideTemplate, context);
      }

      node.renderEndTag(sb, depth);
    }
  }

  // TODO jason, rewrite this
  private boolean resolveBoolean(String s, Context context) {
    s = s.trim();
    int i = s.indexOf("&&");
    if (i != -1) {
      return resolveBoolean(s.substring(0, i), context) && resolveBoolean(s.substring(i + 2), context);
    }

    i = s.indexOf("||");
    if (i != -1) {
      return resolveBoolean(s.substring(0, i), context) || resolveBoolean(s.substring(i + 2), context);
    }

    i = s.indexOf("==");
    if (i != -1) {
      String a = s.substring(0, i);
      String b = s.substring(i + 2);

      return equals(resolve(a, context), resolve(b, context));
    }

    i = s.indexOf("!=");
    if (i != -1) {
      String a = s.substring(0, i);
      String b = s.substring(i + 2);
      return !equals(resolve(a, context), resolve(b, context));
    }

    i = s.indexOf(">=");
    if (i != -1) {
      Number aNum = resolve(s.substring(0, i), context);
      Number bNum = resolve(s.substring(i + 2), context);
      return aNum.doubleValue() >= bNum.doubleValue();
    }

    i = s.indexOf(">");
    if (i != -1) {
      Number aNum = resolve(s.substring(0, i), context);
      Number bNum = resolve(s.substring(i + 1), context);
      return aNum.doubleValue() > bNum.doubleValue();
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
    } else if (o instanceof String) {
      return !((String) o).isEmpty();
    } else if (o instanceof Json) {
      Json j = (Json) o;
      if (j.isArray()) {
        return j.size() > 0;
      } else {
        return j.iterator().hasNext();
      }
    } else if (o instanceof Iterable) {
      return ((Iterable<?>) o).iterator().hasNext();
    } else if (o instanceof Number) {
      return ((Number) o).intValue() != 0;
    } else if (o instanceof Money) {
      return !((Money) o).isZero();
    } else {
      return true;
    }
  }

  private boolean equals(Object a, Object b) {
    if (a == null || b == null) {
      return a == null && b == null;
    }
    if (a.getClass().isEnum()) {
      a = ((Enum<?>) a).name();
    }
    if (b.getClass().isEnum()) {
      b = ((Enum<?>) b).name();
    }
    return a.toString().equalsIgnoreCase(b.toString());
  }

  @SuppressWarnings("unchecked")
  private void renderLoop(DomNode node, StringBuilder sb, int depth, boolean insideTemplate, Context context,
      String loop) {
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
        Object oldValue = context.put(variableName, entry);
        render(new DomNode(node).removeAttribute("loop"), sb, depth, insideTemplate, context);
        context.put(variableName, oldValue);
      });
    } else if (data instanceof Json && ((Json) data).isObject()) {
      Json json = (Json) data;
      for (String key : json) {
        Object oldVal1 = context.put(variableName, key);
        Object oldVal2 = context.put("value", json.getObject(key));
        render(new DomNode(node).removeAttribute("loop"), sb, depth, insideTemplate, context);
        context.put(variableName, oldVal1);
        context.put("value", oldVal2);
      }
    } else if (data instanceof Iterable) {
      for (Object o : (Iterable<?>) data) {
        Object oldValue = context.put(variableName, o);
        render(new DomNode(node).removeAttribute("loop"), sb, depth, insideTemplate, context);
        context.put(variableName, oldValue);
      }
    } else if (data instanceof Multimap) {
      Multimap<Object, Object> multimap = (Multimap<Object, Object>) data;
      for (Object key : multimap.keySet()) {
        Object oldVal1 = context.put(variableName, key);
        Object oldVal2 = context.put("values", multimap.get(key));
        render(new DomNode(node).removeAttribute("loop"), sb, depth, insideTemplate, context);
        context.data.put(variableName, oldVal1);
        context.data.put("values", oldVal2);
      }
    } else {
      throw new RuntimeException("Unhandled data type: " + data.getClass());
    }
  }

  private void renderText(TextNode node, StringBuilder sb, int depth, boolean insideTemplate, Context context) {
    String text = node.content;

    if (insideTemplate || node.parent.tag.equals("style")) {
      sb.append(text);
      return;
    }

    if (node.parent.tag.equals("script") || node.parent.tag.equals("svg")) {
      Function<String, String> replacer = replacer(context, "$$(", ")", false, false);
      text = replacer.apply(text);
    } else if (node.parent.tag.equals("code")) {
      Function<String, String> replacer = replacer(context, "$$(", ")", false, true);
      text = replacer.apply(text);
    } else {
      boolean escapeHtml = true;
      if (node.parent.hasAttribute("allowHtml")) {
        escapeHtml = false;
      }
      text = replacer(context, "$$(", ")", true, escapeHtml).apply(text);
      text = replacer(context, "{", "}", true, escapeHtml).apply(text);
    }

    sb.append(text);
  }

  private Function<String, String> replacer(Context context, String start, String end, boolean nullToEmpty,
      boolean escapeHtml) {
    return text -> {
      if (text == null) {
        return null;
      }

      StringBuilder sb = new StringBuilder();

      boolean parens = start.contains("(");

      int variableStartIndex = -1;
      int depth = 0; // depth lets us handle cases like $$(foo.get())
      for (int i = 0; i < text.length(); i++) {
        if (variableStartIndex >= 0) {
          if (text.startsWith(end, i)) {
            depth--;
            if (depth == 0) {
              String variableName = text.substring(variableStartIndex + start.length(), i);
              sb.append(evaluate(variableName, context, nullToEmpty, escapeHtml));
              variableStartIndex = -1;
              i += end.length() - 1;
            }
          } else if (parens && text.charAt(i) == '(') {
            depth++;
          }
        } else {
          if (text.startsWith(start, i)) {
            variableStartIndex = i;
            i += start.length() - 1;
            depth = 1;
          } else {
            sb.append(text.charAt(i));
          }
        }
      }

      return sb.toString();
    };
  }

  private String evaluate(String variableName, Context context, boolean nullToEmpty, boolean escapeHtml) {
    Object o = resolve(variableName, context);
    if (o == null && nullToEmpty) {
      return "";
    }
    // Log.debug(variableName + " = " + o);
    String ret = String.valueOf(o);
    if (ret != null && (escapeHtml || ret.toLowerCase().contains("<script>"))) {
      ret = HtmlEscapers.htmlEscaper().escape(ret);
    }
    return ret;
  }

  @SuppressWarnings("unchecked")
  private <T> T resolve(String expression, Context context) {
    expression = trim(expression);

    if (expression.isEmpty()) {
      return null;
    }

    checkState(!expression.contains(" "), "Invalid variable: " + expression);

    if (expression.startsWith("'") && expression.endsWith("'")) {
      return (T) expression.substring(1, expression.length() - 1);
    }

    if (expression.equals("null")) {
      return null;
    }

    if (Character.isDigit(expression.charAt(0))) {
      return (T) Long.valueOf(expression);
    }

    Iterator<String> iter = Splitter.on('.').split(expression).iterator();

    Object reference = context.get(iter.next());

    while (iter.hasNext()) {
      String s = iter.next();
      if (s.endsWith("()")) {
        String method = s.substring(0, s.length() - 2);
        if (reference == null) {
          return (T) "";
        }
        reference = invokeMethod(reference, method);
      } else {
        try {
          reference = dereference(reference, s);
        } catch (Exception e) {
          Log.error("Problem resolving expression: " + expression);
          throw propagate(e);
        }
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

  public static Template compile(String source) {
    return compile(source, null, false, false);
  }

  public static Template compile(String source, Controller controller, boolean embedCSS,
      boolean debugMode) {
    return compile(source, controller, embedCSS, debugMode, true);
  }

  public static Template compile(String source, Controller controller, boolean embedCSS,
      boolean debugMode, boolean renderHead) {
    Head head = controller == null || !renderHead ? null : controller.getServer().getHead();
    return new Template(source, controller, new DomParser(head, debugMode), embedCSS);
  }

}
