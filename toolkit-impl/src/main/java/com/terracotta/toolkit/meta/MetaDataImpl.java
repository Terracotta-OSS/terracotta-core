/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.meta;

import org.terracotta.toolkit.internal.meta.MetaData;

import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.metadata.MetaDataDescriptor;
import com.terracottatech.search.NVPair;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MetaDataImpl implements MetaData {

  private final MetaDataDescriptor mdd;

  public MetaDataImpl(String category) {
    this.mdd = ManagerUtil.createMetaDataDescriptor(category);
  }

  @Override
  public String getCategory() {
    return mdd.getCategory();
  }

  @Override
  public void add(String name, Object value) {
    mdd.add(name, value);
  }

  public void add(String name, boolean value) {
    mdd.add(name, value);
  }

  public void add(String name, byte value) {
    mdd.add(name, value);
  }

  public void add(String name, char value) {
    mdd.add(name, value);
  }

  public void add(String name, double value) {
    mdd.add(name, value);
  }

  public void add(String name, float value) {
    mdd.add(name, value);
  }

  public void add(String name, int value) {
    mdd.add(name, value);
  }

  public void add(String name, long value) {
    mdd.add(name, value);
  }

  public void add(String name, short value) {
    mdd.add(name, value);
  }

  public void add(String name, String value) {
    mdd.add(name, value);
  }

  public void add(String name, byte[] value) {
    mdd.add(name, value);
  }

  public void add(String name, Enum value) {
    mdd.add(name, value);
  }

  public void add(String name, Date value) {
    mdd.add(name, value);
  }

  public void add(String name, java.sql.Date value) {
    mdd.add(name, value);
  }

  public void set(String name, Object newValue) {
    mdd.set(name, newValue);
  }

  MetaDataDescriptor getInternalMetaDataDescriptor() {
    return mdd;
  }

  public Map<String, Object> getMetaDatas() {
    Map<String, Object> rv = new HashMap<String, Object>();
    Iterator<NVPair> metaDatas = mdd.getMetaDatas();
    while (metaDatas.hasNext()) {
      NVPair next = metaDatas.next();
      rv.put(next.getName(), next.getObjectValue());
    }
    return rv;
  }

}
