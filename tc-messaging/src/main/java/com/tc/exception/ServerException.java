/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
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
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.MessageCodecException;


/**
 * This specific EntityException type is thrown in cases where an entity failed to be created because an entity with that
 * class and name already exists.
 */
public class ServerException extends Exception {
  private static final long serialVersionUID = 1L;
  private final String className;
  private final String entityName;
  private final ServerExceptionType type;
  /**
   * Creates the exception instance describing the given type-name pair.
   * 
   * @param eid
   * @param description
   * @param cause
   */
  private ServerException(EntityID eid, String description, ServerExceptionType cause) {
    super(description);
    this.className = eid.getClassName();
    this.entityName = eid.getEntityName();
    this.type = cause;
  }
  
  private ServerException(EntityID eid, String description, ServerExceptionType type, Exception cause) {
    super(description, cause);
    this.className = eid.getClassName();
    this.entityName = eid.getEntityName();
    this.type = type;
  }
  
  public String getClassName() {
    return className;
  }

  public String getEntityName() {
    return entityName;
  }
  
  public String getDescription() {
    return super.getMessage();
  }

  public ServerExceptionType getType() {
    return type;
  }
  
  public static ServerException hydrateException(EntityID eid, String description, ServerExceptionType type, StackTraceElement[] stack) {
    Exception cause = null;
    if (stack != null) {
      cause = new Exception(description);
      cause.setStackTrace(stack);
    }
    return new ServerException(eid, description, type, cause);
  }
  
  public static ServerException createNotFoundException(EntityID eid) {
    return new ServerException(eid, "not found", ServerExceptionType.ENTITY_NOT_FOUND);
  }
  
  public static ServerException wrapException(EntityID eid, Exception cause) {
    return new ServerException(eid, message(cause), ServerExceptionType.WRAPPED_EXCEPTION, cause);
  }
  
  public static ServerException createBusyException(EntityID eid) {
    return new ServerException(eid, null, ServerExceptionType.ENTITY_BUSY_EXCEPTION);
  }

  public static ServerException createMessageCodecException(EntityID eid, MessageCodecException cause) {
    return new ServerException(eid, message(cause), ServerExceptionType.MESSAGE_CODEC, cause);
  }
  
  public static ServerException createPermissionDenied(EntityID eid) {
    return new ServerException(eid, null, ServerExceptionType.PERMISSION_DENIED);
  }
  
  public static ServerException createConfigurationException(EntityID eid, Exception cause) {
    return new ServerException(eid, message(cause), ServerExceptionType.ENTITY_CONFIGURATION, cause);
  }
  
  public static ServerException createPermanentException(EntityID eid) {
    return new ServerException(eid, null, ServerExceptionType.PERMANENT_ENTITY);
  }
  
  public static ServerException createClosedException(EntityID eid) {
    return new ServerException(eid, null, ServerExceptionType.CONNECTION_CLOSED);
  }
  
  public static ServerException createReferencedException(EntityID eid) {
    return new ServerException(eid, null, ServerExceptionType.ENTITY_REFERENCED);
  }
  
  public static ServerException createEntityExists(EntityID eid) {
    return new ServerException(eid, null, ServerExceptionType.ENTITY_ALREADY_EXISTS);
  }
  
  public static ServerException createEntityUserException(EntityID eid, EntityUserException cause) {
    return new ServerException(eid, message(cause), ServerExceptionType.ENTITY_USER_EXCEPTION, cause);
  }
  
  public static ServerException createReconnectRejected(EntityID eid, Exception cause) {
    return new ServerException(eid, message(cause), ServerExceptionType.RECONNECT_REJECTED, cause);
  }
  
  public static ServerException createEntityVersionMismatch(EntityID eid, String description) {
    return new ServerException(eid, description, ServerExceptionType.ENTITY_VERSION_MISMATCH);
  }
  
  public static ServerException createEntityNotProvided(EntityID eid) {
    return new ServerException(eid, null, ServerExceptionType.ENTITY_NOT_PROVIDED);
  }
  
  private static String message(Exception exp) {
    return exp == null ? null : exp.getMessage();
  }
}
