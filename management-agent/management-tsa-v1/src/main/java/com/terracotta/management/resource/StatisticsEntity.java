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
package com.terracotta.management.resource;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link org.terracotta.management.resource.VersionedEntity} representing a topology server
 * or client's statistics from the management API.
 *
 * @author Ludovic Orban
 */
public class StatisticsEntity extends AbstractTsaEntity {

  private String sourceId;

  private Map<String, Object> statistics = new HashMap<String, Object>();

  public String getSourceId() {
    return sourceId;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public Map<String, Object> getStatistics() {
    return statistics;
  }

}
