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
package com.tc.object;

public class NamedTraversedReference implements TraversedReference {

  private final String className;
  private final String fieldName;
  private final Object value;

  public NamedTraversedReference(String fullyQualifiedFieldname, Object value) {
    this(null, fullyQualifiedFieldname, value);
  }

  public NamedTraversedReference(String className, String fieldName, Object value) {
    this.className = className;
    this.fieldName = fieldName;
    this.value = value;
  }
  
  @Override
  public Object getValue() {
    return this.value;
  }

  @Override
  public boolean isAnonymous() {
    return false;
  }

  @Override
  public String getFullyQualifiedReferenceName() {
    return this.className == null ? fieldName : className + "." + fieldName;
  }

}
