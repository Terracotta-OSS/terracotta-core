/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.cmdline;

import java.net.InetSocketAddress;
import java.util.Properties;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.DiagnosticsFactory;

/**
 *
 */
public class Probe {

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) {
      String[] hp = args[0].split("[:]");
      InetSocketAddress inet = InetSocketAddress.createUnresolved(hp[0], Integer.parseInt(hp[1]));
      try (com.terracotta.diagnostic.Diagnostics d = (com.terracotta.diagnostic.Diagnostics)DiagnosticsFactory.connect(inet, new Properties())) {
        System.out.println(d.getThreadDump());
        System.out.println(d.getClusterState());
      } catch (ConnectionException e) {
        e.printStackTrace();
      }
  }
  
}
