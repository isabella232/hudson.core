/*******************************************************************************
 *
 * Copyright (c) 2011 Oracle Corporation.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *    Nikita Levyankov
 *
 *******************************************************************************/
//Move patched logic from prototype library.
Ajax.Request.prototype = Object.extend(new Ajax.Base(), {
  respondToReadyState: function(readyState) {
    var state = Ajax.Request.Events[readyState];
    var transport = this.transport, json = this.evalJSON();

    if (state == 'Complete') {
      try {
        this._complete = true;
        (this.options['on' + this.transport.status]
         || this.options['on' + (this.success() ? 'Success' : 'Failure')]
         || Prototype.emptyFunction)(transport, json);
      } catch (e) {
        this.dispatchException(e);
      }

//      var contentType = this.getHeader('Content-type');
//      if (contentType && contentType.strip().
//        match(/^(text|application)\/(x-)?(java|ecma)script(;.*)?$/i))
//          this.evalResponse();
    }

    try {
      (this.options['on' + state] || Prototype.emptyFunction)(transport, json);
      Ajax.Responders.dispatch('on' + state, this, transport, json);
    } catch (e) {
      this.dispatchException(e);
    }

    if (state == 'Complete') {
      // avoid memory leak in MSIE: clean up
      this.transport.onreadystatechange = Prototype.emptyFunction;
    }
  },
  evalResponse: function() {
    try {
      return eval('('+(this.transport.responseText || '').unfilterJSON()+')');
    } catch (e) {
      this.dispatchException(e);
    }
  }
});

Element.Methods = {
  hasClassName: function(element, className) {
    if (!(element = $(element))) return;
    var elementClassName = element.className;
    if (!elementClassName || elementClassName.length == 0) return false;
    if (elementClassName == className ||
        elementClassName.match(new RegExp("(^|\\s)" + className + "(\\s|$)")))
      return true;
    return false;
  }
}

Form.Methods = {
  getInputs: function(form, typeName, name) {
    form = $(form);
    var inputs = form.getElementsByTagName('input');
    var textareas = form.getElementsByTagName('textarea');
    // KK patch
    var selects = form.getElementsByTagName('select');

    if (!typeName && !name) return $A(inputs).concat($A(textareas)).concat($A(selects)).map(Element.extend);


    var matchingInputs = [];
    var f = function(inputs) {
      for (var i = 0, length = inputs.length; i < length; i++) {
        var input = inputs[i];
        if ((typeName && input.type != typeName) || (name && input.name != name))
          continue;
        matchingInputs.push(Element.extend(input));
      }
    };
    f(inputs);
    f(textareas);
    f(selects);

    return matchingInputs;
  }
}