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
package com.tc.objectserver.core.api;

import java.util.Properties;

/**
 *
 */
public interface Guardian {
  public enum Op {
    // these entity operations cannout be validated, they are only here 
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
    GENERIC_OP
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
