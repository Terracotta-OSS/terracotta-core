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
package com.terracotta.management.service.impl.util;

import org.terracotta.management.resource.ErrorEntity;

/**
 * @author Ludovic Orban
 */
public class ManagementSourceException extends RuntimeException {
  private final ErrorEntity errorEntity;

  public ManagementSourceException(Throwable t) {
    super(t);
    errorEntity = null;
  }

  public ManagementSourceException(String msg) {
    super(msg);
    errorEntity = null;
  }

  public ManagementSourceException(String message, ErrorEntity errorEntity) {
    super(message);
    this.errorEntity = errorEntity;
  }

  public ErrorEntity getErrorEntity() {
    return errorEntity;
  }
}
