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
package com.tc.server;

import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.util.factory.AbstractFactory;

public abstract class AbstractServerFactory extends AbstractFactory {
  private static String FACTORY_SERVICE_ID            = "com.tc.server.ServerFactory";
  private static Class  STANDARD_SERVER_FACTORY_CLASS = StandardServerFactory.class;

  public static AbstractServerFactory getFactory() {
    return (AbstractServerFactory) getFactory(FACTORY_SERVICE_ID, STANDARD_SERVER_FACTORY_CLASS);
  }

  public abstract TCServer createServer(L2ConfigurationSetupManager configurationSetupManager,
                                        TCThreadGroup threadGroup);
}
