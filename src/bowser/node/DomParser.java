package bowser.node;

import java.util.List;

import com.google.common.collect.Lists;

import ox.Log;

public class DomParser {

  public final Head head;
  public boolean debugMode = false;

  public DomParser() {
    this(Head.defaults(""), false);
  }

  public DomParser(Head head, boolean debugMode) {
    this.head = head;
    this.debugMode = debugMode;
  }

  public DomNode parse(String s, boolean isRoot) {
    DomNode root;
    if (isRoot && head != null) {
      Head headCopy = head.copy();

      root = new DomNode("html").attribute("lang", "en");
      root.add(new TextNode("\n"));
      root.add(headCopy);
      DomNode body = new DomNode("body");
      root.add(body);
      parse(headCopy, body, s, 0, s.length());
      processMetaTags(root, headCopy);
    } else {
      root = new DomNode("div");
      parse(null, root, s, 0, s.length());
    }
    return root;
  }

  private void processMetaTags(DomNode root, Head head) {
    root.find("meta").forEach(meta -> {
      if (!head.contains(meta)) {
        if (meta.parent != null) {
          String iff = meta.parent.getAttribute("if", "");
          String metaIff = meta.getAttribute("if", "");
          if (!iff.isEmpty()) {
            if (metaIff.isEmpty()) {
              meta.attribute("if", iff);
            } else {
              meta.replaceAttribute("if", iff + " && " + metaIff);
            }
          }
        }
        head.add(meta);
      }
    });
  }

  private void parse(Head head, DomNode parent, String s, int start, int end) {
    if (start == end) {
      return;
    }

    if (s.charAt(start) != '<') {
      int tagIndex = end;
      for (int i = start + 1; i < end; i++) {
        if (s.charAt(i) == '<') {
          tagIndex = i;
          break;
        }
      }
      parent.add(new TextNode(s.substring(start, tagIndex)));
      parse(head, parent, s, tagIndex, end);
      return;
    }

    if (s.charAt(start + 1) == '!') {
      int i = s.indexOf("-->", start);
      if (i != -1) {
        parent.add(new TextNode(s.substring(start, i + 3)));
        parse(head, parent, s, i + 3, end);
      }
      return;
    }

    int endTag = findEndOfStartTag(s, start);

    String tagData = s.substring(start + 1, endTag);
    List<String> m = split(tagData, ' ');

    DomNode node = new DomNode(m.get(0));
    for (int i = 1; i < m.size(); i++) {
      String attribute = m.get(i);
      int index = attribute.indexOf('=');
      if (index == -1) {
        node.attribute(attribute);
      } else {
        node.attribute(attribute.substring(0, index), attribute.substring(index + 1));
      }
    }

    parent.add(node);

    Integer endTagIndex = findEndTag(node.tag, s, endTag + 1, end);
    if (endTagIndex != null) {
      if (node.tag.equalsIgnoreCase("script") /* || node.tag.equalsIgnoreCase("template") */
          || node.tag.equalsIgnoreCase("code") || node.tag.equalsIgnoreCase("svg")) {
        node.add(new TextNode(s.substring(endTag + 1, endTagIndex)));
      } else {
        // Recursive step: parse the children of a node, adding them to the node.
        parse(head, node, s, endTag + 1, endTagIndex);
      }
      endTag = endTagIndex + 2 + node.tag.length();
    }

    if (node.tag.equalsIgnoreCase("title")) {
      if (head == null) {
        Log.warn("<head> is null, skipping title tag.");
      } else {
        parent.remove(node);
        head.add(node);
      }
    }

    parse(head, parent, s, endTag + 1, end);
  }

  private int findEndOfStartTag(String s, int start) {
    boolean insideQuotes = false;
    for (int i = start; i < s.length(); i++) {
      char c = s.charAt(i);
      if (insideQuotes) {
        if (c == '"') {
          insideQuotes = false;
        }
      } else {
        if (c == '>') {
          return i;
        } else if (c == '"') {
          insideQuotes = true;
        }
      }
    }
    throw new RuntimeException("Could not find end of start tag!");
  }

  private Integer findEndTag(String tag, String s, int start, int end) {
    int n = 1;
    while (true) {
      int i = s.indexOf("<" + tag, start);
      int j = s.indexOf("</" + tag, start);

      boolean startTag = i < end && i != -1;
      boolean endTag = j < end && j != -1;

      if (startTag && endTag) {
        if (i < j) {
          endTag = false;
        } else {
          startTag = false;
        }
      }

      if (!startTag && !endTag) {
        return null;
      }

      if (startTag) {
        n++;
        start = i + 1;
      } else if (endTag) {
        n--;
        start = j + 1;
        if (n == 0) {
          return j;
        }
      }
    }
  }

  /**
   * Same as a normal string split, except it doesn't split if inside a quoted literal. Important for things like
   * class="big red" href="blah.com", as we don't want to split apart "big red" at the space.
   */
  private static List<String> split(String s, char z) {
    List<String> ret = Lists.newArrayList();
    StringBuilder sb = new StringBuilder();
    boolean escaped = false;
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"') {
        escaped = !escaped;
      } else {
        if (!escaped && c == z) {
          ret.add(sb.toString());
          sb.setLength(0);
        } else {
          sb.append(c);
        }
      }
    }
    if (sb.length() > 0) {
      ret.add(sb.toString());
    }
    return ret;
  }

}
