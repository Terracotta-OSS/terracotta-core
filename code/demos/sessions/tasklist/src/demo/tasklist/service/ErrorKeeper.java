/*
@COPYRIGHT@
*/
package demo.tasklist.service;

public class ErrorKeeper {
  private String errorMsg;
  
  public ErrorKeeper(String error) {
    errorMsg = error;
  }
  
  public String getErrorMsg() {
    return errorMsg;
  }

}
