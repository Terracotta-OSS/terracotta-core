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
package com.tc.logging;

/**
 * Come here to get loggers for public / customer-facing use
 */
public class CustomerLogging {

  // Logger names. You'll want to keep these unique unless you really want to cross streams
  private static final String GENERIC_CUSTOMER_LOGGER             = "general";

  private static final String DSO_CUSTOMER_GENERIC_LOGGER = "tsa";
  private static final String DSO_RUNTIME_LOGGER          = "tsa.runtime";

  private CustomerLogging() {
    // no need to instaniate me
  }

  public static TCLogger getConsoleLogger() {
    return TCLogging.getConsoleLogger();
  }

  public static TCLogger getGenericCustomerLogger() {
    return TCLogging.getCustomerLogger(GENERIC_CUSTOMER_LOGGER);
  }

  public static TCLogger getDSOGenericLogger() {
    return TCLogging.getCustomerLogger(DSO_CUSTOMER_GENERIC_LOGGER);
  }

  public static TCLogger getDSORuntimeLogger() {
    return TCLogging.getCustomerLogger(DSO_RUNTIME_LOGGER);
  }
  
  public static TCLogger getOperatorEventLogger() {
    return TCLogging.getOperatorEventLogger();
  }
}
