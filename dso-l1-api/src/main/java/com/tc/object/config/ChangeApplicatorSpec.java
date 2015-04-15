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
package com.tc.object.config;

/**
 * Defines change applicators to apply for each class.  The change applicator
 * allows a module to replace a class definition if the module needs to swap in an
 * alternate version with some differing functionality in a cluster. 
 * 
 */
public interface ChangeApplicatorSpec {
  
  /**
   * Get the change applicator for a specified class
   * @param clazz The class
   * @return The change applicator if one exists, or null otherwise
   */
  public Class getChangeApplicator(Class clazz);
}
