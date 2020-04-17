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
 */
package com.tc.config.schema.setup;

import com.tc.exception.ExceptionWrapper;
import com.tc.exception.ExceptionWrapperImpl;
import com.tc.exception.TCException;

/**
 * Thrown when the configuration system couldn't be set up. This should generally be treated as a fatal exception.
 */
public class ConfigurationSetupException extends TCException {

  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();
  
  public ConfigurationSetupException() {
    super();
  }

  public ConfigurationSetupException(String message) {
    super(wrapper.wrap(message));
  }

  public ConfigurationSetupException(Throwable cause) {
    super(cause);
  }

  public ConfigurationSetupException(String message, Throwable cause) {
    super(wrapper.wrap(message), cause);
  }

}
