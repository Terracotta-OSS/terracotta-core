/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.test.schema;

import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The root for all config-builder classes.
 */
public abstract class BaseConfigBuilder {

  private final Map setProperties;
  private final Map arrayPropertyTagNames;
  private final Set allPropertyNames;

  protected int     currentIndentLevel;

  protected BaseConfigBuilder(int indentLevel, String[] allProperties) {
    Assert.eval(indentLevel >= 0);

    this.setProperties = new HashMap();
    this.arrayPropertyTagNames = new HashMap();
    this.allPropertyNames = new HashSet();
    this.allPropertyNames.addAll(Arrays.asList(allProperties));

    this.currentIndentLevel = indentLevel;
  }

  protected final void setArrayPropertyTagName(String property, String tagName) {
    Assert.assertNotNull(property);
    Assert.assertNotNull(tagName);
    this.arrayPropertyTagNames.put(property, tagName);
  }

  protected final void setProperty(String property, int value) {
    setProperty(property, Integer.toString(value));
  }

  protected final void setProperty(String property, boolean value) {
    setProperty(property, Boolean.valueOf(value));
  }

  protected final void setProperty(String property, Object value) {
    Assert.assertNotBlank(property);
    Assert.assertNotNull(value);
    Assert.eval(this.allPropertyNames.contains(property));

    if (value instanceof Object[]) {
      if (this.arrayPropertyTagNames.containsKey(property)) {
        setProperty(property, array((String) this.arrayPropertyTagNames.get(property), (Object[]) value));
      } else {
        setProperty(property, array(null, (Object[]) value));
      }
    } else this.setProperties.put(property, value);
  }

  protected final void unsetProperty(String property) {
    Assert.assertNotBlank(property);
    Assert.eval(this.allPropertyNames.contains(property));
    this.setProperties.remove(property);
  }

  protected String getProperty(String property) {
    Assert.assertNotBlank(property);
    Assert.eval(this.allPropertyNames.contains(property));
    Assert.eval(this.setProperties.containsKey(property));
    return this.setProperties.get(property).toString();
  }

  protected final Object getRawProperty(String property) {
    Assert.assertNotBlank(property);
    Assert.eval(this.allPropertyNames.contains(property));
    Assert.eval(this.setProperties.containsKey(property));
    return this.setProperties.get(property);
  }

  protected final class SelfTaggingArray {
    private final Object[] values;

    public SelfTaggingArray(Object[] values) {
      Assert.assertNoNullElements(values);
      this.values = values;
    }

    public Object[] values() {
      return this.values;
    }

    @Override
    public String toString() {
      ++currentIndentLevel;

      String out = "\n";
      for (int i = 0; i < this.values.length; ++i) {
        String value = this.values[i].toString();
        out += value;
        if (value.indexOf("\n") < 0) value += "\n";
      }

      --currentIndentLevel;
      out += indent();
      return out;
    }
  }

  protected final Object selfTaggingArray(Object[] values) {
    Assert.assertNoNullElements(values);
    return new SelfTaggingArray(values);
  }

  private class Array {
    private final String   elementTag;
    private final Object[] values;

    public Array(String elementTag, Object[] values) {
      Assert.assertNoNullElements(values);

      this.elementTag = elementTag;
      this.values = values;
    }

    @Override
    public String toString() {
      ++currentIndentLevel;

      String out = "";
      for (int i = 0; i < this.values.length; ++i) {
        String value = this.values[i].toString();

        if (this.elementTag != null) {
          out += indent() + "<" + this.elementTag + ">";
          if (value.indexOf("\n") >= 0) out += "\n";
        }
        out += this.values[i];
        if (this.elementTag != null) {
          if (value.indexOf("\n") >= 0 && (!value.endsWith("\n"))) out += "\n";
          if (value.indexOf("\n") >= 0) out += indent();
          out += "</" + this.elementTag + ">\n";
        }
      }

      --currentIndentLevel;
      out += indent();
      return out;
    }
  }

  private Object array(String elementTag, Object[] values) {
    Assert.assertNoNullElements(values);
    return new Array(elementTag, values);
  }

  public final boolean isSet(String property) {
    Assert.assertNotBlank(property);
    return this.setProperties.containsKey(property);
  }

  protected final String indent() {
    return indent(this.currentIndentLevel);
  }

  private String indent(int level) {
    String out = "\n";

    for (int i = 0; i < level; ++i) {
      out += "  ";
    }

    return out;
  }

  protected final String elementGroup(String tagName, String[] elementNames) {
    return openElement(tagName, elementNames) + elements(elementNames) + closeElement(tagName, elementNames);
  }

  // We do this to make sure that order doesn't matter in our config file, which is generally what should be the case.
  protected final String elements(String[] names) {
    String out = "";

    List list = new ArrayList();
    list.addAll(Arrays.asList(names));
    Collections.shuffle(list);

    Iterator iter = list.iterator();
    while (iter.hasNext()) {
      out += element((String) iter.next());
    }

    return out;
  }

  protected final String element(String tagName, String propertyName) {
    Assert.assertNotBlank(tagName);
    Assert.assertNotBlank(propertyName);

    if (isSet(propertyName)) {
//      return openElement(tagName) + getProperty(propertyName) + closeElement(tagName);
      return indent() + "<" + tagName + ">" + getProperty(propertyName) + "</" + tagName + ">";
    }
    else return "";
  }

  protected final String element(String name) {
    return element(name, name);
  }

  protected final String openElement(String tagName) {
    String out = indent() + "<" + tagName + ">\n";
    ++this.currentIndentLevel;
    return out;
  }

  protected final String selfCloseElement(String tagName, Map attributes) {
    Assert.assertNotBlank(tagName);
    String out = indent() + "<" + tagName + " " + attributesToString(attributes) + "/>";
    return out;
  }

  protected final String openElement(String tagName, String[] properties) {
    Assert.assertNotBlank(tagName);
    if (anyAreSet(properties)) return openElement(tagName);
    else return "";
  }

  protected final boolean anyAreSet(String[] properties) {
    boolean needIt = false;

    for (int i = 0; i < properties.length; ++i) {
      if (isSet(properties[i])) needIt = true;
    }

    return needIt;
  }

  protected final String closeElement(String tagName) {
    --this.currentIndentLevel;
    return indent() + "</" + tagName + ">" + "\n";
  }

  protected final String closeElement(String tagName, String[] properties) {
    Assert.assertNotBlank(tagName);
    if (anyAreSet(properties)) return closeElement(tagName);
    else return "";
  }

  protected String openElement(String tagName, Map attributes) {
    String out = indent() + "<" + tagName + attributesToString(attributes) + ">\n";
    ++this.currentIndentLevel;
    return out;
  }

  private String attributesToString(Map attributes) {
    StringBuffer sb = new StringBuffer();
    for (Iterator it = attributes.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Map.Entry) it.next();
      sb.append(' ');
      sb.append(entry.getKey());
      sb.append("=\"");
      sb.append(entry.getValue());
      sb.append("\"");
    }
    return sb.toString();
  }

  protected String propertyAsString(String propertyName) {
    return getProperty(propertyName).toString();
  }

  protected static String[] concat(Object[] data) {
    List out = new ArrayList();

    for (int i = 0; i < data.length; ++i) {
      if (data[i] instanceof String) out.add(data[i]);
      else {
        String[] theData = (String[]) data[i];
        for (int j = 0; j < theData.length; ++j)
          out.add(theData[j]);
      }
    }

    Collections.shuffle(out);
    return (String[]) out.toArray(new String[out.size()]);
  }

}
