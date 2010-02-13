How to run this sample
------------------------------

NOTE: Windows users please use the equivalent Batch scripts

1. Start Terracotta server first
    bin/start-sample-server.sh
     
2. Start sample: bin/start-sample.sh

3. Access Townsend sample at 
  -- http://localhost:9081/Townsend
  -- http://localhost:9082/Townsend

4. Shut down sample: bin/stop-sample.sh

*) To use Maven to run this sample:

mvn tc:start
mvn jetty:run-war -Djetty.port=9081&
mvn jetty:run-war -Djetty.port=9082&

You can obtain Maven here: http://maven.apache.org/download.html
