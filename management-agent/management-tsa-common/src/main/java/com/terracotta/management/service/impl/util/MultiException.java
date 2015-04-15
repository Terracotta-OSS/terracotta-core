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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class MultiException extends Exception {

  private final List<Throwable> throwables;

  public MultiException(String message, List<Throwable> throwables) {
    super(message);
    this.throwables = Collections.unmodifiableList(new ArrayList<Throwable>(throwables));
  }

  public List<Throwable> getThrowables() {
    return throwables;
  }

  public String getMessage() {
    StringBuilder errorMessage = new StringBuilder();
    errorMessage.append(super.getMessage());
    errorMessage.append("; collected ");
    errorMessage.append(throwables.size());
    errorMessage.append(" exception(s):");

    for (Throwable throwable : throwables) {
      errorMessage.append(System.getProperty("line.separator"));
      errorMessage.append(" [");
      errorMessage.append(throwable.getClass().getName());
      errorMessage.append(" - ");
      errorMessage.append(throwable.getMessage());
      errorMessage.append("]");
    }
    return errorMessage.toString();
  }

}
