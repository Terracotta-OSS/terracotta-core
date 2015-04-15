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

/**
 * Base class for all exception helpers
 */
public abstract class AbstractExceptionHelper<V extends Throwable> implements ExceptionHelper {

  private final Class<V> tClass;

  public AbstractExceptionHelper(Class<V> handledClass) {
    tClass = handledClass;
  }

  /**
   * Test if given Throwable is accepted
   * 
   * @param t Throwable
   * @return True if t instanceof
   */
  @Override
  public boolean accepts(Throwable t) {
    return tClass.isInstance(t);
  }

  /**
   * Return chained exception
   * 
   * @param t RuntimeException
   * @return Cause of t
   */
  @Override
  public Throwable getProximateCause(Throwable t) {
    return (accepts(t) && t.getCause() != null) ? t.getCause() : t;
  }

  /**
   * Always throws AssertiontError
   * 
   * @param t Param ignored
   * @return Always AssertionError
   */
  @Override
  public Throwable getUltimateCause(Throwable t) {
    throw new AssertionError();
  }

}
