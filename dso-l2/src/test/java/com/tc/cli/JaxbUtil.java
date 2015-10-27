/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.cli;

import org.terracotta.config.util.DefaultSubstitutor;
public class JaxbUtil {

  /**
   * Make a new instance of the given type and populate any defaults declared from the schema. NOTE: This method is expensive, invoke at your own risk
   */
  public static <T> T newInstanceWithDefaults(Class<T> type) {
    T instance;
    try {
      instance = type.newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    DefaultSubstitutor.applyDefaults(instance);
    return instance;
  }

  private JaxbUtil() {
    //
  }

}
