/*
@COPYRIGHT@
*/
package demo.sharedqueue;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;

public class Main
{
   private final File cwd   = new File(System.getProperty("user.dir"));
   private int lastPortUsed;
   private Queue queue;
   private Worker worker;

   /**
    */
   static String getHostName()
   {
      try 
      {
         InetAddress addr = InetAddress.getLocalHost();
         byte[] ipAddr    = addr.getAddress();
         return addr.getHostName();
      } 
      catch (UnknownHostException e) 
      {
         return "Unknown";
      }
   }

  private int setPort(int port) {
    if (port == -1) {
      if(lastPortUsed == 0) {
        port = lastPortUsed = 1990;
      } else {
        port = ++lastPortUsed;
      }
    } else {
      lastPortUsed = port;
    }

    return port;
  }

   /**
    * Start a web server at the specified port. 
    */
   public void start(int port) 
      throws Exception 
   {
      port = setPort(port);

      System.out.println("Open your browser and go to - http://" + getHostName() + ":" + port + "/webapp\n");
      HttpServer server       = new HttpServer();
      SocketListener listener = new SocketListener();
      listener.setPort(port);
      server.addListener(listener);

      HttpContext context = server.addContext("/");
      String resourceBase = cwd.getPath();
      context.setResourceBase(resourceBase);
      context.addHandler(new ResourceHandler());

      queue = new Queue(port);
      worker   = queue.createWorker();
      
      HttpContext ajaxContext = server.addContext(SimpleHttpHandler.ACTION);
      HttpHandler ajaxHandler = new SimpleHttpHandler(queue, resourceBase);
      ajaxContext.addHandler(ajaxHandler);
      
      startKeepAlive();
      startReaper();
      server.start();
   }
   
   /**
    * Starts a thread that sends a keep-alive signal to workers in the list.
    * If a node goes down, its worker wont receive a keep-alive and will eventually
    * be identified as dead (see also, startReaper())
    */
   private void startKeepAlive()
   {
      Thread keepAlive = new Thread(new Runnable() 
         { 
            public void run() 
            { 
               while (true)
               {
                  Main.this.worker.keepAlive();
                  try { Thread.sleep(1000); } 
                  catch (InterruptedException ie) { System.err.println(ie.getMessage()); }
               }
            } 
         });
      keepAlive.start();
   }
   
   /**
    * Starts a thread to identify dead workers (From nodes that have been brought down)
    * and removes them from the (shared) list of workers.
    */
   private void startReaper()
   {
      Thread reaper = new Thread(new Runnable() 
         { 
            public void run() 
            { 
               while (true)
               {
                  Main.this.queue.reap();
                  try { Thread.sleep(1000); } 
                  catch (InterruptedException ie) { System.err.println(ie.getMessage()); }
               }
            } 
         });
      reaper.start();
   }

   /**
    * Starts the demo
    */
   public static final void main(String[] args) 
      throws Exception 
   {
      System.out.println("DSO SharedQueue"); 
      
      int port = -1;
      try { port = Integer.parseInt(args[0]); }
      catch (Exception e) { }

      new Main().start(port);
   }
}
