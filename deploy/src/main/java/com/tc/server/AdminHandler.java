/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

public class AdminHandler { // extends AbstractHttpHandler {

  // public synchronized void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse
  // response)
  // throws HttpException, IOException {
  // if (pathInContext.startsWith("/lib/") || pathInContext.startsWith("/docs/")) {
  // // leave these requests intact
  // } else if (pathInContext.endsWith(".jar")) {
  // // This is a bit of a hack. The JNLP for the Admin client will request jars relative to the location of the
  // location of the .jnlp file
  // String[] parts = pathInContext.split("/");
  // String newPath = "/lib/" + parts[parts.length - 1];
  // int prev = request.setState(HttpMessage.__MSG_EDITABLE);
  // request.setPath(newPath);
  // request.setState(prev);
  // getHttpContext().getHttpServer().service(request, response);
  // } else if (!pathInContext.startsWith("/admin/")) {
  // response.sendRedirect("/admin" + request.getPath());
  // }
  // }
}
