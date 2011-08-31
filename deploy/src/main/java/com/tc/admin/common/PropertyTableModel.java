/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.table.AbstractTableModel;

/**
 * Displays an object's primitive properties in a PropertySheet-like way.
 */

public class PropertyTableModel extends AbstractTableModel {
  private Class                  type;
  private Object                 instance;
  private String[]               fieldNames;
  private String[]               headings;
  protected Method[]             getters;
  protected Method[]             setters;
  protected Method[]             ops;

  protected static final HashMap primitiveMap = new HashMap();

  private static final String    FIELD_HEADER = "Field";
  private static final String    VALUE_HEADER = "Value";

  public static final int        FIELD_COLUMN = 0;
  public static final int        VALUE_COLUMN = 1;
  public static final int        COLUMN_COUNT = 2;

  static {
    primitiveMap.put(Double.TYPE, Double.class);
    primitiveMap.put(Integer.TYPE, Integer.class);
    primitiveMap.put(Boolean.TYPE, Boolean.class);
    primitiveMap.put(Character.TYPE, Character.class);
    primitiveMap.put(Byte.TYPE, Byte.class);
    primitiveMap.put(Float.TYPE, Float.class);
    primitiveMap.put(Long.TYPE, Long.class);
  }

  public PropertyTableModel() {
    super();
  }

  public PropertyTableModel(Object instance) {
    this(instance, null, null);
  }

  public PropertyTableModel(Object instance, String[] fields) {
    this(instance, fields, null);
  }

  public PropertyTableModel(Class type) {
    this(type, null, null);
  }

  public PropertyTableModel(Class type, String[] fields) {
    this(type, fields, null);
  }

  public PropertyTableModel(Object instance, String[] fields, String[] headings) {
    this(instance.getClass(), fields, headings);
    setInstance(instance);
  }

  public PropertyTableModel(Class type, String[] fields, String[] headers) {
    super();
    init(type, fields, headers);
  }

  public void setInstance(Object instance) {
    this.instance = instance;
    fireTableDataChanged();
  }

  public Object getInstance() {
    return instance;
  }

  public void init(Class theType, String[] fields, String[] theHeadings) {
    this.type = theType;
    fieldNames = determineFields(fields);
    this.headings = determineHeadings(theHeadings);

    setup();
  }

  public void setup() {
    if (type != null) {
      int size = fieldNames.length;

      setters = new Method[size];
      getters = new Method[size];
      ops = new Method[size];

      for (int i = 0; i < fieldNames.length; i++) {
        determineMethods(i, fieldNames[i]);
      }
    }
  }

  private Class _mapPrimitive(Class c) {
    return (Class) primitiveMap.get(c);
  }

  /**
   * If fieldNames == null, determine the set of primitive setters and construct a "fieldName" from the method name:
   * void setMyCoolInteger(int) --> MyCoolInteger
   */
  private String[] determineFields(String theFieldNames[]) {
    if (theFieldNames == null) {
      if (type != null) {
        Method method;
        Method[] methods = type.getMethods();
        ArrayList fieldList = new ArrayList();
        String methodName;
        Class returnType;
        Class[] paramTypes;

        for (int i = 0; i < methods.length; i++) {
          method = methods[i];
          returnType = method.getReturnType();
          paramTypes = method.getParameterTypes();
          methodName = method.getName();

          if (paramTypes.length == 0
              && (methodName.startsWith("get") || methodName.startsWith("is"))
              && (returnType.isPrimitive() || returnType.equals(String.class)
                  || returnType.equals(java.util.Date.class) || hasEditor(returnType))) {
            int j = 0;

            while (!Character.isUpperCase(methodName.charAt(j)))
              j++;
            fieldList.add(methodName.substring(j));
          } else if (paramTypes.length == 0 && returnType == Void.TYPE) {
            fieldList.add(methodName);
          }
        }

        theFieldNames = (String[]) fieldList.toArray(new String[] {});
      }
    }

    return theFieldNames;
  }

