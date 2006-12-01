/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package dso;

import com.tc.config.Directories;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DSODemoServlet extends HttpServlet {
  private final static String      SEP           = System.getProperty("file.separator");
  private final static String      REPEAT_FIELD  = "repeat";
  private final static String      NUMBER1_FIELD = "number1";
  private final static String      NUMBER2_FIELD = "number2";
  private final static String      SIGN_FIELD    = "sign";
  private String                   HTML_PATH;
  private SimpleSharedQueueExample workQueue     = new SimpleSharedQueueExample();

  public void init() {
    try {
      HTML_PATH = Directories.getInstallationRoot() + SEP + "demo" + SEP + "demo.html";
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String pathInContext = request.getRequestURI();
    if (pathInContext.endsWith("/add_work_item.asp")) {
      final float number1 = getFloatForParameter(request, NUMBER1_FIELD);
      final float number2 = getFloatForParameter(request, NUMBER2_FIELD);
      final char sign = request.getParameter(SIGN_FIELD).charAt(0);
      final int timesRepeat = getIntForParameter(request, REPEAT_FIELD);
      new Thread() {
        public void run() {
          workQueue.addAction(number1, number2, sign, timesRepeat);
        }
      }.start();
      response.sendRedirect(request.getServletPath());
      return;
    }

    if (pathInContext.endsWith("/clear_all.asp")) {
      workQueue.clearResults();
      response.sendRedirect(request.getServletPath());
      return;
    }
  }

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    OutputStream out = response.getOutputStream();

    InputStream is = new FileInputStream(HTML_PATH);

    byte[] buffer = new byte[4096];
    while (is.available() > 0) {
      int read = is.read(buffer);
      out.write(buffer, 0, read);
    }

    // ARI: Make a summary table as well as a detail table.
    HashMap resultsSummary = workQueue.getResultsSummary();
    out.write("<hr><h1> L1 Client Summary</h1>\n".getBytes());
    out.write("<table>\n<tr><th>SERVER</th><th>EXECUTES</th><th>PUTS\n</th>\n</tr>".getBytes());

    for (Iterator i = resultsSummary.values().iterator(); i.hasNext();) {
      out.write(i.next().toString().getBytes());
    }

    out.write("</table>\n".getBytes());
    out.write("<hr> <h1>Last 15 Executions</h1>\n".getBytes());

    out.write("<ol>\n".getBytes());
    List results = workQueue.getResults();
    for (Iterator i = results.iterator(); i.hasNext();) {
      out.write(("<li>" + i.next() + "</li>\n").getBytes());
    }

    out.write("</ol></div></body></html>\n".getBytes());
  }

  private float getFloatForParameter(HttpServletRequest request, String name) {
    String param = request.getParameter(name);
    try {
      return param == null ? 0 : Float.parseFloat(param);
    } catch (NumberFormatException nfe) {
      return 0;
    }
  }

  private int getIntForParameter(HttpServletRequest request, String name) {
    String param = request.getParameter(name);
    try {
      return param == null ? 0 : Integer.parseInt(param);
    } catch (NumberFormatException nfe) {
      return 0;
    }
  }
}