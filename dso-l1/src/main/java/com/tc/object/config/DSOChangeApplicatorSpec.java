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

public final class DSOChangeApplicatorSpec implements ChangeApplicatorSpec {
  private final String      changeApplicatorClassName;
  private final ClassLoader classLoader;

  public DSOChangeApplicatorSpec(String changeApplicatorClassName) {
    this(changeApplicatorClassName, null);
  }

  public DSOChangeApplicatorSpec(String changeApplicatorClassName, ClassLoader classLoader) {
    this.changeApplicatorClassName = changeApplicatorClassName;
    this.classLoader = classLoader;
  }

  @Override
  public final Class getChangeApplicator(Class clazz) {
    try {
      if (classLoader == null) {
        return Class.forName(changeApplicatorClassName);
      } else {
        return Class.forName(changeApplicatorClassName, false, classLoader);
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

}