  /**
   * Convert a Class field name to a reasonable display form: int myCoolInteger --> My cool integer
   */
  private static String fieldName2Heading(String fieldName) {
    StringBuffer sb = new StringBuffer();
    int len = fieldName.length();
    char c;

    sb.append(Character.toUpperCase(fieldName.charAt(0)));

    for (int i = 1; i < len; i++) {
      c = fieldName.charAt(i);
      if (Character.isUpperCase(c)) {
        sb.append(" ");
        sb.append(Character.toLowerCase(c));
      } else {
        sb.append(c);
      }
    }

    return sb.toString();
  }

  private String[] determineHeadings(String[] theHeadings) {
    if (theHeadings == null) {
      ArrayList headingList = new ArrayList();
      for (int i = 0; i < fieldNames.length; i++) {
        headingList.add(fieldName2Heading(fieldNames[i]));
      }
      theHeadings = (String[]) headingList.toArray(new String[] {});
    }
    return theHeadings;
  }

  private void determineMethods(int index, String name) {
    Method[] methods = type.getMethods();
    Method method;
    String methodName;
    Class returnType;
    Class[] paramTypes;

    for (int i = 0; i < methods.length; i++) {
      method = methods[i];
      returnType = method.getReturnType();
      paramTypes = method.getParameterTypes();
      methodName = method.getName();

      if (("set" + name).equals(methodName)
          && paramTypes.length == 1
          && (paramTypes[0].isPrimitive() || paramTypes[0].equals(String.class)
              || paramTypes[0].equals(java.util.Date.class) || hasEditor(paramTypes[0]))) {
        setters[index] = method;
        break;
      }
    }

    for (int i = 0; i < methods.length; i++) {
      method = methods[i];
      returnType = method.getReturnType();
      paramTypes = method.getParameterTypes();
      methodName = method.getName();

      if ((("get" + name).equals(methodName) || ("is" + name).equals(methodName))
          && paramTypes.length == 0
          && (isPrimitiveOrWrapper(returnType) || returnType.equals(String.class)
              || returnType.equals(java.util.Date.class) || hasEditor(returnType))) {
        getters[index] = method;
        break;
      }
    }

    for (int i = 0; i < methods.length; i++) {
      method = methods[i];
      methodName = method.getName();
      if (name.equals(methodName)) {
        ops[index] = method;
        break;
      }
    }
  }

  private boolean isPrimitiveOrWrapper(Class c) {
    return c.isPrimitive() || primitiveMap.containsValue(c);
  }

  public int getRowCount() {
    return instance != null ? fieldNames.length : 0;
  }

  public int getColumnCount() {
    return COLUMN_COUNT;
  }

  public boolean isCellEditable(int row, int col) {
    return (setters[row] != null) || (ops[row] != null);
  }

  public String getColumnName(int col) {
    switch (col) {
      case FIELD_COLUMN:
        return FIELD_HEADER;
      case VALUE_COLUMN:
        return VALUE_HEADER;
    }
    return "PropertyTableModel: invalid column: " + col;
  }

  public Class getColumnClass(int col) {
    return Object.class;
  }

  public Class getRowClass(int row) {
    Method method = getters[row];
    if (method != null) {
      Class rowClass = method.getReturnType();
      if (rowClass.isPrimitive()) {
        rowClass = _mapPrimitive(rowClass);
      }
      return rowClass;
    }
    return ops[row] != null ? Method.class : Object.class;
  }

  private Object _getFieldValue(int fieldIndex) {
    try {
      return getters[fieldIndex].invoke(instance, new Object[] {});
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public String getFieldHeading(int row) {
    return headings[row] != null ? headings[row] : fieldNames[row];
  }

  public Object getValueAt(int row, int col) {
    switch (col) {
      case FIELD_COLUMN: {
        return getFieldHeading(row);
      }
      case VALUE_COLUMN: {
        if (instance != null) {
          Method method;
          if ((method = ops[row]) != null) {
            return method;
          } else {
            return _getFieldValue(row);
          }
        }
      }
    }

    return "";
  }

  public void setValueAt(Object value, int row, int col) {
    Method setter = setters[col];
    if (setter != null) {
      try {
        setter.invoke(getValueAt(row, col), new Object[] { value });
      } catch (Exception e) {/* ignore */
      }
    }
  }

  public void clear() {
    setInstance(null);
  }

  public boolean hasEditor(Class theType) {
    return false;
  }
}
