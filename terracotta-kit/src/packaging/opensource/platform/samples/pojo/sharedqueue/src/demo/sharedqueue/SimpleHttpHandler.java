/*
 *
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 *
 */
package demo.sharedqueue;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

public class SimpleHttpHandler extends AbstractHandler {

  public final static String ACTION = "/webapp";
  public final static String ACTION_ADDWORK = "/addWork";
  public final static String ACTION_GETINFO = "/getInfo";
  public final static String UNITS_OF_WORK = "unitsOfWork";

  private final demo.sharedqueue.Queue queue;

  public SimpleHttpHandler(final Queue queue) {
    this.queue = queue;
  }

  private final int getIntForParameter(final HttpServletRequest request,
      final String name) {
    String param = request.getParameter(name);
    try {
      return param == null ? 0 : Integer.parseInt(param);
    } catch (NumberFormatException nfe) {
      return 0;
    }
  }

  public final void handle(final String target,
      final HttpServletRequest request,
      final HttpServletResponse response, final int dispatch)
      throws IOException, ServletException {
    Request base_request = (request instanceof Request) ? (Request) request
        : HttpConnection.getCurrentConnection().getRequest();
    if (target.equals(ACTION_ADDWORK)) {
      doAddWork(base_request, response);
    } else if (target.equals(ACTION_GETINFO)) {
      doGetInfo(base_request, response);
    }
  }

  private final void doAddWork(final Request request,
      final HttpServletResponse response) throws IOException {
    final int unitsOfWork = getIntForParameter(request, UNITS_OF_WORK);
    final Thread processor = new Thread(new Runnable() {
      public void run() {
        for (int i = 0; i < unitsOfWork; i++) {
          queue.addJob();
          // added slight delay to improve visuals
          try {
            Thread.sleep(50);
          } catch (InterruptedException ie) {
            System.err.println(ie.getMessage());
          }
        }
      }
    });
    processor.start();
    response.sendRedirect(ACTION);
  }

  private final void doGetInfo(final Request request,
      final HttpServletResponse response) throws IOException {
    response.setContentType("text/xml");
    response.setStatus(HttpServletResponse.SC_OK);
    response
        .getWriter()
        .println(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
    response.getWriter().println("<root>");
    response.getWriter().println(queue.getXmlData());
    response.getWriter().println("</root>");
    request.setHandled(true);
  }
}
