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
