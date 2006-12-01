/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
  private Class
    m_type;
  
  private Object
    m_instance;

  private String[]
    m_fieldNames;

  private String[]
    m_headings;

  protected Method
    m_getters[];

  protected Method
    m_setters[];

  protected Method
    m_ops[];

  protected static final HashMap
    m_primitiveMap = new HashMap();

  private static final String FIELD_HEADER = "Field";
  private static final String VALUE_HEADER = "Value";

  public static final int FIELD_COLUMN = 0;
  public static final int VALUE_COLUMN = 1;
  public static final int COLUMN_COUNT = 2;

  static {
    m_primitiveMap.put(Double.TYPE,    Double.class);
    m_primitiveMap.put(Integer.TYPE,   Integer.class);
    m_primitiveMap.put(Boolean.TYPE,   Boolean.class);
    m_primitiveMap.put(Character.TYPE, Character.class);
    m_primitiveMap.put(Byte.TYPE,      Byte.class);
    m_primitiveMap.put(Float.TYPE,     Float.class);
    m_primitiveMap.put(Long.TYPE,      Long.class);
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

  public PropertyTableModel(
    Object   instance,
    String[] fields,
    String[] headings)
  {
    this(instance.getClass(), fields, headings);
    setInstance(instance);
  }

  public PropertyTableModel(
    Class    type,
    String[] fields,
    String[] headers)
  {
    super();
    init(type, fields, headers);
  }

  public void setInstance(Object instance) {
    m_instance = instance;
    fireTableDataChanged();
  }

  public Object getInstance() {
    return m_instance;
  }
  
  public void init(
    Class    type,
    String[] fields,
    String[] headings)
  {
    m_type       = type;
    m_fieldNames = determineFields(fields);
    m_headings   = determineHeadings(headings);

    setup();
  }
  
  public void setup() {
    if(m_type != null) {
      int size = m_fieldNames.length;

      m_setters  = new Method[size];
      m_getters  = new Method[size];
      m_ops      = new Method[size];

      for(int i = 0; i < m_fieldNames.length; i++) {
        determineMethods(i, m_fieldNames[i]);
      }
    }
  }

  private Class _mapPrimitive(Class c) {
    return (Class)m_primitiveMap.get(c);
  }

  /**
   * If fieldNames == null, determine the set of primitive setters
   * and construct a "fieldName" from the method name:
   * void setMyCoolInteger(int) --> MyCoolInteger
   */
  private String[] determineFields(String fieldNames[]) {
    if(fieldNames == null) {
      if(m_type != null) {
        Method    method;
        Method[]  methods   = m_type.getMethods();
        ArrayList fieldList = new ArrayList();
        String    methodName;
        Class     returnType;
        Class[]   paramTypes;
        
        for(int i = 0; i < methods.length; i++) {
          method     = methods[i];
          returnType = method.getReturnType();
          paramTypes = method.getParameterTypes();
          methodName = method.getName();

          if(paramTypes.length == 0 && 
             (methodName.startsWith("get") || methodName.startsWith("is")) &&
             (returnType.isPrimitive() ||
              returnType.equals(String.class) ||
              returnType.equals(java.util.Date.class) ||
              hasEditor(returnType)))
          {
            int j = 0;
              
            while(!Character.isUpperCase(methodName.charAt(j))) j++;
            fieldList.add(methodName.substring(j));
          }
          else if(paramTypes.length == 0 && returnType == Void.TYPE) {
            fieldList.add(methodName);
          }
        }
        
        fieldNames = (String[])fieldList.toArray(new String[]{});
      }
    }
    
    return fieldNames;
  }
  
  /**
   * Convert a Class field name to a reasonable display form:
   * int myCoolInteger --> My cool integer
   */
  private static String fieldName2Heading(String fieldName) {
    StringBuffer sb  = new StringBuffer();
    int          len = fieldName.length();
    char         c;
    
    sb.append(Character.toUpperCase(fieldName.charAt(0)));
    
    for(int i = 1; i < len; i++) {
      c = fieldName.charAt(i);
      
      if(Character.isUpperCase(c)) {
        sb.append(" ");
        sb.append(Character.toLowerCase(c));
      }
      else {
        sb.append(c);
      }
    }

    return sb.toString();
  }
  
  private String[] determineHeadings(String headings[]) {
    if(headings == null) {
      ArrayList headingList = new ArrayList();
      
      for(int i = 0; i < m_fieldNames.length; i++) {
        headingList.add(fieldName2Heading(m_fieldNames[i]));
      }

      headings = (String[])headingList.toArray(new String[]{});
    }
    
    return headings;
  }
    
  private void determineMethods(int index, String name) {
    Method[] methods = m_type.getMethods();
    Method   method;
    String   methodName;
    Class    returnType;
    Class[]  paramTypes;

    for(int i = 0; i < methods.length; i++) {
      method     = methods[i];
      returnType = method.getReturnType();
      paramTypes = method.getParameterTypes();
      methodName = method.getName();

      if(("set"+name).equals(methodName) &&
         paramTypes.length == 1  && 
         (paramTypes[0].isPrimitive() ||
          paramTypes[0].equals(String.class) ||
          paramTypes[0].equals(java.util.Date.class) ||
          hasEditor(paramTypes[0])))
      {
        m_setters[index] = method;
        break;
      }
    }

    for(int i = 0; i < methods.length; i++) {
      method     = methods[i];
      returnType = method.getReturnType();
      paramTypes = method.getParameterTypes();
      methodName = method.getName();

      if((("get"+name).equals(methodName) ||
          ("is"+name).equals(methodName)) &&
          paramTypes.length == 0 && 
          (returnType.isPrimitive() ||
           returnType.equals(String.class) ||
           returnType.equals(java.util.Date.class) ||
           hasEditor(returnType)))
      {
        m_getters[index] = method;
        break;
      }
    }

    for(int i = 0; i < methods.length; i++) {
      method     = methods[i];
      methodName = method.getName();

      if(name.equals(methodName)) {
        m_ops[index] = method;
        break;
      }
    }
  }

  public int getRowCount() {
    return m_instance != null ? m_fieldNames.length : 0;
  }

  public int getColumnCount() {
    return COLUMN_COUNT;
  }

  public boolean isCellEditable(int row, int col) {
    return (m_setters[row] != null) || (m_ops[row] != null);
  }

  public String getColumnName(int col) {
    switch(col) {
      case FIELD_COLUMN: return FIELD_HEADER;
      case VALUE_COLUMN: return VALUE_HEADER;
    }

    return "PropertyTableModel: invalid column: "+col;
  }

  public Class getColumnClass(int col) {
    return Object.class;
  }

  public Class getRowClass(int row) {
    Method method = m_getters[row];

    if(method != null) {
      Class rowClass = method.getReturnType();

      if(rowClass.isPrimitive()) {
        rowClass = _mapPrimitive(rowClass);
      }  

      return rowClass;
    }

    if((method = m_ops[row]) != null) {
      return Method.class;
    }

    return Object.class;
  }
  
  private Object _getFieldValue(int fieldIndex) {
    try {
      return m_getters[fieldIndex].invoke(m_instance, new Object[] {});
    }
    catch(Exception e) {
      return e.getMessage();
    }
  }

  public String getFieldHeading(int row) {
    return m_headings[row] != null ?
           m_headings[row] : m_fieldNames[row];
  }
  
  public Object getValueAt(int row, int col) {
    switch(col) {
      case FIELD_COLUMN: {
        return getFieldHeading(row);
      }
      case VALUE_COLUMN: {
        if(m_instance != null) {
          Method method;
          
          if((method = m_ops[row]) != null) {
            return method;
          }
          else {
            return _getFieldValue(row);
          }
        }
      }
    }

    return "";
  }

  public void setValueAt(Object value, int row, int col) {
    Method setter = m_setters[col];

    if(setter != null) {
      try {
        setter.invoke(getValueAt(row, col), new Object[] {value});
      }
      catch(Exception e) {/*ignore*/}
    }
  }

  public void clear() {
    setInstance(null);
  }

  public boolean hasEditor(Class type) {
    return false;
  }
}
