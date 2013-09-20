/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.mockl2.test;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MockUtil {
  
  static Logger logger = Logger.getLogger(MockUtil.class.getName());
  
  public static void main(String[] args) {
    MockUtil.logInfo("dd");
  }
  
  public static void logInfo(String arr){
   logger.log(Level.INFO, arr);
  }

  public static void logInfo(String arr, boolean force){
    if(force) {
      System.err.println(arr);
    }
   }
}
