How to run this sample
------------------------------

NOTE: Windows users please use the equivalent Batch scripts

1. Obtain the Hibernate 3.3 core distribution, extract it, and copy
   hibernate3.jar into the $TERRACOTTA_HOME/ehcache directory.

   You can obtain Hibernate here: https://www.hibernate.org/6.html

2. Start Terracotta server first
     bin/start-sample-server.sh

3. Start the database:
     bin/start-db.sh

4. Start sample:
     bin/start-sample.sh

5. Access the Events sample at 
  -- http://localhost:9081/Events
  -- http://localhost:9082/Events
  
6. Shut down sample:
     bin/stop-db.sh
     bin/stop-sample.sh

7. Shut down the Terracotta server:
     bin/stop-sample-server.sh

*) To use Maven to start the Terracotta Server and run sample clients:

mvn tc:start&
mvn clean package
mvn -Pstart-h2 exec:java&
mvn -P9081 jetty:run-war&
mvn -P9082 jetty:run-war&

*) To use Maven to stop the Terracotta Server and sample clients:

mvn -P9081 jetty:stop
mvn -P9082 jetty:stop
mvn -Pstop-h2 exec:java
mvn tc:stop

You can obtain Maven here: http://maven.apache.org/download.html
