package bowser;

import static ox.util.Utils.propagate;

import java.net.URL;

import cz.vutbr.web.css.CSSFactory;
import cz.vutbr.web.css.CombinedSelector;
import cz.vutbr.web.css.Rule;
import cz.vutbr.web.css.RuleSet;
import cz.vutbr.web.css.Selector.Combinator;
import cz.vutbr.web.css.StyleSheet;
import cz.vutbr.web.csskit.SelectorImpl;
import ox.IO;
import ox.Log;

public class CSSUtils {

  static {
    System.setProperty("org.slf4j.simpleLogger.log.cz.vutbr.web.csskit", "warn");
  }

  public static String addScope(String css, String scopeSelector) {
    return addScope(css, scopeSelector, null);
  }

  public static String addScope(String css, String scopeSelector, String url) {
    try {
      StyleSheet ss = CSSFactory.parseString(css, url == null ? null : new URL(url));
      addScope(ss, scopeSelector);

      StringBuilder sb = new StringBuilder();
      ss.forEach(rule -> sb.append(rule.toString()));
      return sb.toString().replace("\t", "  ");
    } catch (Exception e) {
      throw propagate(e);
    }
  }

  private static void addScope(Rule<?> rule, String scopeSelector) {
    rule.forEach(item -> {
      if (item instanceof RuleSet) {
        RuleSet rules = (RuleSet) item;
        for (CombinedSelector comboSelector : rules.getSelectors()) {
          SelectorImpl impl = new StringSelector(scopeSelector);
          // impl.unlock().add(new MyElementClassImpl("DING"));
          comboSelector.add(0, impl);
          comboSelector.get(1).setCombinator(Combinator.DESCENDANT);
        }
      } else {
        for (Object o : rule) {
          if (o instanceof Rule) {
            addScope((Rule<?>) o, scopeSelector);
          }
        }
      }
    });

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
    String from = IO.from(CSSUtils.class, "test.css").toString();
    String to = CSSUtils.addScope(from, ".ENDER_SCOPE");
    Log.debug(to);
  }
}
