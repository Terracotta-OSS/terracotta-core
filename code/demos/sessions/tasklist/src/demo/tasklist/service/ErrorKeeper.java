/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
*/
package demo.tasklist.service;

public class ErrorKeeper implements java.io.Serializable {
  private String errorMsg;
  
  public ErrorKeeper(String error) {
    errorMsg = error;
  }
  
  public String getErrorMsg() {
    return errorMsg;
  }

}
