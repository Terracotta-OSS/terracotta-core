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
package com.tc.admin;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.tc.test.TCTestCase;
import com.tc.util.ProductInfo;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UpdateCheckRequestTest extends TCTestCase {
  private Server fServer;
  private int    fPort;

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    fServer = new Server(0);
    ServletHolder servletHolder = new ServletHolder(new TestServlet());
    servletHolder.setInitOrder(1);
    ServletContextHandler context = new ServletContextHandler(fServer, "/", ServletContextHandler.SESSIONS);
    context.addServlet(servletHolder, "/test");
    fServer.start();
    fPort = fServer.getConnectors()[0].getLocalPort();
  }

  @Override
  protected void tearDown() throws Exception {
    if (fServer != null) {
      fServer.stop();
    }
    super.tearDown();
  }

  public void testUpdateCheckRequest() throws Exception {
    URL url = getURL();
    HttpClient httpClient = new HttpClient();
    String response = getResponseBody(url, httpClient).trim();
    if (!response.equals("OK")) {
      fail(response);
    }
  }

  public static String getResponseBody(URL url, HttpClient client) throws ConnectException, IOException {
    GetMethod get = new GetMethod(url.toString());

    get.setFollowRedirects(true);
    try {
      int status = client.executeMethod(get);
      if (status != HttpStatus.SC_OK) { throw new ConnectException(
                                                                   "The http client has encountered a status code other than ok for the url: "
                                                                       + url + " status: "
                                                                       + HttpStatus.getStatusText(status)); }
      return get.getResponseBodyAsString();
    } finally {
      get.releaseConnection();
    }
  }

  private URL getURL() throws MalformedURLException {
    URL url = constructCheckURL(ProductInfo.getInstance(), 0);
    return new URL("http://localhost:" + fPort + "/test?" + url.getQuery());
  }

  public static URL constructCheckURL(ProductInfo productInfo, int id) throws MalformedURLException {
    String defaultPropsUrl = "http://www.terracotta.org/kit/reflector?kitID=default&pageID=update.properties";
    String propsUrl = System.getProperty("terracotta.update-checker.url", defaultPropsUrl);
    StringBuffer sb = new StringBuffer(propsUrl);

    sb.append(defaultPropsUrl.equals(propsUrl) ? '&' : '?');

    sb.append("id=");
    sb.append(URLEncoder.encode(Integer.toString(id)));
    sb.append("&os-name=");
    sb.append(URLEncoder.encode(System.getProperty("os.name")));
    sb.append("&jvm-name=");
    sb.append(URLEncoder.encode(System.getProperty("java.vm.name")));
    sb.append("&jvm-version=");
    sb.append(URLEncoder.encode(System.getProperty("java.version")));
    sb.append("&platform=");
    sb.append(URLEncoder.encode(System.getProperty("os.arch")));
    sb.append("&tc-version=");
    sb.append(URLEncoder.encode(productInfo.version()));
    sb.append("&tc-product=");
    sb.append(productInfo.license().equals(ProductInfo.DEFAULT_LICENSE) ? "oss" : "ee");
    sb.append("&source=console");

    return new URL(sb.toString());
  }

}

class TestServlet extends HttpServlet {
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);

    String[] expectedParams = { "kitID", "pageID", "id", "os-name", "jvm-name", "jvm-version", "platform",
        "tc-version", "tc-product", "source" };
    Map paramMap = request.getParameterMap();
    for (String expectedParam : expectedParams) {
      if (paramMap.get(expectedParam) == null) {
        response.getWriter().println("Missing parameter '" + expectedParam + "'");
        return;
      }
    }
    if (paramMap.size() != expectedParams.length) {
      response.getWriter().println("Parameter count doesn't match expected count='" + expectedParams.length + "'");
      return;
    }
    response.getWriter().println("OK");
  }
}
