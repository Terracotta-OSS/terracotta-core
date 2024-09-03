/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.exception;

import com.tc.object.EntityID;


/**
 * This specific EntityException type is thrown in cases where an entity failed to be created because an entity with that
 * class and name already exists.
 */
public class ServerRuntimeException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private final EntityID eid;
  private final ServerExceptionType type;
  /**
   * Creates the exception instance describing the given type-name pair.
   * 
   * @param eid
   * @param description
   * @param cause
   */
  private ServerRuntimeException(EntityID eid, String description, ServerExceptionType cause, Exception e) {
    super(description, e);
    this.eid = eid;
    this.type = cause;
  }

  public String getClassName() {
    return eid.getClassName();
  }

  public String getEntityName() {
    return eid.getEntityName();
  }
  
  public String getDescription() {
    return super.getMessage();
  }

  public ServerExceptionType getType() {
    return type;
  }

  public static ServerRuntimeException createServerUncaught(EntityID eid, Exception cause) {
    return new ServerRuntimeException(eid, null, ServerExceptionType.ENTITY_SERVER_UNCAUGHT, cause);
  }
}
