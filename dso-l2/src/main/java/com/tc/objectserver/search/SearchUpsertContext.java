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
package com.tc.objectserver.search;

import com.tc.object.ObjectID;
import com.tc.objectserver.metadata.MetaDataProcessingContext;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.ValueID;

import java.util.List;

/**
 * Context holding search index creation information.
 * 
 * @author Nabib El-Rahman
 */
public class SearchUpsertContext extends BaseSearchEventContext {

  private final List<NVPair> attributes;
  private final String       cacheKey;
  private final ValueID      cacheValue;
  private final boolean      isInsert;

  public SearchUpsertContext(ObjectID segmentOid, String name, String cacheKey, ValueID cacheValue,
                             List<NVPair> attributes, MetaDataProcessingContext metaDataContext, final boolean isInsert) {
    super(segmentOid, name, metaDataContext);
    this.cacheKey = cacheKey;
    this.cacheValue = cacheValue;
    this.attributes = attributes;
    this.isInsert = isInsert;
  }

  /**
   * Key for cache entry.
   */
  public String getCacheKey() {
    return cacheKey;
  }

  /**
   * Value for cache entry
   */
  public ValueID getCacheValue() {
    return cacheValue;
  }

  /**
   * Return List of attributes-value associated with the key.
   */
  public List<NVPair> getAttributes() {
    return attributes;
  }

  public boolean isInsert() {
    return isInsert;
  }

}
