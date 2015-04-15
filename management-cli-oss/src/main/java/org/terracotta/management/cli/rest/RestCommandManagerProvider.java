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
package org.terracotta.management.cli.rest;

public class RestCommandManagerProvider {

  private static final String COMMAND_MANAGER_PROPERTY_KEY = "manager";
  private static RestCommandManager manager;

  private RestCommandManagerProvider() {
  }

  @SuppressWarnings("unchecked")
  public static RestCommandManager getCommandManager() {
    if (manager == null) {
      String clazz = System.getProperty(COMMAND_MANAGER_PROPERTY_KEY);
      Class<RestCommandManager> managerClazz;
      try {
        managerClazz = (Class<RestCommandManager>) Class.forName(clazz);
        manager = managerClazz.newInstance();
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      } catch (InstantiationException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    }
    return manager;
  }

}
