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
 * Indicates that an object cannot be made portable.  
 */
public class TCNonPortableObjectError extends TCError {
  
  public static final String NPOE_TROUBLE_SHOOTING_GUIDE = "http://www.terracotta.org/kit/reflector?kitID=default&pageID=npoe";

  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  public TCNonPortableObjectError(String message) {
    super(wrapper.wrap(message));
  }

}
