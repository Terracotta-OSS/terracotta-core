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
 */
package com.tc.exception;

/**
 *
 */
public enum ServerExceptionType {
  CONNECTION_CLOSED,CONNECTION_SHUTDOWN,ENTITY_REFERENCED,ENTITY_ALREADY_EXISTS,
  ENTITY_CONFIGURATION,ENTITY_NOT_FOUND,ENTITY_NOT_PROVIDED,ENTITY_SERVER,
  ENTITY_SERVER_UNCAUGHT,ENTITY_VERSION_MISMATCH,ENTITY_USER_EXCEPTION,
  ENTITY_BUSY_EXCEPTION,PERMISSION_DENIED,RECONNECT_REJECTED,MESSAGE_CODEC,
  WRAPPED_EXCEPTION,PERMANENT_ENTITY;
}
