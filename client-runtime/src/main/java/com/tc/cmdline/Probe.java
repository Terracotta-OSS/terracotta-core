/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package com.tc.cmdline;

import java.net.InetSocketAddress;
import java.util.Properties;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.DiagnosticsFactory;

/**
 *
 * @author mscott2
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
