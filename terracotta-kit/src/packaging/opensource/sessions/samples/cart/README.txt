How to run this sample
------------------------------

NOTE: Windows users please use the equivalent Batch scripts

1. Start Terracotta server first
     bin/start-sample-server.sh
     
2. Start sample: bin/start-sample.sh

3. Access Cart sample at 
  -- http://localhost:9081/Cart
  -- http://localhost:9082/Cart
  
4. Shut down sample: bin/stop-sample.sh

*) To use Maven to run this sample:

mvn tc:start
mvn -P9081 jetty:run-war&
mvn -P9082 jetty:run-war&

*) To use Maven to stop the Terracotta Server and sample clients:

mvn tc:stop
mvn -P9081 jetty:stop
mvn -P9082 jetty:stop

You can obtain Maven here: http://maven.apache.org/download.html
