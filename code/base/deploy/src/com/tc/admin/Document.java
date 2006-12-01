/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Document {
  private StringBuffer out;
  int                  indent       = 0;

  private Map          fontParamMap = new HashMap();
  private StringBuffer fontParams   = new StringBuffer();

  public Document() {
    this(new StringBuffer());
  }

  public Document(Document parent) {
    this(new StringBuffer());
    this.indent = parent.indent;
    this.fontParamMap.putAll(parent.fontParamMap);
    bakeFontParams();
  }

  public Document(StringBuffer out) {
    this.out = out;
  }

  public String toString() {
    return out.toString();
  }

  public Document otag(Object name) {
    return otag(name, "");
  }

  public Document otag(Object name, Object params) {
    indent();
    print("<" + name);
    if (params != null) {
      print(params);
    }
    return println(">");
  }

  public Document ctag(Object name) {
    return outdent().println("</" + name + ">");
  }

  public Document octag(Object name) {
    return octag(name, "");
  }

  public Document octag(Object name, Object params) {
    return print("<" + name + (params == null ? "" : params) + "/>");
  }

  public Document html() {
    return otag("html");
  }

  public Document chtml() {
    return ctag("html");
  }

  public Document head() {
    return otag("head");
  }

  public Document chead() {
    return ctag("head");
  }

  public Document title(Object title) {
    return otag("title").println(title).ctag("title");
  }

  public Document body() {
    return otag("body");
  }

  public Document cbody() {
    return ctag("body");
  }

  public Document comment(Object comment) {
    return println("<!--" + comment + "-->");
  }

  public String param(Object name, Object value) {
    return " " + name + "=\"" + value + "\"";
  }

  public Document text(Object text) {
    if (text != null) {
      return print(escape(text.toString()));
    } else {
      return this;
    }
  }

  private String escape(String source) {
    if (source == null) return null;

    StringBuffer myOut = new StringBuffer();
    char[] sourceChars = source.toCharArray();
    for (int i = 0; i < sourceChars.length; ++i) {
      char theChar = sourceChars[i];
      String toAppend = new String(new char[] { theChar });

      if (theChar == '<') toAppend = "&lt;";
      if (theChar == '>') toAppend = "&gt;";
      if (theChar == '&') toAppend = "&amp;";
      if (theChar > 0x7E || theChar < 0x20) toAppend = "&#" + Integer.toHexString(theChar) + ";";

      myOut.append(toAppend);
    }

    return myOut.toString();
  }

  public Document img(Object url) {
    return img(url, " border=0");
  }

  public Document img(Object url, Object params) {
    return otag("img src=\"" + url + "\" ", params);
  }

  public Document href(Object url, String text) {
    return otag("a href=\"" + url + "\"").print(text).ctag("a");
  }

  public Document br() {
    return octag("br");
  }

  public Document hr() {
    return octag("hr");
  }

  public Document ul() {
    return ul("");
  }

  public Document ul(String params) {
    return otag("ul", params);
  }

  public Document cul() {
    return ctag("ul");
  }

  public Document li() {
    return li("");
  }

  public Document li(String params) {
    return otag("li", params);
  }

  public Document cli() {
    return ctag("li");
  }

  public Document style() {
    return style("");
  }

  public Document style(Object params) {
    return otag("style", params);
  }

  public Document cstyle() {
    return ctag("style");
  }

  private void bakeFontParams() {
    fontParams.delete(0, fontParams.length());
    Object key;
    for (Iterator iter = fontParamMap.keySet().iterator(); iter.hasNext();) {
      key = iter.next();
      fontParams.append(param(key, fontParamMap.get(key)));
    }
  }

  public Document fontparam(Object name, Object value) {
    fontParamMap.put(name, value);
    bakeFontParams();
    return this;
  }

  public Document dfontparam(Object name) {
    fontParamMap.remove(name);
    bakeFontParams();
    return this;
  }

  public Document fontclear() {
    fontParamMap.clear();
    bakeFontParams();
    return this;
  }

  public Document font() {
    return font(fontParams);
  }

  public Document font(Object params) {
    return otag("font", params);
  }

  public Document cfont() {
    return ctag("font");
  }

  public Document table() {
    return table("");
  }

  public Document table(String params) {
    return otag("table", params);
  }

  public Document ctable() {
    return ctag("table");
  }

  public Document row(String data) {
    tr().print(data).ctr();
    return this;
  }

  public Document cell(Object o) {
    return cell(o == null ? "" : o.toString());
  }

  public Document cell(String data) {
    td().println(data).ctd();
    return this;
  }

  public Document tr() {
    return otag("tr");
  }

  public Document ctr() {
    return ctag("tr");
  }

  public Document th() {
    return th("");
  }

  public Document th(Object params) {
    return otag("th", params);
  }

  public Document cth() {
    return ctag("th");
  }

  public Document td() {
    return td("");
  }

  public Document td(Object params) {
    return otag("td", params);
  }

  public Document ctd() {
    return ctag("td");
  }

  Document indent() {
    indent += 2;
    return this;
  }

  Document outdent() {
    indent = indent - 2;
    return this;
  }

  public Document newline() {
    out.append("\n");
    for (int i = 0; i < indent; i++) {
      out.append(" ");
    }
    return this;
  }

  public Document println(Object data) {
    print(data);
    newline();
    return this;
  }

  public Document print(Object data) {
    out.append(data);
    return this;
  }

}