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

import com.tc.config.schema.setup.ConfigurationSetupManagerFactory;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.exception.MortbayMultiExceptionHelper;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.lang.ThrowableHandlerImpl;
import com.tc.logging.TCLogging;

public class TCServerMain {

  public static void main(final String[] args) {
    ThrowableHandler throwableHandler = new ThrowableHandlerImpl(TCLogging.getLogger(TCServerMain.class));
    throwableHandler.addHelper(new MortbayMultiExceptionHelper());

    try {
      TCThreadGroup threadGroup = new TCThreadGroup(throwableHandler);

      ConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                                    args,
                                                                                                    StandardConfigurationSetupManagerFactory.ConfigMode.L2,
                                                                                                    new FatalIllegalConfigurationChangeHandler(), null);
      AbstractServerFactory serverFactory = AbstractServerFactory.getFactory();
      TCServer server = serverFactory.createServer(factory.createL2TVSConfigurationSetupManager(null, true),
                                                   threadGroup);
      server.start();

      server.waitUntilShutdown();

    } catch (Throwable t) {
      throwableHandler.handleThrowable(Thread.currentThread(), t);
    }
  }
}
