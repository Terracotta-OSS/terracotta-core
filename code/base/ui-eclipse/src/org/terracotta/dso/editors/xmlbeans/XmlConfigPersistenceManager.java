/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.dso.editors.xmlbeans;

import org.apache.xmlbeans.SchemaProperty;
import org.apache.xmlbeans.SchemaStringEnumEntry;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.StringEnumAbstractBase;
import org.apache.xmlbeans.XmlAnySimpleType;
import org.apache.xmlbeans.XmlObject;

import java.lang.reflect.Method;
import java.util.StringTokenizer;

import javax.xml.namespace.QName;

// TODO: Perhaps an error dialog should be displayed instead of failing silently
final class XmlConfigPersistenceManager {

  private static final Class[]  NO_PARAMS  = new Class[0];
  private static final Object[] NO_ARGS    = new Object[0];
  private static final String   XSET       = "xset";
  private static final String   SET        = "set";
  private static final String   XGET       = "xget";
  private static final String   GET        = "get";
  private static final String   TYPE       = "type";
  private static final String   ADD_NEW    = "addNew";
  private static final String   FOR_STRING = "forString";
  private static final String   ENUM_VALUE = "enumValue";

  static String readElement(XmlObject parent, String elementName) {
    try {
      Class parentType = parent.schemaType().getJavaClass();
      XmlAnySimpleType element = ((XmlAnySimpleType) getXmlObject(parent, parentType, convertElementName(elementName)));
      if (element != null) return element.getStringValue();
      return getSchemaProperty(parentType, elementName).getDefaultText();
    } catch (Exception e) {
      e.printStackTrace(); // XXX
      return "";
    }
  }

  static void writeElement(XmlObject parent, String elementName, String value) {
    Class parentType = parent.schemaType().getJavaClass();
    Class[] params = new Class[] { String.class };
    Object[] args = new Object[] { value };
    Method method = null;
    // STRING
    try {
      method = parentType.getMethod(SET + convertElementName(elementName), params);
      method.invoke(parent, args);
    } catch (Exception e) { /* skip */
    }
    // BOOLEAN
    if (method == null) {
      try {
        params[0] = Boolean.TYPE;
        method = parentType.getMethod(SET + convertElementName(elementName), params);
        args[0] = Boolean.parseBoolean(value);
        method.invoke(parent, args);
      } catch (Exception e) { /* skip */
      }
    }
    // INTEGER
    if (method == null) {
      try {
        params[0] = Integer.TYPE;
        method = parentType.getMethod(SET + convertElementName(elementName), params);
        args[0] = Integer.parseInt(value);
        method.invoke(parent, args);
      } catch (Exception e) { /* skip */
      }
    }
    // ENUM
    if (method == null) {
      try {
        params[0] = String.class;
        args[0] = value;
        XmlObject xmlObject = ensureElementHierarchy(parent, parentType, elementName, convertElementName(elementName),
            false);
        StringEnumAbstractBase enumValue = (StringEnumAbstractBase) xmlObject.getClass().getMethod(ENUM_VALUE,
            new Class[0]).invoke(xmlObject, new Object[0]);
        method = enumValue.getClass().getMethod(FOR_STRING, params);
        Object enumElement = method.invoke(enumValue, args);
        xmlObject.getClass().getMethod("set", new Class[] { StringEnumAbstractBase.class }).invoke(xmlObject,
            new Object[] { enumElement });
      } catch (Exception e) { /* skip */
      }
    }
    if (method == null) {
      System.err.println("Unable to save XML value for: " + parent + " " + elementName + " " + value);// XXX
    }
  }

  static String[] getListDefaults(Class parentType, String elementName) {
    try {
      SchemaStringEnumEntry[] enumEntries = getPropertySchemaType(parentType, elementName).getStringEnumEntries();
      String[] values = new String[enumEntries.length];
      for (int i = 0; i < enumEntries.length; i++) {
        values[i] = enumEntries[i].getString();
      }
      return values;
    } catch (Exception e) {
      e.printStackTrace(); // XXX
      return new String[0];
    }
  }

  static XmlObject ensureXml(XmlObject parent, Class parentType, String elementName) {
    try {
      return ensureElementHierarchy(parent, parentType, elementName, convertElementName(elementName), true);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static XmlObject ensureElementHierarchy(XmlObject parent, Class parentType, String elementName,
                                                  String fieldName, boolean addNew) throws Exception {
    XmlObject xmlObject = getXmlObject(parent, parentType, fieldName);
    if (xmlObject != null) return xmlObject;
    Class[] params = new Class[] { getPropertySchemaType(parentType, elementName).getJavaClass() };
    Object[] args = new Object[] { getSchemaProperty(parentType, elementName).getDefaultValue() };
    Method method = null;

    // TODO: move try catch for add new
    // TODO: handle [] additions correctly
    try {
      if (addNew && args.length == 1 && args[0] == null) {
        method = parentType.getMethod(ADD_NEW + fieldName, new Class[0]);
        args = new Object[0];
      } else {
        method = parentType.getMethod(XSET + fieldName, params);
      }
    } catch (NoSuchMethodException e) {
      try {
        method = parentType.getMethod(SET + fieldName, params);
      } catch (NoSuchMethodException ee) {
        method = parentType.getMethod(XSET + fieldName + "1", params);
      }
    }
    method.invoke(parent, args);
    return getXmlObject(parent, parentType, fieldName);
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
    if (sb.toString().equals("Class")) return sb.toString() + "1";
    return sb.toString();
  }

  private static XmlObject getXmlObject(XmlObject parent, Class parentType, String fieldName) throws Exception {
    return (XmlObject) invokePrefixedParentNoParams(XGET, parent, parentType, fieldName);
  }

  private static Object invokePrefixedParentNoParams(String prefix, XmlObject parent, Class parentType, String fieldName)
      throws Exception {
    Method method = null;
    try {
      method = parentType.getMethod(prefix + fieldName, NO_PARAMS);
    } catch (NoSuchMethodException e) {
      try {
        method = parentType.getMethod(GET + fieldName, NO_PARAMS);
      } catch (NoSuchMethodException ee) {
        method = parentType.getMethod(prefix + fieldName + "1", NO_PARAMS);
      }
    }
    return (method != null) ? method.invoke(parent, NO_ARGS) : null;
  }

  private static SchemaType getParentSchemaType(Class parentType) throws Exception {
    return (SchemaType) parentType.getField(TYPE).get(null);
  }

  private static SchemaProperty getSchemaProperty(Class parentType, String elementName) throws Exception {
    QName qname = QName.valueOf(elementName);
    SchemaType type = getParentSchemaType(parentType);
    SchemaProperty property = type.getElementProperty(qname);
    if (property == null) property = type.getAttributeProperty(qname);
    return property;
  }

  private static SchemaType getPropertySchemaType(Class parentType, String elementName) throws Exception {
    return getSchemaProperty(parentType, elementName).getType();
  }
}
