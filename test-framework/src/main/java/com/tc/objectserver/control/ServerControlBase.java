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
package com.tc.objectserver.control;

import org.terracotta.test.util.WaitUtil;

import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.TCServerInfoMBean;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.Callable;

public abstract class ServerControlBase implements ServerControl {
  private final int                  adminPort;
  private final String               host;
  private final int                  tsaPort;
  private final ServerMBeanRetriever serverMBeanRetriever;
  private static final int           SO_CONNECT_TIMEOUT = 10000;

  public ServerControlBase(String host, int tsaPort, int adminPort) {
    this.host = host;
    this.tsaPort = tsaPort;
    this.adminPort = adminPort;
    this.serverMBeanRetriever = new ServerMBeanRetriever(host, adminPort);
  }

  @Override
  public boolean isRunning() {
    Socket socket = new Socket();
    try {
      socket.connect(new InetSocketAddress(host, adminPort), SO_CONNECT_TIMEOUT);
      if (!socket.isConnected()) throw new AssertionError();
      return true;
    } catch (IOException e) {
      System.err.println("Socket connect to " + host + ":" + adminPort + " failed because of " + e.getMessage());
      return false;
    } finally {
        try {
          socket.close();
        } catch (IOException ioe) {
          // ignore
        }
    }
  }

  @Override
  public int getAdminPort() {
    return adminPort;
  }

  @Override
  public int getTsaPort() {
    return tsaPort;
  }

  protected String getHost() {
    return host;
  }

  public L2DumperMBean getL2DumperMBean() throws Exception {
    if (!isRunning()) { throw new RuntimeException("Server is not up."); }
    return serverMBeanRetriever.getL2DumperMBean();
  }

  public TCServerInfoMBean getTCServerInfoMBean() throws Exception {
    if (!isRunning()) { throw new RuntimeException("Server is not up."); }
    return serverMBeanRetriever.getTCServerInfoMBean();
  }

  @Override
  public void waitUntilL2IsActiveOrPassive() throws Exception {
    WaitUtil.waitUntilCallableReturnsTrue(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        TCServerInfoMBean tcServerInfo = getTCServerInfoMBean();
        return tcServerInfo.isActive() || tcServerInfo.isPassiveStandby();
      }
    });
  }
}
