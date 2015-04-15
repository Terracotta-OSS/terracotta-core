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
package com.terracotta.toolkit.search;


import org.terracotta.toolkit.ToolkitObjectType;
import org.terracotta.toolkit.search.QueryBuilder;

import com.tc.platform.PlatformService;
import com.terracotta.toolkit.collections.map.ValuesResolver;

public class UnsupportedSearchFactory implements SearchFactory {

  public static final SearchFactory INSTANCE = new UnsupportedSearchFactory();

  private UnsupportedSearchFactory() {
    // private
  }

  @Override
  public SearchExecutor createSearchExecutor(String name,  ToolkitObjectType objectType,ValuesResolver valuesResolver, boolean eventual,
                                             PlatformService platformService) {
    throw new UnsupportedOperationException("Search is supported in enterprise version only");
  }

  @Override
  public QueryBuilder createQueryBuilder(SearchableEntity entity, ToolkitObjectType objectType) {
    throw new UnsupportedOperationException("Search is supported in enterprise version only");
  }

}
