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

public class SearchPutIfAbsentContext extends BaseSearchEventContext {
  private final List<NVPair> attributes;
  private final String       cacheKey;
  private final ValueID      cacheValue;

  public SearchPutIfAbsentContext(ObjectID segmentOid, String name, String cacheKey, ValueID cacheValue,
                                  List<NVPair> attributes, MetaDataProcessingContext metaDataContext) {
    super(segmentOid, name, metaDataContext);
    this.cacheKey = cacheKey;
    this.cacheValue = cacheValue;
    this.attributes = attributes;
  }

  public List<NVPair> getAttributes() {
    return attributes;
  }

  public String getCacheKey() {
    return cacheKey;
  }

  public ValueID getCacheValue() {
    return cacheValue;
  }

}
