/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.toolkit.meta;


import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.platform.PlatformService;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.SearchMetaData;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MetaDataImpl implements MetaData {

  private final MetaDataDescriptor mdd;

  public MetaDataImpl(PlatformService platformService, String category) {
    this.mdd = platformService.createMetaDataDescriptor(category);
  }

  @Override
  public String getCategory() {
    return mdd.getCategory();
  }

  @Override
  public void add(SearchMetaData name, Object value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, boolean value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, byte value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, char value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, double value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, float value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, int value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, long value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, short value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, SearchMetaData value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, byte[] value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, Enum value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, Date value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void add(SearchMetaData name, java.sql.Date value) {
    mdd.add(name.toString(), value);
  }

  @Override
  public void set(SearchMetaData name, Object newValue) {
    mdd.set(name.toString(), newValue);
  }


  MetaDataDescriptor getInternalMetaDataDescriptor() {
    return mdd;
  }

  @Override
  public Map<String, Object> getMetaDatas() {
    Map<String, Object> rv = new HashMap<String, Object>();
    Iterator<NVPair> metaDatas = mdd.getMetaDatas();
    while (metaDatas.hasNext()) {
      NVPair next = metaDatas.next();
      rv.put(next.getName(), next.getObjectValue());
    }
    return rv;
  }

  @Override
  public void add(String name, Object val) {
    mdd.add(name, val);
  }

}
