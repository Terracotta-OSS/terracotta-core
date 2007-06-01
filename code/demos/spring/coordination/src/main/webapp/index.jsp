<!--

  All content copyright (c) 2003-2006 Terracotta, Inc.,
  except as may otherwise be noted in a separate copyright notice.
  All rights reserved.

-->

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN">
<!-- - - - - - - - - - - - - - - - - - - - - - -->
<!--  Thread Coordination Sample Application   -->
<!--  Terracotta, Inc.                         -->
<!-- - - - - - - - - - - - - - - - - - - - - - -->
<html>
<head>
<title>Terracotta for Spring  &bull;  Thread Coordination - <%= request.getHeader("host") %></title>
<style>
   body { width: 600px; font-size: 85%; font-family: trebuchet ms, sans-serif;
          margin: 20px 40px; }
   </style>
<script type='text/javascript' src='/coordination/dwr/engine.js'></script>
<script type='text/javascript' src='/coordination/dwr/interface/Counter.js'></script>
<script type='text/javascript' src='/coordination/dwr/util.js'></script>
<script type='text/javascript'>
    timestamp = new Date().getTime();
    isServerDown = "Cannot connect to the server, it is running?";

    function checkCounter() {
      setTimeout('checkCounter()', 1000)
      setTimeout('checkTimeout()', 4000)
      try {
        Counter.getStatus(
        {
          timeout:4000,
          callback:function(counter) {
            DWRUtil.setValue("counter", counter);
            timestamp = new Date().getTime();
          },
          errorHandler:function(message) {
            DWRUtil.setValue("counter", isServerDown);
          }
        });
      } catch(e) {
        DWRUtil.setValue("counter", isServerDown);
      }
    }

    function checkTimeout() {
      if((timestamp + 10000) < new Date().getTime()) {
        DWRUtil.setValue("counter", isServerDown);
      }
    }
 
    function startDemo() {
       document.getElementById("errmsg0").style.display = 'none';
       checkCounter();
    }
</script>
</head>
<body onload="startDemo()">
   <h2>Thread Coordination:
      <% if(request.getServerPort()==8081) { %>
      Tomcat server node 1
      <% } else { %>
      Tomcat server node 2
      <% } %>
   </h2>

   <div id="errmsg0" align="center" style="color:red; background-color:yellow; border:2px solid brown; font-size:12pt; padding:5px; display:;">
      You need to have JavaScript enabled.
   </div>
   <p/>

	This sample application shows process coordination with Terracotta for Spring.
   <p/>

   <div id="counter" style="font-size: 2em; text-align: center;"></div>
   <p/>

	On each Tomcat server, the Spring-managed CounterService bean starts a
	thread in the afterPropertiesSet() method. Upon startup, that thread
	tries to acquire a lock (distributed among the Tomcat server nodes by
	Terracotta for Spring) by using normal Java synchronization
	and if it succeeds, it increments a distributed counter value every second
	(shown above).
   <p/>

	For the purposes of this demonstration, the Tomcat
	server containing this thread is referred to as the <b><i>active</i></b> Tomcat
	server since it holds the object monitor by synchronizing on it and is
	actively incrementing the counter, while the other Tomcat server that is
	waiting to synchronize on the lock is referred to as <b><i>passive</i></b>.
   <p/>

	When you stop the active Tomcat server node, the [distributed] lock that
   is held	is released and made available to the passive server. It can
   then acquire the lock (and become the active server) and start incrementing
   the distributed counter. When you restart the Tomcat server, it becomes the
   passive server.
   <p/>

   You can stop the Tomcat server nodes individually by issuing the appropriate <code>stop-tomcat</code>
   command from the <code>samples/spring/coordination</code> directory. For example, <code>./stop-tomcat1.sh</code>
   (or <code>stop-tomcat1.bat</code> in Windows) will stop the Tomcat server instance from node 1.
   <p/>

   By stopping/re-starting the Tomcat servers you can watch them switch between
   active and passive.
   <p/>

	The source code (CounterService.java) for the bean shows this typical synchronization
   pattern and can	be found in:
	<code>samples/spring/coordination/src/main/java/demo/coordination</code>
   <p/>
</body>
</html>
