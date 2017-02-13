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
package com.tc.exception;

import org.terracotta.exception.EntityException;
import org.terracotta.exception.RuntimeEntityException;


/**
 * An implementation of the EntityException which is designed to wrap RuntimeEntityException instances so they don't
 * require explicitly duplicated paths through the system.
 */
public class VoltronWrapperException extends EntityException {
  private static final long serialVersionUID = 1L;

  private final RuntimeEntityException exception;

  public VoltronWrapperException(RuntimeEntityException exception) {
    super(exception.getClassName(), exception.getEntityName(), exception.getDescription(), exception.getCause());
    this.exception = exception;
  }

  public RuntimeEntityException getWrappedException() {
    return this.exception;
  }
}
