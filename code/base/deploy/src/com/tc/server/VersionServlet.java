/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import com.tc.util.ProductInfo;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VersionServlet extends HttpServlet {
  protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ProductInfo productInfo = ProductInfo.getInstance();

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
