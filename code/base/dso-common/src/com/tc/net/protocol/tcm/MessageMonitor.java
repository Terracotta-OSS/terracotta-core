/*
 * Created on May 14, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.tc.net.protocol.tcm;




public interface MessageMonitor {

  public void newIncomingMessage(TCMessage message);

  public void newOutgoingMessage(TCMessage message);

}