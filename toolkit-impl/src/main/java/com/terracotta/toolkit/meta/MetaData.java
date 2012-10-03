/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.meta;

import com.terracottatech.search.SearchMetaData;

import java.util.Date;
import java.util.Map;

public interface MetaData {

  String getCategory();

  void add(SearchMetaData name, Object value);

  void add(SearchMetaData name, boolean value);

  void add(SearchMetaData name, byte value);

  void add(SearchMetaData name, char value);

  void add(SearchMetaData name, double value);

  void add(SearchMetaData name, float value);

  void add(SearchMetaData name, int value);

  void add(SearchMetaData name, long value);

  void add(SearchMetaData name, short value);

  void add(SearchMetaData name, SearchMetaData value);

  void add(SearchMetaData name, byte[] value);

  void add(SearchMetaData name, Enum value);

  void add(SearchMetaData name, Date value);

  void add(SearchMetaData name, java.sql.Date value);

  void add(String name, Object val);

  void set(SearchMetaData name, Object newValue);

  Map<String, Object> getMetaDatas();

}