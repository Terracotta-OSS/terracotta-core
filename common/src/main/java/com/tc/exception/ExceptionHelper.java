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
 * Helper for extracting proximate cause and ultimate cause from exceptions.
 */
public interface ExceptionHelper {

  /**
   * Check whether this helper accepts the kind of t
   * @param t Throwable
   * @return True if it accepts, false if not
   */
  public boolean accepts(Throwable t);
  
  /**
   * Get closest cause
   * @param t Throwable
   * @return Closest cause
   */
  public Throwable getProximateCause(Throwable t);

  /**
   * Get original cause
   * @param t Throwable
   * @return Original cause
   */
  public Throwable getUltimateCause(Throwable t);

}
