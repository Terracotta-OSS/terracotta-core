/*
@COPYRIGHT@
*/
package demo.sharedqueue;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MBeanServerNotification;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpHandler;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;

public class Main {
   private final File cwd = new File(System.getProperty("user.dir"));
   private int lastPortUsed;
   private Queue queue;
   private Worker worker;

   /**
    *  Start a web server at the specified port.
    *
    *@param  port           Description of Parameter
    *@exception  Exception  Description of Exception
    */
   public void start(int port)
          throws Exception {
      String nodeId = registerForNotifications();
      port = setPort(port);

      System.out.println("DSO SharedQueue (node " + nodeId + ")");
      System.out.println("Open your browser and go to - http://" + getHostName() + ":" + port + "/webapp\n");
      HttpServer server = new HttpServer();
      SocketListener listener = new SocketListener();
      listener.setPort(port);
      server.addListener(listener);

      HttpContext context = server.addContext("/");
      String resourceBase = cwd.getPath();
      context.setResourceBase(resourceBase);
      context.addHandler(new ResourceHandler());

      queue  = new Queue(port);
      worker = queue.createWorker(nodeId);

      HttpContext ajaxContext = server.addContext(SimpleHttpHandler.ACTION);
      HttpHandler ajaxHandler = new SimpleHttpHandler(queue, resourceBase);
      ajaxContext.addHandler(ajaxHandler);

      startReaper();
      server.start();
   }

   private int setPort(int port) {
      if (port == -1) {
         if (lastPortUsed == 0) {
            port = lastPortUsed = 1990;
         }
         else {
            port = ++lastPortUsed;
         }
      }
      else {
         lastPortUsed = port;
      }

      return port;
   }

   /**
    *  Starts a thread to identify dead workers (From nodes that have been
    *  brought down) and removes them from the (shared) list of workers.
    */
   private void startReaper() {
      Thread reaper = new Thread(
               new Runnable() {
                  public void run() {
                     while (true) {
                        Main.this.queue.reap();
                        try {
                           Thread.sleep(1000);
                        }
                        catch (InterruptedException ie) {
                           System.err.println(ie.getMessage());
                        }
                     }
                  }
               });
      reaper.start();
   }

   /**
    *  Starts the demo
    *
    *@param  args           The command line arguments
    *@exception  Exception  Description of Exception
    */
   public static final void main(String[] args)
          throws Exception {
      int port = -1;
      try { port = Integer.parseInt(args[0]); }
      catch (Exception e) { }

      (new Main()).start(port);
   }

   /**
    *@return    The HostName value
    */
   static String getHostName() {
      try {
         InetAddress addr = InetAddress.getLocalHost();
         byte[] ipAddr = addr.getAddress();
         return addr.getHostName();
      }
      catch (UnknownHostException e) {
         return "Unknown";
      }
   }
   
   /**
    * Registers this client for JMX notifications. 
    * @returns This clients Node ID
    */
   private String registerForNotifications() 
      throws Exception {
      java.util.List servers              = MBeanServerFactory.findMBeanServer(null);
      MBeanServer server                  = (MBeanServer)servers.get(0);
      final ObjectName clusterBean        = new ObjectName("com.terracottatech:type=Terracotta Cluster,name=Terracotta Cluster Bean");
      ObjectName delegateName             = ObjectName.getInstance("JMImplementation:type=MBeanServerDelegate");
      final java.util.List   clusterBeanBag   = new java.util.ArrayList();
      
      // listener for newly registered MBeans
      NotificationListener listener0 = new NotificationListener() {
         public void handleNotification(Notification notification, Object handback) {
            synchronized (clusterBeanBag) {
               clusterBeanBag.add(handback);
               clusterBeanBag.notifyAll();
            }
         }
      };

      // filter to let only clusterBean passed through
      NotificationFilter filter0 = new NotificationFilter() {
         public boolean isNotificationEnabled(Notification notification) {
            if (notification.getType().equals("JMX.mbean.registered")
                  && ((MBeanServerNotification) notification)
                        .getMBeanName().equals(clusterBean))
               return true;
            return false;
         }
      };
      
      // add our listener for clusterBean's registration
      server.addNotificationListener(delegateName, listener0, filter0,
            clusterBean);

      // because of race condition, clusterBean might already have registered
      // before we registered the listener
      java.util.Set allObjectNames = server.queryNames(null, null);

      if (!allObjectNames.contains(clusterBean)) {
         synchronized (clusterBeanBag) {
            while (clusterBeanBag.isEmpty()) {
               clusterBeanBag.wait();
            }
         }
      }

      // clusterBean is now registered, no need to listen for it
      server.removeNotificationListener(delegateName, listener0);
      
      // listener for clustered bean events
      NotificationListener listener1 = new NotificationListener() {
         public void handleNotification(Notification notification, Object handback) {
            String nodeId = notification.getMessage();
            Worker worker = Main.this.queue.getWorker(nodeId);
            if (worker != null) {
               worker.markForExpiration();
            } else {
               System.err.println("Worker for nodeId: " + nodeId + " not found.");
            }
         }
      };

      // filter for nodeDisconnected notifications only
      NotificationFilter filter1 = new NotificationFilter() {
         public boolean isNotificationEnabled(Notification notification) {
            return notification.getType().equals("com.tc.cluster.event.nodeDisconnected");
         }
      };

      // now that we have the clusterBean, add listener for membership events
      server.addNotificationListener(clusterBean, listener1, filter1, clusterBean);
      return (server.getAttribute(clusterBean, "NodeId")).toString();
   }
}
