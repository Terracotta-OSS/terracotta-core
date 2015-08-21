/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.opentypes.adapters;

import java.util.ArrayList;
import java.util.List;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

public class ClassCreationCount implements Comparable<ClassCreationCount> {

  private static final String        COMPOSITE_TYPE_NAME        = "ObjectCreationCountPerClass";
  private static final String        COMPOSITE_TYPE_DESCRIPTION = "Number of objects created per class";
  private static final String[]      ITEM_NAMES                 = new String[] { "className", "objectCreationCount" };
  private static final String[]      ITEM_DESCRIPTIONS          = new String[] { "className", "objectCreationCount" };
  private static final OpenType<?>[] ITEM_TYPES                 = new OpenType[] { SimpleType.STRING, SimpleType.INTEGER                                       };
  private static final CompositeType COMPOSITE_TYPE;
  private static final String        TABULAR_TYPE_NAME          = "ObjectCreationCountByClass";
  private static final String        TABULAR_TYPE_DESCRIPTION   = "Object creation count by class";
  private static final String[]      INDEX_NAMES                = new String[] { "className" };
  private static final TabularType   TABULAR_TYPE;

  static {
    try {
      COMPOSITE_TYPE = new CompositeType(COMPOSITE_TYPE_NAME, COMPOSITE_TYPE_DESCRIPTION, ITEM_NAMES,
                                         ITEM_DESCRIPTIONS, ITEM_TYPES);
      TABULAR_TYPE = new TabularType(TABULAR_TYPE_NAME, TABULAR_TYPE_DESCRIPTION, COMPOSITE_TYPE, INDEX_NAMES);
    } catch (OpenDataException e) {
      throw new RuntimeException(e);
    }
  }

  private final String               className;
  private final Integer              count;

  public ClassCreationCount(String className, Integer count) {
    this.className = className;
    this.count = count;
  }

  public String getClassName() {
    return className;
  }
  
  public Integer getCount() {
    return count;
  }
  
  public ClassCreationCount(CompositeData cData) {
    className = (String) cData.get(ITEM_NAMES[0]);
    count = (Integer) cData.get(ITEM_NAMES[1]);
  }

  public CompositeData toCompositeData() {
    try {
      return new CompositeDataSupport(COMPOSITE_TYPE, ITEM_NAMES, new Object[] { className, count });
    } catch (OpenDataException e) {
      throw new RuntimeException(e);
    }
  }

  public static TabularData newTabularDataInstance() {
    return new TabularDataSupport(TABULAR_TYPE);
  }

  public static ClassCreationCount[] fromTabularData(TabularData tabularData) {
    final List<ClassCreationCount> countList = new ArrayList<>(tabularData.size());
    for (Object data : tabularData.values()) {
      countList.add(new ClassCreationCount((CompositeData) data));
    }
    final ClassCreationCount[] counts = new ClassCreationCount[countList.size()];
    countList.toArray(counts);
    return counts;
  }

  @Override
  public int compareTo(ClassCreationCount other) {
    int result = count.compareTo(other.count);
    return result != 0 ? result : className.compareTo(other.className);
  }
}
