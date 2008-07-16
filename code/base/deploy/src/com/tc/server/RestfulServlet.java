/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.apache.commons.lang.StringUtils;

import com.tc.exception.TCRuntimeException;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestfulServlet extends HttpServlet {
  private final static String RESTFUL_METHOD_PREFIX = "method";

  private volatile String     path;

  protected void service(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
    ensureServletPath(request);

    String pathinfo = request.getPathInfo();
    if (null == pathinfo || "/".equals(pathinfo)) {
      outputMethods(response);
    } else {
      String method_name = RESTFUL_METHOD_PREFIX + StringUtils.capitalize(pathinfo.substring(1));
      try {
        Method method = getClass()
            .getDeclaredMethod(method_name, new Class[] { HttpServletRequest.class, HttpServletResponse.class });
        method.invoke(this, new Object[] { request, response });
      } catch (InvocationTargetException e) {
        throw new TCRuntimeException(e.getTargetException());
      } catch (IllegalAccessException e) {
        throw new TCRuntimeException(e);
      } catch (NoSuchMethodException e) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
      }
    }
  }

  private void ensureServletPath(final HttpServletRequest request) {
    if (null == path) {
      String context_path = request.getContextPath();

      // build a correct absolute path by using the servlet path and ensuring the value is acceptable
      String servlet_path = request.getServletPath();
      if (context_path != null && !context_path.equals(".") && !context_path.equals("/")) {
        path = context_path;
        if (servlet_path != null && !servlet_path.equals(".") && !servlet_path.equals("/")) {
          path += servlet_path;
        }
      } else {
        path = "";
      }
    }
  }

  private void outputMethods(final HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    out.println("<html><head><title>Supported methods</title></head><body>");
    out.println("<h1>Supported methods</h1>");
    out.println("<ul>");
    Method[] methods = getClass().getDeclaredMethods();
    for (int i = 0; i < methods.length; i++) {
      Class[] parameter_types = methods[i].getParameterTypes();
      if (methods[i].getName().startsWith(RESTFUL_METHOD_PREFIX) && void.class == methods[i].getReturnType()
          && 2 == parameter_types.length && HttpServletRequest.class == parameter_types[0]
          && HttpServletResponse.class == parameter_types[1]) {
        String method = StringUtils.uncapitalize(methods[i].getName().substring(RESTFUL_METHOD_PREFIX.length()));
        out.print("<li>");
        out.print("<a href=\"" + path + "/" + method + "\">");
        out.print(method);
        out.print("</a>");
        out.println("</li>");
      }
    }
    out.println("</ul>");
    out.println("</body></html>");
    out.close();
  }

  protected void print(final HttpServletResponse response, final Object message) throws IOException {
    print(response, message, HttpServletResponse.SC_OK);
  }

  protected void print(final HttpServletResponse response, final Object message, final int code) throws IOException {
    response.setContentType("text/plain");
    response.setStatus(code);
    PrintWriter out = response.getWriter();
    out.print(message);
    out.close();
  }

  protected void printOk(HttpServletResponse response) throws IOException {
    print(response, "OK");
  }

  protected void print(HttpServletResponse response, String[] values) throws IOException {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < values.length; i++) {
      builder.append(values[i]);
      builder.append("\n");
    }
    print(response, builder.toString());
  }
}