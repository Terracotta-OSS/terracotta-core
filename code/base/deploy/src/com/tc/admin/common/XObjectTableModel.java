/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.common;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

/**
 * ObjectTableModel - abstract view onto a collection of Objects of the same type. You tell it the type, the display
 * field names, and the object set. The ordering of the list elements will be determined by the type of object
 * collection used to create the instance. The "field" names are used to determine a getter method. Foo -> getFoo. When
 * used in combintaion with a Table, this facility is meant to provide a high-level data display/editing view with very
 * low cognitive overhead and high usability.
 */

public class XObjectTableModel extends AbstractTableModel {
  private Class                  type;
  private ArrayList              fieldDescriptions;
  private final ArrayList        objects      = new ArrayList();
  private String[]               fieldNames;
  private ArrayList              showingFields;

  protected static final HashMap primitiveMap = new HashMap();

  static {
    primitiveMap.put(Double.TYPE, Double.class);
    primitiveMap.put(Integer.TYPE, Integer.class);
    primitiveMap.put(Boolean.TYPE, Boolean.class);
    primitiveMap.put(Character.TYPE, Character.class);
    primitiveMap.put(Byte.TYPE, Byte.class);
    primitiveMap.put(Float.TYPE, Float.class);
    primitiveMap.put(Long.TYPE, Long.class);
  }

  public static final int        UP           = SwingConstants.NORTH;
  public static final int        DOWN         = SwingConstants.SOUTH;

  public XObjectTableModel() {
    super();
  }

  public XObjectTableModel(Class type, String[] fields, String[] headings) {
    super();
    configure(type, fields, headings);
  }

  public XObjectTableModel(Class type, String[] fields, String[] headings, Object[] data) {
    this(type, fields, headings);

    if (data != null) {
      add(data);
    }
  }

  public XObjectTableModel(Class type, String[] fields, String[] headings, Enumeration enumeration) {
    this(type, fields, headings);

    if (enumeration != null) {
      add(enumeration);
    }
  }

  public XObjectTableModel(Class type, String[] fields, String[] headings, Iterator iter) {
    this(type, fields, headings);

    if (iter != null) {
      add(iter);
    }
  }

  public XObjectTableModel(Class type, String[] fields, String[] headings, Collection c) {
    this(type, fields, headings, c.iterator());
  }

  public void configure(Class theType, String[] fields, String[] headings) {
    this.type = theType;
    fieldNames = Arrays.asList(fields).toArray(new String[0]);
    createColumns(fields, headings);
    fireTableStructureChanged();
  }

  private void determineMethods(FieldDescription fieldDesc) {
    String name = fieldDesc.getFieldName();
    Method[] methods = type.getMethods();
    Method method;
    String methodName;
    Class returnType;
    Class[] paramTypes;

    for (Method method2 : methods) {
      method = method2;
      returnType = method.getReturnType();
      paramTypes = method.getParameterTypes();
      methodName = method.getName();

      if (("set" + name).equals(methodName)
          && paramTypes.length == 1
          && (paramTypes[0].isPrimitive() || paramTypes[0].equals(String.class)
              || paramTypes[0].equals(java.util.Date.class) || hasEditor(paramTypes[0]))) {
        fieldDesc.setSetter(method);
        break;
      }
    }

    for (Method method2 : methods) {
      method = method2;
      returnType = method.getReturnType();
      paramTypes = method.getParameterTypes();
      methodName = method.getName();

      if ((("get" + name).equals(methodName) || ("is" + name).equals(methodName)) && paramTypes.length == 0
          && (canHandle(returnType) || hasEditor(returnType))) {
        fieldDesc.setGetter(method);
        fieldDesc.setSortable(determineSortability(method));
        break;
      }
    }

    for (Method method2 : methods) {
      method = method2;
      methodName = method.getName();

      if (name.equals(methodName)) {
        fieldDesc.setOperation(method);
        break;
      }
    }
  }

  private static boolean canHandle(Class c) {
    try {
      return c.isPrimitive() || c.equals(String.class) || c.equals(java.util.Date.class) || c.getField("TYPE") != null;
    } catch (NoSuchFieldException e) {/**/
    }
    return false;
  }

  private boolean determineSortability(Method getter) {
    if (getter != null) {
      Class returnType = getter.getReturnType();
      return Comparable.class.isAssignableFrom(returnType)
             || (returnType.isPrimitive() && !Void.class.equals(returnType));
    }

    return false;
  }

  public void createColumns(String[] fields, String[] headings) {
    if (type != null) {
      FieldDescription fieldDesc;

      fieldDescriptions = new ArrayList();
      showingFields = new ArrayList();

      for (int i = 0; i < fieldNames.length; i++) {
        fieldDesc = new FieldDescription(fields[i], headings[i]);
        fieldDescriptions.add(fieldDesc);
        showingFields.add(fieldDesc);
        determineMethods(fieldDesc);
      }
    }
  }

