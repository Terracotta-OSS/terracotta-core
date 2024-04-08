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
package com.terracotta.diagnostic;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.terracotta.connection.DiagnosticsFactory;

/**
 *
 */
public class DiagnosticCLI {

  /**
   */
  public static void main(String[] args) throws Exception {
    String[] hostPort = args[0].split(":");
    try (Diagnostics d = (Diagnostics)DiagnosticsFactory.connect(InetSocketAddress.createUnresolved(hostPort[0], Integer.parseInt(hostPort[1])), new Properties())) {
      System.out.format("Invoking: %s at address: %s \n", Stream.of(Arrays.copyOfRange(args,1,args.length)).collect(Collectors.joining(" ")), args[0]);
      System.out.println(d.invoke(args[1], args[2], Arrays.copyOfRange(args,3,args.length)));
    }
  }
  
}
