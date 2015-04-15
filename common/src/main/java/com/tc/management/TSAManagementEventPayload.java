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
package com.tc.management;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link TCManagementEvent} payload that originates from a server.
 *
 * @author Ludovic Orban
 */
public class TSAManagementEventPayload implements Serializable {

  private String type;
  private final Map<String, Object> attributes = new HashMap<String, Object>();

  public TSAManagementEventPayload() {
  }

  public TSAManagementEventPayload(String type) {
    this.type = type;
  }

  public Map<String, Object> getAttributes() {
    return attributes;
  }

  public String getType() {
    return type;
  }

  public TCManagementEvent toManagementEvent() {
    return new TCManagementEvent(this, getType());
  }

}
