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
package org.terracotta.test.util;

/**
 * Just a util classes for delegating all the logs to single place. Easier to replace with any good logging framework
 * going futher
 */
public class LogUtil {


  public static void info(Class<?> clazz, String message) {
    System.out.println(clazz.getSimpleName() + " :: " + message);
  }

  public static void debug(Class<?> clazz, String message) {
    System.out.println(clazz.getSimpleName() + " :: " + message);
  }
}
