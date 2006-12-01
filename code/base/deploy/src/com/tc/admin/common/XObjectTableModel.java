/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.admin.common;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

/**
 * ObjectTableModel - abstract view onto a collection of Objects of the same
 * type.
 *
 * You tell it the type, the display field names, and the object set. The
 * ordering of the list elements will be determined by the type of object
 * collection used to create the instance.
 * The "field" names are used to determine a getter method.  Foo -> getFoo.
 *
 * When used in combintaion with a Table, this facility is meant to provide
 * a high-level data display/editing view with very low cognitive overhead
 * and high usability.
 */

public class XObjectTableModel extends AbstractTableModel {
  private Class
    m_type;

  private ArrayList
    m_fieldDescriptions;
  
  private ArrayList
    m_objects = new ArrayList();

  private String[]
    m_fieldNames;
  
  private ArrayList
    m_showingFields;

  protected static final HashMap
    m_primitiveMap = new HashMap();

  static {
    m_primitiveMap.put(Double.TYPE,    Double.class);
    m_primitiveMap.put(Integer.TYPE,   Integer.class);
    m_primitiveMap.put(Boolean.TYPE,   Boolean.class);
    m_primitiveMap.put(Character.TYPE, Character.class);
    m_primitiveMap.put(Byte.TYPE,      Byte.class);
    m_primitiveMap.put(Float.TYPE,     Float.class);
    m_primitiveMap.put(Long.TYPE,      Long.class);
  }
    
  public static final int UP   = SwingConstants.NORTH;
  public static final int DOWN = SwingConstants.SOUTH;

  public XObjectTableModel() {
    super();
  }

  public XObjectTableModel(Class type, String[] fields, String[] headings) {
    super();

    m_type       = type;
    m_fieldNames = fields;
    
    createColumns(fields, headings);
  }

  public XObjectTableModel(
    Class    type,
    String[] fields,
    String[] headings,
    Object[] data)
  {
    this(type, fields, headings);

    if(data != null) {
      add(data);
    }
  }

  public XObjectTableModel(
    Class       type,
    String[]    fields,
    String[]    headings,
    Enumeration enumeration)
{
    this(type, fields, headings);

    if(enumeration != null) {
      add(enumeration);
    }
  }

  public XObjectTableModel(
    Class    type,
    String[] fields,
    String[] headings,
    Iterator iter)
  {
    this(type, fields, headings);

    if(iter != null) {
      add(iter);
    }
  }

  public XObjectTableModel(
    Class      type,
    String[]   fields,
    String[]   headings,
    Collection c)
  {
    this(type, fields, headings, c.iterator());
  }

