How to run this sample
------------------------------

NOTE: Windows users please use the equivalent Batch scripts

1. Start Terracotta server first
     bin/start-sample-server.sh

2. Obtain the Hibernate v3 JAR and install it in $TERRACOTTA_HOME/ehcache.
   You can obtain hibernate here: https://www.hibernate.org/6.html

2. Start the database:
     bin/start-db.sh

2. Start sample:
     bin/start-sample.sh

3. Access the Events sample at 
  -- http://localhost:9081/Events
  -- http://localhost:9082/Events
  
4. Shut down sample:
     bin/stop-db.sh
     bin/stop-sample.sh

*) To use Maven to run this sample:

mvn clean package
mvn -Pstart-h2 exec:java&
mvn -P9081 jetty:run-war&
mvn -P9082 jetty:run-war&

*) To use Maven to stop the Terracotta Server and sample clients:

mvn -P9081 jetty:stop
mvn -P9082 jetty:stop
mvn -Pstop-h2 exec:java

You can obtain Maven here: http://maven.apache.org/download.html
