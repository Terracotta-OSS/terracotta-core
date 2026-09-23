/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2026
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
package com.tc.objectserver.entity;

import org.terracotta.entity.EntityResponse;

/**
 *
 * @author myronscott
 */
public interface MessageResponse<T extends EntityResponse> {
  /**
   * Was an exception thrown in the execution of this message.
   *
   * @return true if a message resulted in an exception during invoke
   */
  boolean wasExceptionThrown();
  /**
   * An exception thrown during execution of the message on the active server.
   * @return null if no exception, else the exception that was thrown during invoke
   */
  Exception getException();
  /**
   * @return the response of the invoke or null if an exception that occurred
   */
  T getResponse();
}