  private void determineMethods(FieldDescription fieldDesc) {
    String   name    = fieldDesc.getFieldName();
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
        fieldDesc.setSetter(method);
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
          (canHandle(returnType) || hasEditor(returnType)))
      {
        fieldDesc.setGetter(method);
        fieldDesc.setSortable(determineSortability(method));
        break;
      }
    }

    for(int i = 0; i < methods.length; i++) {
      method     = methods[i];
      methodName = method.getName();

      if(name.equals(methodName)) {
        fieldDesc.setOperation(method);
        break;
      }
    }
  }

  private static boolean canHandle(Class c) {
    try {
      return c.isPrimitive() ||
             c.equals(String.class) ||
             c.equals(java.util.Date.class) ||
             c.getField("TYPE") != null;
    } catch(NoSuchFieldException e) {/**/}
    return false;
  }
  
  private boolean determineSortability(Method getter) {
    if(getter != null) {
      Class type = getter.getReturnType();

      return Comparable.class.isAssignableFrom(type) ||
        (type.isPrimitive() && !Void.class.equals(type));
    }

    return false;
  }

  public void createColumns(String[] fields, String[] headings) {
    if(m_type != null) {
      FieldDescription fieldDesc;

      m_fieldDescriptions = new ArrayList();
      m_showingFields     = new ArrayList();

      for(int i = 0; i < m_fieldNames.length; i++) {
        fieldDesc = new FieldDescription(fields[i], headings[i]);
        m_fieldDescriptions.add(fieldDesc);
        m_showingFields.add(fieldDesc);
        determineMethods(fieldDesc);
      }
    }
  }

  public int getShowingFieldCount() {
    return getColumnCount();
  }
  
  public String[] getShowingFields() {
    String[] fieldNames = new String[getShowingFieldCount()];

    for(int i = 0; i < fieldNames.length; i++) {
      fieldNames[i] = getShowingFieldDescription(i).getFieldName();
    }
    
    return fieldNames;
  }
  
  public FieldDescription getFieldDescription(int index) {
    return (FieldDescription)m_fieldDescriptions.get(index);
  }
  
  public FieldDescription getFieldDescription(String fieldName) {
    return getFieldDescription(indexOfField(fieldName));
  }
  
  public FieldDescription getShowingFieldDescription(int index) {
    return (FieldDescription)m_showingFields.get(index);
  }
  
  public FieldDescription getShowingFieldDescription(String fieldName) {
    int              size = m_showingFields.size();
    FieldDescription fieldDesc;
    
    for(int i = 0; i < size; i++) {
      fieldDesc = getShowingFieldDescription(i);
      
      if(fieldName.equals(fieldDesc.getFieldName())) {
        return fieldDesc;
      }
    }

    return null;
  }
  
  private Class _mapPrimitive(Class c) {
    return (Class)m_primitiveMap.get(c);
  }

  public boolean isColumnSortable(int col) {
    return getFieldDescription(col).isSortable();
  }

  public Method getFieldGetter(int col) {
    return getFieldDescription(col).getGetter();
  }
  
  public Method getShowingFieldGetter(int col) {
    return getShowingFieldDescription(col).getGetter();
  }

  public Method getFieldSetter(int col) {
    return getFieldDescription(col).getSetter();
  }

  public Method getShowingFieldSetter(int col) {
    return getShowingFieldDescription(col).getSetter();
  }

  public Method getFieldOperation(int col) {
    return getFieldDescription(col).getOperation();
  }

  public Method getShowingFieldOperation(int col) {
    return getShowingFieldDescription(col).getOperation();
  }

  public Class getColumnClass(int col) {
    Method method = getShowingFieldGetter(col);

    if(method != null) {
      Class colClass = method.getReturnType();

      if(colClass.isPrimitive()) {
        colClass = _mapPrimitive(colClass);
      }  

      return colClass;
    }

    if((method = getShowingFieldOperation(col)) != null) {
      return Method.class;
    }

    return Object.class;
  }

  public void clear() {
    m_objects.clear();
  }

  public void add(Object object) {
    if(m_type != null) {
      m_objects.add(object);
    }
  }

  public void add(int index, Object object) {
    if(m_type != null) {
      m_objects.add(index, object);
    }
  }

  public void remove(Object object) {
    if(m_type != null) {
      m_objects.remove(object);
    }
  }

  public void remove(int index) {
    if(m_type != null) {
      m_objects.remove(index);
    }
  }

  public void add(Object[] objects) {
    if(objects != null) {
      for(int i = 0; i < objects.length; i++) {
        add(objects[i]);
      }
    }
  }

  public void remove(Object[] objects) {
    if(objects != null) {
      for(int i = 0; i < objects.length; i++) {
        remove(objects[i]);
      }
    }
  }

  public void set(Object[] objects) {
    clear();
    add(objects);
    fireTableDataChanged();
  }

  public void add(Enumeration enumeration) {
    if(enumeration != null) {
      while(enumeration.hasMoreElements()) {
        add(enumeration.nextElement());
      }
    }
  }

  public void set(Enumeration enumeration) {
    clear();
    add(enumeration);
    fireTableDataChanged();
  }

  public void add(Iterator iter) {
    if(iter != null) {
      while(iter.hasNext()) {
        add(iter.next());
      }
    }
  }

  public void set(Iterator iter) {
    clear();
    add(iter);
    fireTableDataChanged();
  }

  public void add(Collection collection) {
    if(collection != null) {
      add(collection.iterator());
    }
  }

  public void set(Collection collection) {
    clear();
    add(collection);
    fireTableDataChanged();
  }

  public int getRowCount() {
    return m_objects != null ? m_objects.size() : 0;
  }

  public int getColumnCount() {
    return m_showingFields != null ? m_showingFields.size() : 0;
  }

  public boolean isCellEditable(int row, int col) {
    return getShowingFieldSetter(col)    != null ||
           getShowingFieldOperation(col) != null;
  }

  public String getColumnName(int col) {
    FieldDescription fieldDesc = getShowingFieldDescription(col);
    String           heading   = fieldDesc.getHeader();
    
    return heading != null ? heading : fieldDesc.getFieldName();
  }

  public String getFieldName(int col) {
    FieldDescription fieldDesc = getShowingFieldDescription(col);
    return fieldDesc != null ? fieldDesc.getFieldName() : null;
  }

  public Object getObjectAt(int row) {
    return m_objects.get(row);
  }

  public int getObjectIndex(Object object) {
    int count = getRowCount();

    for(int i = 0; i < count; i++) {
      if(object == getObjectAt(i)) {
        return i;
      }
    }

    return -1;
  }

  public Object getValueAt(int row, int col) {
    Method method = getShowingFieldGetter(col);

    if(method != null) {
      try {
        return method.invoke(getObjectAt(row), new Object[] {});
      }
      catch(Exception e) {
        return e.getMessage();
      }
    }

    if((method = getShowingFieldOperation(col)) != null) {
      return method;
    }

    return "";
  }

  protected Object xgetValueAt(int row, int col) {
    Method method = getFieldGetter(col);

    if(method != null) {
      try {
        return method.invoke(getObjectAt(row), new Object[] {});
      }
      catch(Exception e) {
        return e.getMessage();
      }
    }

    if((method = getFieldOperation(col)) != null) {
      return method;
    }

    return "";
  }

  public void setValueAt(Object value, int row, int col) {
    Method setter = getShowingFieldSetter(col);

    if(setter != null) {
      try {
        setter.invoke(getObjectAt(row), new Object[] {value});
        fireTableCellUpdated(row, col);
      }
      catch(Exception e) {/*ignore*/}
    }
  }

  private boolean compareAdjacentRows(int direction, int row, int col) {
    Comparable prev = (Comparable)xgetValueAt(row-1, col);
    Object     next = xgetValueAt(row, col);
    int        diff = prev.compareTo(next);
    
    return (direction == DOWN) ? (diff > 0) : (diff < 0);
  }

  public void sortColumn(int col, int direction) {
    int count = getRowCount();

    for(int i = 0; i < count; i++) {
      for(int j = i; j > 0 && compareAdjacentRows(direction, j, col); j--) {
        Object tmp = getObjectAt(j);

        m_objects.set(j, getObjectAt(j-1));
        m_objects.set(j-1, tmp);
      }
    }

    fireTableDataChanged();
  }

  public boolean hasEditor(Class type) {
    return false;
  }

  public int indexOfField(String fieldName) {
    if(fieldName != null) {
      for(int i = 0; i < m_fieldNames.length; i++) {
        if(fieldName.equals(m_fieldNames[i])) {
          return i;
        }
      }
    }

    return -1;
  }

  protected FieldDescription findDescription(String fieldName) {
    int              size = m_fieldDescriptions.size();
    FieldDescription fieldDesc;
    
    for(int i = 0; i < size; i++) {
      fieldDesc = getFieldDescription(i);
      if(fieldName.equals(fieldDesc.getFieldName())) {
        return fieldDesc;
      }
    }
    
    return null;
  }
  
  public void showColumnsExclusive(String[] fieldNames) {
    FieldDescription fieldDesc;
    
    m_showingFields = new ArrayList();
    for(int i = 0; i < fieldNames.length; i++) {
      if((fieldDesc = findDescription(fieldNames[i])) != null) {
        m_showingFields.add(fieldDesc);
      }
    }

    fireTableStructureChanged();
  }
  
  public void showColumn(String fieldName) {
    if(isColumnShowing(fieldName)) {
      return;
    }
    
    int              showingCount = m_showingFields.size();
    int              fieldIndex   = indexOfField(fieldName);
    FieldDescription targetDesc   = getFieldDescription(fieldIndex);
    FieldDescription fieldDesc;
    int              shownIndex;
   
    for(int i = 0; i < showingCount; i++) {
      fieldDesc  = getShowingFieldDescription(i);
      shownIndex = fieldDesc.indexOfField();
      
      if(fieldIndex <= shownIndex) {
        m_showingFields.add(i, targetDesc);
        fireTableStructureChanged();
        return;
      }
    }

    m_showingFields.add(targetDesc);
    fireTableStructureChanged();
  }
  
  public void hideColumn(String fieldName) {
    int index = getShowingFieldIndex(fieldName);

    if(index != -1) {
      m_showingFields.remove(index);
      fireTableStructureChanged();
    }
  }
  
  public boolean isColumnShowing(String fieldName) {
    int              size = m_showingFields.size();
    FieldDescription fieldDesc;
    
    for(int i = 0; i < size; i++) {
      fieldDesc = getShowingFieldDescription(i);
      
      if(fieldName.equals(fieldDesc.getFieldName())) {
        return true;
      }
    }

    return false;
  }
  
  public int getShowingFieldIndex(String fieldName) {
    int              size = m_showingFields.size();
    FieldDescription fieldDesc;
    
    for(int i = 0; i < size; i++) {
      fieldDesc = getShowingFieldDescription(i);
      
      if(fieldName.equals(fieldDesc.getFieldName())) {
        return i;
      }
    }

    return -1;
  }
  
  class FieldDescription {
    String  m_fieldName;
    String  m_header;
    Method  m_getter;
    Method  m_setter;
    Method  m_op;
    boolean m_sortable;
   
    FieldDescription(String fieldName, String header) {
      m_fieldName = fieldName;
      m_header    = header;
    }

    String getFieldName() {
      return m_fieldName;
    }
    
    int indexOfField() {
      return XObjectTableModel.this.indexOfField(m_fieldName);
    }
    
    String getHeader() {
      return m_header != null ? m_header : m_fieldName;
    }

    void setGetter(Method getter) {
      m_getter = getter;
    }
    
    Method getGetter() {
      return m_getter;
    }
    
    void setSetter(Method setter) {
      m_setter = setter;
    }
    
    Method getSetter() {
      return m_setter;
    }
    
    void setOperation(Method op) {
      m_op = op;
    }
    
    Method getOperation() {
      return m_op;
    }
    
    void setSortable(boolean sortable) {
      m_sortable = sortable;
    }
    
    boolean isSortable() {
      return m_sortable;
    }
  }
}
