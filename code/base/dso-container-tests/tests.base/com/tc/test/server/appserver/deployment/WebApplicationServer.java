/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import org.xml.sax.SAXException;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.StandardAppServerParameters;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

public interface WebApplicationServer extends Server {

  public StandardAppServerParameters getServerParameters();

  public WebApplicationServer addWarDeployment(Deployment warDeployment, String context);

  public void deployWar(Deployment warDeployment, String context);

  public void redeployWar(Deployment warDeployment, String context);

  public void undeployWar(Deployment warDeployment, String context);

  public WebResponse ping(String url) throws MalformedURLException, IOException, SAXException;

  public WebResponse ping(String url, WebConversation wc) throws MalformedURLException, IOException, SAXException;

  public int getPort();

  public File getWorkingDirectory();

}