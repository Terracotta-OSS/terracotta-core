How to run this sample
------------------------------

NOTE: Windows users please use the equivalent Batch scripts

1. Start Terracotta server first
     bin/start-sample-server.sh

2. Obtain hibernate-core v3.3 and install it in $TERRACOTTA_HOME/ehcache.
   You can obtain hibernate-core here: https://www.hibernate.org/6.html

2. Start sample: bin/start-sample.sh

3. Access the Events sample at 
  -- http://localhost:9081/Events
  -- http://localhost:9082/Events
  
4. Shut down sample: bin/stop-sample.sh 

*) To use Maven to run this sample:

mvn clean package
mvn -Pstart-h2 exec:java
mvn hibernate3:hbm2ddl (this step is important and necessary after a "clean")
mvn jetty:run-war -Djetty.port=9081&
mvn jetty:run-war -Djetty.port=9082&

You can obtain Maven here: http://maven.apache.org/download.html
