/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.distrunner;

import com.tcsimulator.ClientSpec;
import com.tcsimulator.distrunner.ServerSpec;

import java.net.InetAddress;
import java.net.UnknownHostException;

import junit.framework.TestCase;

public class ArgParserTest extends TestCase {
  /*
   * public void testBasics() throws Throwable { String[] args = null; SpecFactory specFactory = new SpecFactoryImpl();
   * String client1Host = "client1Host"; String client1TestHome = "/client1TestHome"; int clientIntensity = 2; int
   * client1VMCount = 2; int client1ExecutionCount = 2; String client2Host = "client2Host"; String client2TestHome =
   * "/client2TestHome"; String client1Description = "client://" + client1Host + client1TestHome + "?vm.count=" +
   * client1VMCount + "&execution.count=" + client1ExecutionCount; String client2Description = "client://" + client2Host +
   * client2TestHome; ClientSpec client1 = new ClientSpecImpl(client1Host, client1TestHome, client1VMCount,
   * client1ExecutionCount); ClientSpec client2 = new ClientSpecImpl(client2Host, client2TestHome, 1, 1); Collection
   * clientSpecs = new LinkedList(); clientSpecs.add(client1); clientSpecs.add(client2); String serverHostname =
   * "myServer"; String serverTestHome = "/my/server/test/home"; ServerSpec server = new ServerSpecImpl(serverHostname,
   * serverTestHome); String testClassname = "myTestClassname"; args = new String[] { "server://" + serverHostname +
   * serverTestHome, client1Description, client2Description, "classname=" + testClassname }; ArgParser argParser = new
   * ArgParser(args, specFactory, true); assertEquals(clientSpecs, argParser.getClientSpecs()); assertEquals(server,
   * argParser.getServerSpec()); assertEquals(testClassname, argParser.getTestClassname()); }
   */
  /*
   * public void testNoArguments() { String[] args = null; SpecFactory specFactory = new SpecFactoryImpl(); Collection
   * clientSpecs = new LinkedList(); String serverHostname = "myServer"; String serverTestHome = "/my/server/test/home";
   * ServerSpec server = new ServerSpecImpl(serverHostname, serverTestHome, ArgParser.DEFAULT_SERVER_CACHE,
   * ArgParser.DEFAULT_HTTP_PORT, ArgParser.DEFAULT_JMX_PORT, ArgParser.DEFAULT_DSO_PORT, new ArrayList(),
   * ServerSpec.TEST_SERVER); String testClassname = "undefined"; int intensity = -1; args = new String[] { "server://" +
   * serverHostname + serverTestHome, "classname=" + testClassname }; ArgParser argParser; try { argParser = new
   * ArgParser(args, specFactory, true, false); } catch (ArgException e) { throw new AssertionError(e); }
   * assertEquals(clientSpecs, argParser.getClientSpecs()); assertEquals(server.toString(),
   * argParser.getServerSpec().toString()); assertEquals(testClassname, argParser.getTestClassname());
   * assertEquals(intensity, argParser.getIntensity()); }
   */
  public void testJvmOptParsing() throws UnknownHostException {
    InetAddress host = InetAddress.getLocalHost();
    String hostName = host.getHostName();

    String clientInfo = "client://" + hostName
                        + "/test?vm.count=3&execution.count=5&jvm.args=-server,-Xms256m,-Xmx256m,-Xss128k";
    SpecFactory specFactory = new SpecFactoryImpl();
    ArgParser parser;
    try {
      parser = new ArgParser(new String[] { clientInfo }, specFactory, false, false);
    } catch (ArgException e) {
      throw new AssertionError(e);
    }
    assertEquals(clientInfo, ArgParser.getArgumentForClientSpec((ClientSpec) parser.getClientSpecs().iterator().next()));
  }

  public void testServerParsing() throws ArgException {
    String[] cmdLine = new String[] { "server://cfisherpc.terracotta.lan/tmp/dso-sandbox?"
                                      + "config.dso.http.port=3333&config.dso.jmx.port=2222&config.dso.dso.port=5555;"
                                      + "control://cfisherpc.terracotta.lan/tmp/dso-sandbox" };

    ArgParser parser = new ArgParser(cmdLine, new SpecFactoryImpl(), true, true);
    ServerSpec controlServer = parser.getControlServerSpec();
    System.out.println(controlServer.toString());
    ServerSpec testServer = parser.getServerSpec();
    System.out.println(testServer.toString());
  }

}
