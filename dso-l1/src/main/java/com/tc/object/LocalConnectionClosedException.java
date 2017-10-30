/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.object;

import org.terracotta.exception.ConnectionClosedException;

/**
 *  This is a temporary fix to connection exception naming problems
 */
public class LocalConnectionClosedException extends ConnectionClosedException {
  
  private final EntityID entityID;
  
  public LocalConnectionClosedException(EntityID eid, String description, Throwable cause) {
    super(description, cause);
    this.entityID = eid;
  }
  
  public LocalConnectionClosedException(EntityID eid, ConnectionClosedException cause) {
    super(cause.getDescription(), cause);
    this.entityID = eid;
  }
  
  private static String fixDescription(EntityID eid, String description) {
    StringBuilder buffer = new StringBuilder();
    buffer.append(eid.getClassName());
    buffer.append(":");
    buffer.append(eid.getEntityName());
    buffer.append(" - ");
    buffer.append(description);
    return buffer.toString();
  }

  @Override
  public String getEntityName() {
    return entityID.getEntityName();
  }

  @Override
  public String getClassName() {
    return entityID.getClassName();
  }

  @Override
  public String getLocalizedMessage() {
    return fixDescription(entityID, super.getDescription()); 
  }

  @Override
  public String getMessage() {
    return fixDescription(entityID, super.getDescription()); 
  }
}
