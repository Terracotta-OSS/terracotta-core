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
