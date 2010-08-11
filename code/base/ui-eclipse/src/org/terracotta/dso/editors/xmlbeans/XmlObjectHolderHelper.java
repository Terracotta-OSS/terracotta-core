/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.SchemaStringEnumEntry;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlInteger;
import org.apache.xmlbeans.XmlObject;
import org.apache.xmlbeans.XmlToken;

import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

// TODO: assign all static values to instance variable instead of constant
// reflection-based lookup. Examples include: isRequired, hasDefault, defaultText.

public class XmlObjectHolderHelper {
  private XmlObject                                       m_parent;
  private Class                                           m_parentType;
  private SchemaType                                      m_parentSchemaType;
  private String                                          m_elementName;
  private String                                          m_fieldName;
  private SchemaProperty                                  m_schemaProperty;
  private SchemaType                                      m_propertySchemaType;
  private String                                          m_defaultStringValue;

  private transient ArrayList<XmlObjectStructureListener> m_listenerList;
  private transient XmlObjectStructureChangeEvent         m_changeEvent;

  private static final Class[]                            NO_PARAMS = new Class[0];
  private static final Object[]                           NO_ARGS   = new Object[0];

  public XmlObject getParent() {
    return m_parent;
  }

  public Class getParentType() {
    return m_parentType;
  }

  public String getElementName() {
    return m_elementName;
  }

  public String getFieldName() {
    return m_fieldName;
  }

  public void init(Class parentType, String elementName) {
    m_parentType = parentType;
    m_elementName = elementName;
    m_fieldName = convertElementName(elementName);

    // this is here because some elementNames ("class") don't cleanly map to
    // their fieldName ("Class1")
    if (Character.isDigit(elementName.charAt(elementName.length() - 1))) {
      m_elementName = elementName.substring(0, elementName.length() - 1);
    }

    m_parentSchemaType = null;
    m_schemaProperty = null;
    m_propertySchemaType = null;
    m_defaultStringValue = null;
  }

  public void setup(XmlObject parent) {
    m_parent = parent;
    if (m_changeEvent != null) {
      m_changeEvent.setXmlObject(parent);
    }
  }

  public void tearDown() {
    m_parent = null;
    if (m_changeEvent != null) {
      m_changeEvent.setXmlObject(null);
    }
  }

  private static String convertElementName(String s) {
    StringBuffer sb = new StringBuffer();
    StringTokenizer st = new StringTokenizer(s, "-");
    String tok;

    while (st.hasMoreTokens()) {
      tok = st.nextToken();
      sb.append(Character.toUpperCase(tok.charAt(0)));
      sb.append(tok.substring(1));
    }

    return sb.toString();
  }

  public XmlObject getXmlObject() {
    if (m_parent != null) {
      try {
        return (XmlObject) invokePrefixedParentNoParams("xget");
      } catch (Exception e) {
        try {
          return (XmlObject) invokePrefixedParentNoParams("get");
        } catch (Exception e2) {
          e2.printStackTrace();
        }
      }
    }

    return null;
  }

