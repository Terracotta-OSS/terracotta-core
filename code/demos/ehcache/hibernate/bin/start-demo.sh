mvn jetty:run-war -Djetty.port=9081 -DSTOP_PORT=9981&
sleep 3
mvn jetty:run-war -Djetty.port=9082 -DSTOP_PORT=9982&
