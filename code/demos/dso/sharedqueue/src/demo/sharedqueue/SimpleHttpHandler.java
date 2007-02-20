/*
@COPYRIGHT@
*/
package demo.sharedqueue;

import java.io.IOException;
import java.io.OutputStream;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpException;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;

public class SimpleHttpHandler
       implements HttpHandler {

   private boolean started = false;
   private HttpContext context;
   private Queue queue;
   private final String htmlPath;
   /**
    *  Description of the Field
    */
   public static final String ACTION = "/webapp/";
   /**
    *  Description of the Field
    */
   public static final String UNITS_OF_WORK = "unitsOfWork";

   /**
    *@param  queue     Description of Parameter
    *@param  htmlPath  Description of Parameter
    */
   public SimpleHttpHandler(Queue queue, String htmlPath) {
      this.queue = queue;
      this.htmlPath = htmlPath;
   }

   /**
    *@return    The Name value
    */
   public String getName() {
      return "Simple Http Handler";
   }

   /**
    *@return    The HttpContext value
    */
   public HttpContext getHttpContext() {
      return context;
   }

   /**
    *@return    The Started value
    */
   public boolean isStarted() {
      return started;
   }

   /**
    *@param  pathInContext      Description of Parameter
    *@param  pathParams         Description of Parameter
    *@param  request            Description of Parameter
    *@param  response           Description of Parameter
    *@exception  HttpException  Description of Exception
    *@exception  IOException    Description of Exception
    */
   public void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response)
          throws HttpException, IOException {
      if (pathInContext.equals("/add_work_item.asp")) {
         final int unitsOfWork = getIntForParameter(request, UNITS_OF_WORK);
         Thread processor = new Thread(
                  new Runnable() {
                     public void run() {
                        for (int i = 0; i < unitsOfWork; i++) {
                           queue.addJob();
                           try {
                              Thread.sleep(50);
                           }
                           // added slight delay to improve visuals
                           catch (InterruptedException ie) {
                              System.err.println(ie.getMessage());
                           }
                        }
                     }
                  });
         processor.start();
         response.sendRedirect(ACTION);
      }

      else if (pathInContext.equals("/get_info_xml.asp")) {
         response.setContentType("text/xml");
         response.setField("Cache-Control", "no-cache");
         OutputStream os = response.getOutputStream();
         os.write("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>".getBytes());
         os.write("<root>".getBytes());
         os.write(queue.getXmlData().getBytes());
         os.write("</root>".getBytes());
         response.commit();
         request.setHandled(true);
      }
   }

   /**
    *@param  initContext  Description of Parameter
    */
   public void initialize(HttpContext initContext) {
      this.context = initContext;
   }

   /**
    *@exception  Exception  Description of Exception
    */
   public void start()
          throws Exception {
      this.started = true;
   }

   /**
    */
   public void stop() {
      started = false;
   }

   /**
    *@param  request  Description of Parameter
    *@param  name     Description of Parameter
    *@return          The IntForParameter value
    */
   private int getIntForParameter(HttpRequest request, String name) {
      String param = request.getParameter(name);
      try {
         return param == null ? 0 : Integer.parseInt(param);
      }
      catch (NumberFormatException nfe) {
         return 0;
      }
   }
}
