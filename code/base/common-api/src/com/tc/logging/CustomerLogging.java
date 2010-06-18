/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.logging;

/**
 * Come here to get loggers for public / customer-facing use
 */
public class CustomerLogging {

  // Logger names. You'll want to keep these unique unless you really want to cross streams
  private static final String GENERIC_CUSTOMER_LOGGER             = "general";

  private static final String DSO_CUSTOMER_GENERIC_LOGGER         = "dso";
  private static final String DSO_INSTRUMENTATON_LOGGER           = "dso.instrumentation";
  private static final String DSO_RUNTIME_LOGGER                  = "dso.runtime";

  private CustomerLogging() {
    // no need to instaniate me
  }

  public static TCLogger getConsoleLogger() {
    return TCLogging.getConsoleLogger();
  }

  public static TCLogger getGenericCustomerLogger() {
    return TCLogging.getCustomerLogger(GENERIC_CUSTOMER_LOGGER);
  }

  public static TCLogger getDSOInstrumentationLogger() {
    return TCLogging.getCustomerLogger(DSO_INSTRUMENTATON_LOGGER);
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
