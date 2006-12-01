package dso;

import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.ResourceHandler;

import com.tc.config.Directories;
import com.tc.util.Assert;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */

public class SharedQueueExampleHttpServer {
  private SimpleSharedQueueExample  workQueue;

  private final static String       SEP    = System.getProperty("file.separator");
  private final static String       ACTION = "/action/";
  private final String              HTML_PATH;

  public SharedQueueExampleHttpServer() {
    try {
      HTML_PATH = Directories.getInstallationRoot() + SEP + "demo" + SEP + "demo.html";
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static class DemoHttpHandler implements HttpHandler {

    private final static String      REPEAT_FIELD  = "repeat";
    private final static String      NUMBER1_FIELD = "number1";
    private final static String      NUMBER2_FIELD = "number2";
    private final static String      SIGN_FIELD    = "sign";

    private HttpContext              context;
    private SimpleSharedQueueExample workQueue;
    private boolean                  started       = false;
    private final String             htmlPath;

    public DemoHttpHandler(SimpleSharedQueueExample workQueue, String htmlPath) {
      Assert.eval(workQueue != null);
      this.workQueue = workQueue;
      this.htmlPath = htmlPath;
    }

    public String getName() {
      return "Queue Http Handler";
    }

    public HttpContext getHttpContext() {
      return context;
    }

    public void initialize(HttpContext initContext) {
      this.context = initContext;
    }

    public void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response)
        throws HttpException, IOException {
      if (pathInContext.equals("/add_work_item.asp") && request.getParameterNames().size() == 4) {
        final float number1 = getFloatForParameter(request, NUMBER1_FIELD);
        final float number2 = getFloatForParameter(request, NUMBER2_FIELD);
        final char sign = request.getParameter(SIGN_FIELD).charAt(0);
        final int timesRepeat = getIntForParameter(request, REPEAT_FIELD);
        new Thread() {
          public void run() {
            workQueue.addAction(number1, number2, sign, timesRepeat);
          }
        }.start();
        response.sendRedirect(ACTION);
        return;
      }

      if (pathInContext.equals("/clear_all.asp")) {
        workQueue.clearResults();
        response.sendRedirect(ACTION);
        return;
      }

      InputStream is = new FileInputStream(htmlPath);
      OutputStream os = response.getOutputStream();
      byte[] buffer = new byte[4096];
      while (is.available() > 0) {
        int read = is.read(buffer);
        os.write(buffer, 0, read);
      }

      // ARI: Make a summary table as well as a detail table.
      HashMap resultsSummary = workQueue.getResultsSummary();
      os.write("<hr><h1> L1 Client Summary</h1>\n".getBytes());
      os.write("<table>\n<tr><th>SERVER</th><th>EXECUTES</th><th>PUTS\n</th>\n</tr>".getBytes());

      for (Iterator i = resultsSummary.values().iterator(); i.hasNext();) {
        os.write(i.next().toString().getBytes());
      }

      os.write("</table>\n".getBytes());
      os.write("<hr> <h1>Last 15 Executions</h1>\n".getBytes());

      os.write("<ol>\n".getBytes());
      List results = workQueue.getResults();
      for (Iterator i = results.iterator(); i.hasNext();) {
        os.write(("<li>" + i.next() + "</li>\n").getBytes());
      }

      os.write("</ol></div></body></html>\n".getBytes());

      response.commit();
      request.setHandled(true);
    }

    private float getFloatForParameter(HttpRequest request, String name) {
      String param = request.getParameter(name);
      try {
        return param == null ? 0 : Float.parseFloat(param);
      } catch (NumberFormatException nfe) {
        return 0;
      }
    }

    private int getIntForParameter(HttpRequest request, String name) {
      String param = request.getParameter(name);
      try {
        return param == null ? 0 : Integer.parseInt(param);
      } catch (NumberFormatException nfe) {
        return 0;
      }
    }

    public void start() throws Exception {
      this.started = true;
    }

    public void stop() {
      started = false;
    }

    public boolean isStarted() {
      return started;
    }

  }

  public void start(int port) throws Exception {
    HttpServer server = new HttpServer();
    SocketListener listener = new SocketListener();
    listener.setPort(port);
    server.addListener(listener);
    HttpContext context = server.addContext("/");
    HttpContext actionContext = server.addContext(ACTION);
    System.out.println("Setting resource base: " + HTML_PATH);
    context.setResourceBase(HTML_PATH);
    context.addHandler(new ResourceHandler());
    this.workQueue = new SimpleSharedQueueExample();
    HttpHandler handler = new DemoHttpHandler(workQueue, HTML_PATH);
    actionContext.addHandler(handler);
    server.start();
  }

  public static void main(String[] args) throws Exception {
    new SharedQueueExampleHttpServer().start(Integer.parseInt(new String(args[0])));
  }
}