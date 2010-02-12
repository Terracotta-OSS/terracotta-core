mvn clean package
mvn -Pstart-h2 exec:java
mvn hibernate3:hbm2ddl (this step is important and necessary after a "clean")
mvn jetty:run-war -Djetty.port=9081&
mvn jetty:run-war -Djetty.port=9082&

