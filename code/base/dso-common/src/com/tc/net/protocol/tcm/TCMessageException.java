/*
 * Created on Apr 22, 2004 To change the template for this generated file go to Window&gt;Preferences&gt;Java&gt;Code
 * Generation&gt;Code and Comments
 */
package com.tc.net.protocol.tcm;

import com.tc.exception.TCException;

/**
 * @author Orion Letizi To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TCMessageException extends TCException {

  /**
   * 
   */
  public TCMessageException() {
    super();
  }

  /**
   * @param arg0
   */
  public TCMessageException(String arg0) {
    super(arg0);
  }

  /**
   * @param arg0
   */
  public TCMessageException(Throwable arg0) {
    super(arg0);
  }

  /**
   * @param arg0
   * @param arg1
   */
  public TCMessageException(String arg0, Throwable arg1) {
    super(arg0, arg1);
  }

}