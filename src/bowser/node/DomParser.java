package bowser.node;

import java.util.List;

import com.google.common.collect.Lists;

import ox.Log;

public class DomParser {

  public final Head head;

  public DomParser() {
    this(Head.defaults(""));
  }

  public DomParser(Head head) {
    this.head = head;
  }

  public DomNode parse(String s, boolean isRoot) {
    DomNode root;
    if (isRoot && head != null) {
      DomNode headCopy = head.copy();

      root = new DomNode("html").attribute("lang", "en");
      root.add(new TextNode("\n"));
      root.add(headCopy);
      DomNode body = new DomNode("body");
      root.add(body);
      parse(headCopy, body, s, 0, s.length());
    } else {
      root = new DomNode("div");
      parse(null, root, s, 0, s.length());
    }
    return root;
  }

  private void parse(DomNode head, DomNode parent, String s, int start, int end) {
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

    int endTag = s.indexOf('>', start);

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

    if (node.tag.equalsIgnoreCase("meta")) {
      if (head == null) {
        Log.warn("<head> is null, skipping meta tag.");
      } else {
        head.add(node);
      }
    } else {
      parent.add(node);
    }

    Integer endTagIndex = findEndTag(node.tag, s, endTag + 1, end);
    if (endTagIndex != null) {
      if (node.tag.equalsIgnoreCase("script")) {
        node.add(new TextNode(s.substring(endTag + 1, endTagIndex)));
      } else {
        parse(head, node, s, endTag + 1, endTagIndex); // add a child
      }
      endTag = endTagIndex + 2 + node.tag.length();
    }

    parse(head, parent, s, endTag + 1, end);
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
