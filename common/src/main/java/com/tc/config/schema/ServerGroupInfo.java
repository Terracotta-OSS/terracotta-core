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
package com.tc.config.schema;

import java.io.Serializable;

public class ServerGroupInfo implements Serializable {
  private final L2Info[] members;
  private final String   name;
  private final int      id;
  private final boolean  isCoordinator;

  public ServerGroupInfo(L2Info[] members, String name, int id, boolean isCoordinator) {
    this.members = members;
    this.name = name;
    this.id = id;
    this.isCoordinator = isCoordinator;
  }

  public L2Info[] members() {
    return members;
  }

  public String name() {
    return name;
  }

  public int id() {
    return id;
  }

  public boolean isCoordinator() {
    return isCoordinator;
  }
}
