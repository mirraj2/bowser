package bowser.template;

import static ox.util.Utils.first;
import static ox.util.Utils.propagate;

import java.net.URL;

import bowser.node.DomNode;
import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Rule;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector;
import cz.vutbr.web.css.Selector.Combinator;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.SelectorImpl;
import ox.IO;
import ox.Log;

public class CSSUtils {

  static {
    System.setProperty("org.slf4j.simpleLogger.log.cz.vutbr.web.csskit", "warn");
  }

  public static String addScope(DomNode root, String css, String scopeSelector) {
    return addScope(root, css, scopeSelector, null);
  }

  public static String addScope(DomNode root, String css, String scopeSelector, String url) {
    try {
      StyleSheet ss = CSSFactory.parseString(css, url == null ? null : new URL(url));
      addScope(root, ss, scopeSelector);

      StringBuilder sb = new StringBuilder();
      ss.forEach(rule -> sb.append(rule.toString()));
      return sb.toString().replace("\t", "  ");
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  private static void addScope(DomNode root, Rule<?> rule, String scopeSelector) {
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
    if (selector.size() != 1) {
      return false;
    }
    String s = selector.toString();
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

  public static void main(String[] args) {
    String from = IO.from(CSSUtils.class, "cssutils-test.css").toString();
    DomNode root = new DomNode("chat").attribute("class", "test");
    String to = CSSUtils.addScope(root, from, ".ENDER_SCOPE");
    Log.debug(to);
  }
}
