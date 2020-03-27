$.fn.render = function(data) {
  let ret = $("<div>");
  $($(this).prop("content")).children().each(function() {
    Bowser.render(this, ret, data);

  });
  return ret.children();
}

const Bowser = function() {
  function render(node, output, context) {
    if (node.nodeType == Node.COMMENT_NODE) {
      return;
    }

    if (node.nodeType == Node.TEXT_NODE) {
      let clone = node.cloneNode();
      clone.textContent = stringReplace(clone.textContent, context);
      output.append(clone);
      return;
    }

    if (node.hasAttribute("loop")) {
      renderLoop(node, output, context);
      return;
    }

    let clone = node.cloneNode();
    for (var i = 0; i < clone.attributes.length; i++) {
      let attribute = clone.attributes[i];
      attribute.value = stringReplace(attribute.value, context);
    }
    node.childNodes.forEach(function(child) {
      render(child, clone, context);
    });
    output.append(clone);
  }

  function renderLoop(node, output, context) {
    const loop = node.getAttribute("loop");
    let m = loop.split(" ");
    if (m[1] != "in") {
      throw "Invalid loop: " + loop;
    }
    let variableName = m[0];
    let collectionName = m[2];
    let collection = resolve(collectionName, context);

    node.removeAttribute("loop");

    try {
      if (!collection) {
        return;
      }
      $(collection).each(function() {
        let oldValue = context[variableName];
        context[variableName] = this;
        render(node, output, context);
        context[variableName] = oldValue;
      });
    } finally {
      node.setAttribute("loop", loop);
    }
  }

  function resolve(expression, context) {
    let reference = null;
    expression.split(".").forEach(function(s) {
      try {
        if (reference == null) {
          reference = context[s];
        } else {
          reference = reference[s];
        }
      } catch (e) {
        console.error("Problem resolving expression: " + expression);
        throw e;
      }
    });
    return reference;
  }

  function evaluate(expression, context) {
    let ret = resolve(expression, context);
    if (ret == null) {
      ret = "";
    }
    return ret;
  }

  //TODO this method has a lot of room for optimization
  function stringReplace(text, context) {
    if (!text) {
      return text;
    }

    const start = "$$(";
    const end = ")";
    let sb = "";
    let variableStartIndex = -1;

    // depth lets us handle cases like $$(foo.get())
    //might not be relevant in bowser.js, TODO delete?
    let depth = 0;

    for (var i = 0; i < text.length; i++) {
      if (variableStartIndex >= 0) {
        if (text.startsWith(end, i)) {
          depth--;
          if (depth == 0) {
            let variableName = text.substring(variableStartIndex + start.length, i);
            sb += evaluate(variableName, context);
            variableStartIndex = -1;
            i += end.length - 1;
          }
        } else if (text.charAt(i) == '(') {
          depth++;
        }
      } else {
        if (text.startsWith(start, i)) {
          variableStartIndex = i;
          i += start.length - 1;
          depth = 1;
        } else {
          sb += text.charAt(i);
        }
      }
    }
    return sb;
  }

  return {
    render: render
  }
}();