  public XmlObject ensureXmlObject() {
    XmlObject xmlObject = null;

    if (m_parent != null && (xmlObject = getXmlObject()) == null) {
      try {
        Class[] params = new Class[] { getPropertySchemaType().getJavaClass() };
        Object[] args = new Object[] { getSchemaProperty().getDefaultValue() };
        String methodName = "xset" + m_fieldName;
        Method method;

        try {
          method = m_parentType.getMethod(methodName, params);
        } catch (NoSuchMethodException nsme) {
          methodName = "set" + m_fieldName;
          method = m_parentType.getMethod(methodName, params);
        }

        method.invoke(m_parent, args);

        xmlObject = getXmlObject();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return xmlObject;
  }

  private SchemaType getParentSchemaType() {
    if (m_parentSchemaType == null) {
      try {
        m_parentSchemaType = (SchemaType) m_parentType.getField("type").get(null);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return m_parentSchemaType;
  }

  private SchemaProperty getSchemaProperty() {
    if (m_schemaProperty == null) {
      QName qname = QName.valueOf(m_elementName);
      SchemaType type = getParentSchemaType();

      if ((m_schemaProperty = type.getElementProperty(qname)) == null) {
        m_schemaProperty = type.getAttributeProperty(qname);
      }
    }

    return m_schemaProperty;
  }

  private SchemaType getPropertySchemaType() {
    if (m_propertySchemaType == null) {
      m_propertySchemaType = getSchemaProperty().getType();
    }
    return m_propertySchemaType;
  }

  // TODO: make ivar
  public boolean hasStringEnumValues() {
    return getPropertySchemaType().hasStringEnumValues();
  }

  // TODO: make lazy ivar
  public StringEnumAbstractBase[] getEnumValues() {
    SchemaStringEnumEntry[] enumEntries = getPropertySchemaType().getStringEnumEntries();
    int size = enumEntries.length;
    StringEnumAbstractBase[] entries = new StringEnumAbstractBase[size];

    for (int i = 0; i < size; i++) {
      entries[i] = enumForInt(enumEntries[i].getIntValue());
    }

    return entries;
  }

  // TODO: make lazy ivar
  public StringEnumAbstractBase defaultEnumValue() {
    return enumForString(defaultStringValue());
  }

  // TODO: make ivar
  public boolean isRequired() {
    return getSchemaProperty().getMinOccurs().intValue() > 0;
  }

  // TODO: make ivar
  public boolean hasDefault() {
    return getSchemaProperty().hasDefault() > 0;
  }

  public boolean isSet() {
    if (m_parent == null) { return false; }

    if (isRequired()) { return true; }

    try {
      return ((Boolean) invokePrefixedParentNoParams("isSet")).booleanValue();
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  public StringEnumAbstractBase getEnumValue() {
    try {
      return enumForString(((XmlToken) getXmlObject()).getStringValue());
    } catch (Exception e) {
      return defaultEnumValue();
    }
  }

  public StringEnumAbstractBase enumForInt(int i) {
    return getPropertySchemaType().enumForInt(i);
  }

  public StringEnumAbstractBase enumForString(String s) {
    return getPropertySchemaType().enumForString(s);
  }

  private XmlAnySimpleType getPropertyFacet(int facet) {
    return getPropertySchemaType().getFacet(facet);
  }

  public Integer minInclusive() {
    XmlInteger min = (XmlInteger) getPropertyFacet(SchemaType.FACET_MIN_INCLUSIVE);
    return Integer.valueOf(min.getBigIntegerValue().intValue());
  }

  public Integer maxInclusive() {
    XmlInteger min = (XmlInteger) getPropertyFacet(SchemaType.FACET_MAX_INCLUSIVE);
    return Integer.valueOf(min.getBigIntegerValue().intValue());
  }

  // TODO: make lazy ivar
  public int defaultInteger() {
    return parseInt(defaultStringValue());
  }

  // TODO: make lazy ivar
  public Integer defaultIntegerValue() {
    return Integer.valueOf(defaultInteger());
  }

  public int getInteger() {
    return parseInt(getStringValue(), defaultInteger());
  }

  public Integer getIntegerValue() {
    return Integer.valueOf(getInteger());
  }

  // TODO: make lazy ivar
  public boolean defaultBoolean() {
    return defaultBooleanValue().booleanValue();
  }

  // TODO: make lazy ivar
  public Boolean defaultBooleanValue() {
    return Boolean.valueOf(defaultStringValue());
  }

  public boolean getBoolean() {
    return getBooleanValue().booleanValue();
  }

  public Boolean getBooleanValue() {
    return Boolean.valueOf(getStringValue());
  }

  public String defaultStringValue() {
    if (m_defaultStringValue == null) {
      m_defaultStringValue = getSchemaProperty().getDefaultText();
    }
    return m_defaultStringValue;
  }

  public String getStringValue() {
    XmlAnySimpleType o = (XmlAnySimpleType) getXmlObject();
    return o != null ? o.getStringValue() : null;
  }

  public void set(String text) {
    if (m_parent == null) { return; }

    try {
      XmlObject xmlObject = ensureXmlObject();
      Class[] params = new Class[] { String.class };
      Object[] args = new Object[] { text };
      String methodName = "setStringValue";
      Class objClass = xmlObject.getClass();
      Method method = objClass.getMethod(methodName, params);

      method.invoke(xmlObject, args);
      fireXmlObjectStructureChanged();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void unset() {
    if (m_parent == null || isRequired() || !isSet()) { return; }

    try {
      invokePrefixedParentNoParams("unset");
      fireXmlObjectStructureChanged();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private Object invokePrefixedParentNoParams(String prefix) throws Exception {
    Method method = m_parentType.getMethod(prefix + m_fieldName, NO_PARAMS);
    return (method != null) ? method.invoke(m_parent, NO_ARGS) : null;
  }

  public synchronized void addXmlObjectStructureListener(XmlObjectStructureListener listener) {
    if (listener != null) {
      if (m_listenerList == null) {
        m_listenerList = new ArrayList<XmlObjectStructureListener>();
      }
      m_listenerList.add(listener);
    }
  }

  public synchronized void removeXmlObjectStructureListener(XmlObjectStructureListener listener) {
    if (listener != null) {
      if (m_listenerList != null) {
        m_listenerList.remove(listener);
      }
    }
  }

  private XmlObjectStructureChangeEvent getChangeEvent() {
    if (m_changeEvent == null) {
      m_changeEvent = new XmlObjectStructureChangeEvent();
      if (m_parent != null) {
        m_changeEvent.setXmlObject(m_parent);
      }
    }

    return m_changeEvent;
  }

  private XmlObjectStructureListener[] getListenerArray() {
    return m_listenerList.toArray(new XmlObjectStructureListener[0]);
  }

  protected synchronized void fireXmlObjectStructureChanged() {
    if (m_listenerList != null) {
      XmlObjectStructureListener[] listeners = getListenerArray();
      XmlObjectStructureChangeEvent event = getChangeEvent();

      for (XmlObjectStructureListener listener : listeners) {
        listener.structureChanged(event);
      }
    }
  }

  protected static int parseInt(String s) {
    return parseInt(s, 42);
  }

  protected static int parseInt(String s, int defaultValue) {
    try {
      return NumberFormat.getIntegerInstance().parse(s).intValue();
    } catch (Exception e) {
      return defaultValue;
    }
  }
}
