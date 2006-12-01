/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
    ProductInfo productInfo = ProductInfo.getThisProductInfo();

    PrintWriter writer = response.getWriter();
    writer.println("<html><title>Version Information</title><body><pre>");
    response.getWriter().println(productInfo.toLongString());
    writer.println("</pre></body></html>");
  }
}
