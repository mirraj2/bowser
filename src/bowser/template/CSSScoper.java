package bowser.template;

import static ox.util.Utils.first;
import static ox.util.Utils.propagate;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import bowser.misc.SCSSProcessor;
import bowser.node.DomNode;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Rule;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.Selector.Combinator;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.SelectorImpl;

public class CSSScoper {

  static {
    System.setProperty("org.slf4j.simpleLogger.log.cz.vutbr.web.csskit", "warn");
  }

  private final SCSSProcessor processor;

  public CSSScoper(SCSSProcessor processor) {
    this.processor = processor;
  }

  public String addScope(DomNode root, String css, String cssFileName) {
    return addScope(root, css, cssFileName, null);
  }

  public String addScope(DomNode root, String css, String cssFileName, String url) {
    if (cssFileName.endsWith(".scss")) {
      byte[] data = processor.process(cssFileName, css.getBytes(StandardCharsets.UTF_8));
      css = new String(data, StandardCharsets.UTF_8);
    }
    try {
      StyleSheet ss = CSSFactory.parseString(css, url == null ? null : new URL(url));
      String scopeSelector = "[css='" + cssFileName + "']";
      addScope(root, ss, scopeSelector);

      StringBuilder sb = new StringBuilder();
      ss.forEach(rule -> sb.append(rule.toString()));
      return sb.toString().replace("\t", "  ");
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  private void addScope(DomNode root, Rule<?> rule, String scopeSelector) {
    rule.forEach(item -> {
      if (item instanceof RuleSet) {
        RuleSet rules = (RuleSet) item;
        for (CombinedSelector comboSelector : rules.getSelectors()) {
          if (matches(comboSelector, root)) {
            comboSelector.set(0, new StringSelector(comboSelector.get(0).toString() + scopeSelector));
          } else {
            SelectorImpl impl = new StringSelector(scopeSelector);
            comboSelector.add(0, impl);
            comboSelector.get(1).setCombinator(Combinator.DESCENDANT);
          }
        }
      } else {
        for (Object o : rule) {
          if (o instanceof Rule) {
            addScope(root, (Rule<?>) o, scopeSelector);
          }
        }
      }
    });

  }

  private static boolean matches(CombinedSelector combo, DomNode node) {
    Selector selector = first(combo);
    String s = selector.toString();
    int i = s.indexOf('[');
    if (i != -1) {
      s = s.substring(0, i);
    }
    if (s.charAt(0) == '.') {
      return node.getClasses().contains(s.substring(1));
    } else {
      return node.tag.equalsIgnoreCase(s);
    }
  }

  public static class StringSelector extends SelectorImpl {

    private final String s;

    public StringSelector(String s) {
      this.s = s;
    }

    @Override
    public String toString() {
      return s;
    }
  }

  // public static void main(String[] args) {
  // String from = IO.from(CSSUtils.class, "cssutils-test.css").toString();
  // DomNode root = new DomNode("chat").attribute("class", "test");
  // String to = CSSUtils.addScope(root, from, ".ENDER_SCOPE");
  // Log.debug(to);
  // }
}
