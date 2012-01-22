/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.opentypes.adapters;

import java.util.ArrayList;
import java.util.Iterator;
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

public class ClassCreationCount implements Comparable {

  private static final String        COMPOSITE_TYPE_NAME        = "ObjectCreationCountPerClass";
  private static final String        COMPOSITE_TYPE_DESCRIPTION = "Number of objects created per class";
  private static final String[]      ITEM_NAMES                 = new String[] { "className", "objectCreationCount" };
  private static final String[]      ITEM_DESCRIPTIONS          = new String[] { "className", "objectCreationCount" };
  private static final OpenType[]    ITEM_TYPES                 = new OpenType[] { SimpleType.STRING,
      SimpleType.INTEGER                                       };
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

  public ClassCreationCount(final String className, final Integer count) {
    this.className = className;
    this.count = count;
  }

  public String getClassName() {
    return className;
  }
  
  public Integer getCount() {
    return count;
  }
  
  public ClassCreationCount(final CompositeData cData) {
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

  public static ClassCreationCount[] fromTabularData(final TabularData tabularData) {
    final List countList = new ArrayList(tabularData.size());
    for (final Iterator pos = tabularData.values().iterator(); pos.hasNext();) {
      countList.add(new ClassCreationCount((CompositeData) pos.next()));
    }
    final ClassCreationCount[] counts = new ClassCreationCount[countList.size()];
    countList.toArray(counts);
    return counts;
  }

  public int compareTo(final Object theOtherGuy) {
    ClassCreationCount rhs = (ClassCreationCount) theOtherGuy;
    int result = count.compareTo(rhs.count);
    return result != 0 ? result : className.compareTo(rhs.className);
  }
}
