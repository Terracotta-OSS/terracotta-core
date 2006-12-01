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
   implements HttpHandler 
{
   public final static String ACTION        = "/webapp/";
   public final static String UNITS_OF_WORK = "unitsOfWork";

   private boolean started = false;
   private HttpContext context;
   private Queue queue;
   private final String htmlPath;

   /**
    */
   public SimpleHttpHandler(Queue queue, String htmlPath)
   {
      this.queue = queue;
      this.htmlPath = htmlPath;
   }

   /**
    */
   public void handle(String pathInContext, String pathParams, HttpRequest request, HttpResponse response)
      throws HttpException, IOException 
   {
      if (pathInContext.equals("/add_work_item.asp")) 
      {
         final int unitsOfWork = getIntForParameter(request, UNITS_OF_WORK);
         Thread processor      = new Thread(new Runnable() 
            { 
               public void run() 
               { 
                  for (int i=0; i<unitsOfWork; i++)
                  {
                     queue.addJob(); 
                     try { Thread.sleep(50); } // added slight delay to improve visuals
                     catch (InterruptedException ie) { System.err.println(ie.getMessage()); }
                  }
               } 
            });
         processor.start();
         response.sendRedirect(ACTION);
      }

      else if (pathInContext.equals("/get_info_xml.asp")) 
      {
         response.setContentType("text/xml");
         response.setField("Cache-Control","no-cache");
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
    */
   public String getName() 
   {
      return "Simple Http Handler";
   }

   /**
    */
   public HttpContext getHttpContext() 
   {
      return context;
   }

   /**
    */
   public void initialize(HttpContext initContext) 
   {
      this.context = initContext;
   }

   /**
    */
   private int getIntForParameter(HttpRequest request, String name) 
   {
      String param = request.getParameter(name);
      try { return param == null ? 0 : Integer.parseInt(param); } 
      catch (NumberFormatException nfe) { return 0; }
   }

   /**
    */
   public void start() 
      throws Exception 
   {
      this.started = true;
   }

   /**
    */
   public void stop() 
   {
      started = false;
   }

   /**
    */
   public boolean isStarted() 
   {
      return started;
   }
}
