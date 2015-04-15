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