  public int getShowingFieldCount() {
    return getColumnCount();
  }

  public String[] getShowingFields() {
    String[] showingFieldNames = new String[getShowingFieldCount()];
    for (int i = 0; i < showingFieldNames.length; i++) {
      showingFieldNames[i] = getShowingFieldDescription(i).getFieldName();
    }
    return showingFieldNames;
  }

  public FieldDescription getFieldDescription(int index) {
    return (FieldDescription) fieldDescriptions.get(index);
  }

  public FieldDescription getFieldDescription(String fieldName) {
    return getFieldDescription(indexOfField(fieldName));
  }

  public FieldDescription getShowingFieldDescription(int index) {
    return (FieldDescription) showingFields.get(index);
  }

  public FieldDescription getShowingFieldDescription(String fieldName) {
    int size = showingFields.size();
    FieldDescription fieldDesc;

    for (int i = 0; i < size; i++) {
      fieldDesc = getShowingFieldDescription(i);

      if (fieldName.equals(fieldDesc.getFieldName())) { return fieldDesc; }
    }

    return null;
  }

  private Class _mapPrimitive(Class c) {
    return (Class) primitiveMap.get(c);
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

  @Override
  public Class getColumnClass(int col) {
    Method method = getShowingFieldGetter(col);

    if (method != null) {
      Class colClass = method.getReturnType();

      if (colClass.isPrimitive()) {
        colClass = _mapPrimitive(colClass);
      }

      return colClass;
    }

    return getShowingFieldOperation(col) != null ? Method.class : Object.class;
  }

  public void clear() {
    objects.clear();
  }

  public void add(Object object) {
    if (type != null) {
      objects.add(object);
    }
  }

  public void add(int index, Object object) {
    if (type != null) {
      objects.add(index, object);
    }
  }

  public void remove(Object object) {
    if (type != null) {
      objects.remove(object);
    }
  }

  public void remove(int index) {
    if (type != null) {
      objects.remove(index);
    }
  }

  public void add(Object[] theObjects) {
    if (theObjects != null) {
      for (Object o : theObjects) {
        add(o);
      }
    }
  }

  public void remove(Object[] theObjects) {
    if (theObjects != null) {
      for (Object o : theObjects) {
        remove(o);
      }
    }
  }

  public void set(Object[] objects) {
    clear();
    add(objects);
    fireTableDataChanged();
  }

  public void add(Enumeration enumeration) {
    if (enumeration != null) {
      while (enumeration.hasMoreElements()) {
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
    if (iter != null) {
      while (iter.hasNext()) {
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
    if (collection != null) {
      add(collection.iterator());
    }
  }

  public void set(Collection collection) {
    clear();
    add(collection);
    fireTableDataChanged();
  }

  public int getRowCount() {
    return objects != null ? objects.size() : 0;
  }

  public int getColumnCount() {
    return showingFields != null ? showingFields.size() : 0;
  }

  @Override
  public boolean isCellEditable(int row, int col) {
    return getShowingFieldSetter(col) != null || getShowingFieldOperation(col) != null;
  }

  @Override
  public String getColumnName(int col) {
    FieldDescription fieldDesc = getShowingFieldDescription(col);
    String heading = fieldDesc.getHeader();
    return heading != null ? heading : fieldDesc.getFieldName();
  }

  public String getFieldName(int col) {
    FieldDescription fieldDesc = getShowingFieldDescription(col);
    return fieldDesc != null ? fieldDesc.getFieldName() : null;
  }

  public Object getObjectAt(int row) {
    return objects.get(row);
  }

  public int getObjectIndex(Object object) {
    int count = getRowCount();
    for (int i = 0; i < count; i++) {
      if (object == getObjectAt(i)) { return i; }
    }
    return -1;
  }

  public Object getValueAt(int row, int col) {
    Method method = getShowingFieldGetter(col);

    if (method != null) {
      try {
        return method.invoke(getObjectAt(row), new Object[] {});
      } catch (Exception e) {
        return e.getMessage();
      }
    }

    if ((method = getShowingFieldOperation(col)) != null) { return method; }

    return "";
  }

  protected Object xgetValueAt(int row, int col) {
    return xgetObjectValueAt(getObjectAt(row), col);
  }

  protected Object xgetObjectValueAt(Object o, int col) {
    Method method = getFieldGetter(col);

    if (method != null) {
      try {
        return method.invoke(o, new Object[] {});
      } catch (Exception e) {
        return e.getMessage();
      }
    }

    if ((method = getFieldOperation(col)) != null) { return method; }

    return "";
  }

  @Override
  public void setValueAt(Object value, int row, int col) {
    Method setter = getShowingFieldSetter(col);

    if (setter != null) {
      try {
        setter.invoke(getObjectAt(row), new Object[] { value });
        fireTableCellUpdated(row, col);
      } catch (Exception e) {/* ignore */
      }
    }
  }

  public void sortColumn(final int col, final int direction) {
    Comparator c = new Comparator<Object>() {
      public int compare(Object o1, Object o2) {
        Comparable prev = (Comparable) xgetObjectValueAt(o1, col);
        Object next = xgetObjectValueAt(o2, col);
        if (prev == null && next == null) {
          return 0;
        } else if (prev == null && next != null) {
          return -1;
        } else if (next == null && prev != null) {
          return 1;
        } else {
          int diff = 0;
          if (prev != null) { // FindBugs: prev can't really be null here
            diff = prev.compareTo(next);
          }
          return (direction == SwingConstants.SOUTH) ? diff : -diff;
        }
      }
    };
    Collections.sort(objects, c);

    fireTableDataChanged();
  }

  public boolean hasEditor(Class theType) {
    return false;
  }

  public int indexOfField(String fieldName) {
    if (fieldName != null) {
      for (int i = 0; i < fieldNames.length; i++) {
        if (fieldName.equals(fieldNames[i])) { return i; }
      }
    }
    return -1;
  }

  protected FieldDescription findDescription(String fieldName) {
    int size = fieldDescriptions.size();
    FieldDescription fieldDesc;
    for (int i = 0; i < size; i++) {
      fieldDesc = getFieldDescription(i);
      if (fieldName.equals(fieldDesc.getFieldName())) { return fieldDesc; }
    }
    return null;
  }

  public void showColumnsExclusive(String[] theFieldNames) {
    FieldDescription fieldDesc;
    showingFields = new ArrayList();
    for (String theFieldName : theFieldNames) {
      if ((fieldDesc = findDescription(theFieldName)) != null) {
        showingFields.add(fieldDesc);
      }
    }
    fireTableStructureChanged();
  }

  public void showColumn(String fieldName) {
    if (isColumnShowing(fieldName)) { return; }

    int showingCount = showingFields.size();
    int fieldIndex = indexOfField(fieldName);
    FieldDescription targetDesc = getFieldDescription(fieldIndex);
    FieldDescription fieldDesc;
    int shownIndex;

    for (int i = 0; i < showingCount; i++) {
      fieldDesc = getShowingFieldDescription(i);
      shownIndex = fieldDesc.indexOfField();

      if (fieldIndex <= shownIndex) {
        showingFields.add(i, targetDesc);
        fireTableStructureChanged();
        return;
      }
    }

    showingFields.add(targetDesc);
    fireTableStructureChanged();
  }

  public void hideColumn(String fieldName) {
    int index = getShowingFieldIndex(fieldName);
    if (index != -1) {
      showingFields.remove(index);
      fireTableStructureChanged();
    }
  }

  public boolean isColumnShowing(String fieldName) {
    int size = showingFields.size();
    FieldDescription fieldDesc;
    for (int i = 0; i < size; i++) {
      fieldDesc = getShowingFieldDescription(i);
      if (fieldName.equals(fieldDesc.getFieldName())) { return true; }
    }
    return false;
  }

  public int getShowingFieldIndex(String fieldName) {
    int size = showingFields.size();
    FieldDescription fieldDesc;
    for (int i = 0; i < size; i++) {
      fieldDesc = getShowingFieldDescription(i);
      if (fieldName.equals(fieldDesc.getFieldName())) { return i; }
    }
    return -1;
  }

  class FieldDescription {
    String  fieldName;
    String  header;
    Method  getter;
    Method  setter;
    Method  op;
    boolean sortable;

    FieldDescription(String fieldName, String header) {
      this.fieldName = fieldName;
      this.header = header;
    }

    String getFieldName() {
      return fieldName;
    }

    int indexOfField() {
      return XObjectTableModel.this.indexOfField(fieldName);
    }

    String getHeader() {
      return header != null ? header : fieldName;
    }

    void setGetter(Method getter) {
      this.getter = getter;
    }

    Method getGetter() {
      return getter;
    }

    void setSetter(Method setter) {
      this.setter = setter;
    }

    Method getSetter() {
      return setter;
    }

    void setOperation(Method op) {
      this.op = op;
    }

    Method getOperation() {
      return op;
    }

    void setSortable(boolean sortable) {
      this.sortable = sortable;
    }

    boolean isSortable() {
      return sortable;
    }
  }
}
