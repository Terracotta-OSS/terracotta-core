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
package com.tc.spi;

import java.util.Properties;

/**
 *
 */
public interface Guardian {
  public enum Op {
    // these entity operations cannot be validated, they are only here 
    // for logging purposes. a false return on validation will not be honored
    ENTITY_CREATE,
    ENTITY_FETCH,
    ENTITY_RECONFIGURE,
    ENTITY_DESTROY,
    // false return on these operations will result in a refusal of the network connection
    CONNECT_SERVER,
    CONNECT_CLIENT,
    
    SERVER_DUMP,
    SERVER_EXIT,
    GENERIC_OP,
    AUDIT_OP,

    SECURITY_OP
  };
  /**
   * Validate that an operation can proceed based on property context.  
   * @param op the operation attempted
   * @param context properties associated with the operation
   * @return false if the operation should not proceed, true to allow.  (NOTE: only some operations
   *   honor this return. Entity operations currently will not honor this flag. Any exception thrown 
   *   from a guardian will be logged and the operation will be allowed to proceed.
   */
  boolean validate(Op op, Properties context);
}
