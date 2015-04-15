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
package com.tc.exception;

import org.eclipse.jetty.util.MultiException;

/**
 * Deal with Jetty MultiException to extract useful info
 */
public class MortbayMultiExceptionHelper extends AbstractExceptionHelper<MultiException> {

  public MortbayMultiExceptionHelper() {
    super(MultiException.class);
  }

  /**
   * Get closest cause, which is defined here as the first exception in a MultiException.
   * 
   * @param t MultiException
   * @return First in the MultiException
   */
  @Override
  public Throwable getProximateCause(Throwable t) {
    if (t instanceof MultiException) {
      MultiException m = (MultiException) t;
      if (m.size() > 0) return m.getThrowable(0);
    }
    return t;
  }

}
