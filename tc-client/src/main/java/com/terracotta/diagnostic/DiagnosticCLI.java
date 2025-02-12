/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
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
