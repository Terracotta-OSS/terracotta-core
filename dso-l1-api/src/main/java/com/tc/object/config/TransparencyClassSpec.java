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
 * Configure and describe the custom adaption of a class
 */
public interface TransparencyClassSpec {

  /**
   * Get the class name for this spec
   * 
   * @return Name
   */
  public String getClassName();

  /**
   * @return Change applicator specification
   */
  public ChangeApplicatorSpec getChangeApplicatorSpec();

  /**
   * @return True if should use non-default constrcutor
   */
  public boolean isUseNonDefaultConstructor();

  /**
   * Set to use non default constructor
   * 
   * @param useNonDefaultConstructor True to use non-default
   */
  public void setUseNonDefaultConstructor(boolean useNonDefaultConstructor);

  /**
   * @return Get name of change applicator class
   */
  public String getChangeApplicatorClassName();

}
