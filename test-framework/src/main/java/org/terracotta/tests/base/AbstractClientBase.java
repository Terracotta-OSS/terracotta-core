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
package org.terracotta.tests.base;

import org.terracotta.test.util.JMXUtils;

import com.tc.objectserver.control.MBeanServerInvocationProxy;
import com.tc.test.jmx.TestHandler;
import com.tc.test.jmx.TestHandlerMBean;
import com.tc.test.setup.GroupsData;

import java.text.SimpleDateFormat;
import java.util.Date;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public abstract class AbstractClientBase implements Runnable {

  public final int                      HEAVY_CLIENT_TEST_TIME = 5 * 60 * 1000;
  private static final SimpleDateFormat DATE_FORMATTER         = new SimpleDateFormat("HH:mm:ss.SSS");

  private final int                     clientIndex;
  private final MBeanServerConnection   mBeanServerConnection;

  private TestHandlerMBean        testControlMBean;

  abstract protected void doTest() throws Throwable;

  public AbstractClientBase(String args[]) {
    int index = 0;
    clientIndex = Integer.parseInt(args[index++]);
    final int testControlMbeanPort = Integer.parseInt(args[index++]);
    try {
      JMXConnector jmxConnector = JMXUtils.getJMXConnector("localhost", testControlMbeanPort);
      mBeanServerConnection = jmxConnector.getMBeanServerConnection();
    } catch (Exception e) {
      System.out.println("****** Exception while connecting to TestMBean Port[" + testControlMbeanPort + "]");
      throw new RuntimeException(e);
    }
  }

  protected final MBeanServerConnection getmBeanServerConnection() {
    return mBeanServerConnection;
  }

  public int getClientIndex() {
    return clientIndex;
  }

  @Override
  public final void run() {
    try {
      doTest();
      pass();
      System.exit(0);
    } catch (Throwable t) {
      t.printStackTrace();
      try {
        getTestControlMbean().dumpClusterState();
      } catch (Exception e) {
        new Exception("Unabled to dump cluster state.", e).printStackTrace();
      }
      getTestControlMbean().clientExitedWithException(t);
      System.exit(1);
    }
  }

  protected void pass() {
    System.err.println("[PASS: " + getClass().getName() + "]");
  }

  protected TestHandlerMBean createTestControlMBean() {
    return MBeanServerInvocationProxy.newMBeanProxy(getmBeanServerConnection(), TestHandler.TEST_SERVER_CONTROL_MBEAN,
        TestHandlerMBean.class, false);
  }

  protected synchronized TestHandlerMBean getTestControlMbean() {
    if (testControlMBean == null) {
      testControlMBean = createTestControlMBean();
    }
    return testControlMBean;
  }

  public GroupsData getGroupData(int groupIndex) {
    return getTestControlMbean().getGroupsData()[groupIndex];
  }

  public int getParticipantCount() {
    return getTestControlMbean().getParticipantCount();
  }

  public String getTerracottaUrl() {
    return getTestControlMbean().getTerracottaUrl();
  }

  public static synchronized void debug(String msg) {
    System.out.println("[D E B U G : " + DATE_FORMATTER.format(new Date()) + " '" + Thread.currentThread().getName()
                       + "'] " + msg);
  }

  public String getStackTrace(Throwable t) {
    StringBuilder stackTrace = new StringBuilder("\n" + t.getClass().getName() + " :" + t.getMessage() + "\n");
    for (StackTraceElement stackTraceElement : t.getStackTrace()) {
      stackTrace = stackTrace.append(stackTraceElement + "\n");
    }
    return stackTrace.toString();
  }

}
