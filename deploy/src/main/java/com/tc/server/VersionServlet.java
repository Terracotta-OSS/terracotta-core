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
package com.tc.server;

import com.tc.util.ProductInfo;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VersionServlet extends HttpServlet {
  @Override
  protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ProductInfo productInfo = ProductInfo.getInstance();
    response.setHeader("Version", productInfo.version());
    PrintWriter writer = response.getWriter();
    writer.println("<html><title>Version Information</title><body><pre>");
    writer.println(productInfo.toLongString());
    if (productInfo.isPatched()) {
      writer.println("<br>");
      writer.println(productInfo.toLongPatchString());
    }
    writer.println("</pre></body></html>");
  }
}
