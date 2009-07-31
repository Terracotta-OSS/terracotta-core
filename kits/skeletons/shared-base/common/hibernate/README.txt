
This README shows you how to set up Terracotta second-level cache for Hibernate using the JAR files found under the ${TERRACOTTA_HOME}/hibernate directory.

This "standalone" version of the Terracotta second-level cache for Hibernate delivers the following advantages:

* Designed for quick implementation to minimize setup and configuration time.
* Improves the performance of applications that use Hibernate as an ORM.
* Distributes in-memory data using the fastest and most efficient clustered-cache solution.

The following sections present, in order, the steps you should take to set up and run your Hibernate application with the Terracotta second-level cache.

Installing and Updating
-----------------------

The ${TERRACOTTA_HOME}/hibernate directory should contain the following JAR files:

* terracotta-hibernate-cache-<version>.jar
* terracotta-hibernate-agent-<version>.jar

where <version> is the version of the JAR file.

NOTE: terracotta-hibernate-cache-<version>.jar must be in the classpath of your application, or in WEB-INF/lib if using a WAR file.

You can update these JAR files to the latest version suitable for your installation using the following command:

   ${TERRACOTTA_HOME}/bin/tim-get.sh update terracotta-hibernate-cache

In Microsoft Windows, use this command:

   ${TERRACOTTA_HOME}\bin\tim-get.bat update terracotta-hibernate-cache

Do not add the <version> or the .jar file extension. Note that terracotta-hibernate-agent-<version>.jar is a dependency of terracotta-hibernate-cache-<version>.jar and is updated automatically when you update terracotta-hibernate-cache-<version>.jar.

Maven users can configure these JAR files in their POM and update them by updating their versions in that POM:

````````````````````````````````````````````````````````````````````````````````
 <!-- This snippet should be in the <dependencies> section. 
      The versions may need to be updated. -->
   <dependency>
     <artifactId>terracotta-hibernate-agent</artifactId>
     <version>1.0.0-SNAPSHOT</version>
     <groupId>org.terracotta.hibernate</groupId>
     <scope>runtime</scope>
   </dependency>

   <dependency>
     <artifactId>terracotta-hibernate-cache</artifactId>
     <version>1.0.0-SNAPSHOT</version>
     <groupId>org.terracotta.hibernate</groupId>
   </dependency>
````````````````````````````````````````````````````````````````````````````````


Configuring the Cache
---------------------
The Terracotta second-level cache is configured with tc-hibernate-cache.xml, which must be in your application's class path to be found by the second-level cache. For web applications, it is suggested that tc-hibernate-cache.xml be saved to WEB-INF/classes.

A typical tc-hibernate-cache.xml file could look similar to the following:

````````````````````````````````````````````````````````````````````````````````
   <?xml version="1.0" encoding="UTF-8"?> 

   <terracotta-hibernate-cache-configuration> 

   <!-- The default configuration applies to any properties not
     defined in the <cache> blocks below. -->
     
     <default-configuration> 
     
   <!-- A value of "0" equals an infinite value. WARNING: The default 
        values shown below turn off eviction. -->
       
       <logging>false</logging> 
       <time-to-idle-seconds>0</time-to-idle-seconds> 
       <time-to-live-seconds>0</time-to-live-seconds> 
       <target-max-in-memory-count>0</target-max-in-memory-count>
       <target-max-total-count>0</target-max-total-count>

  
   <!-- Use <cache> blocks to apply configuration to specific
        cache regions. -->
        
     <cache> 
     
   <!-- Define as many regions as needed. --> 

       <region-name>org.mycompany.myapplication.domain.Widget</region-name> 
       <region-name>org.mycompany.myapplication.domain.Item</region-name> 

   <!-- A value of "0" equals an infinite value. WARNING: The default 
        values shown below turn off eviction. -->

       <configuration>
         <logging>false</logging> 
         <time-to-idle-seconds>0</time-to-idle-seconds> 
         <time-to-live-seconds>0</time-to-live-seconds> 
         <target-max-in-memory-count>0</target-max-in-memory-count>
         <target-max-total-count>0</target-max-total-count>
       </configuration> 
     </cache> 

   <!-- More cache blocks here as needed. --> 

   <!-- <environment-settings> contains optional Terracotta environment 
        configuration. -->
   
     <environment-settings>
     
   <!-- <environment-settings> contains EITHER an <url> element whose value is 
        an URL to a valid Terracotta configuration, such as the Terracotta 
        server (hostname:dso-port) or a file:
        <url>file://path/to/tc-config.xml</url>
        
        OR
        a <tc-config> element containing specific valid Terracotta configuration
        element blocks. -->
        
       <tc-config>
         <clients>
         
   <!-- <logs> sets the target directory where the Terracotta client writes its 
        logs. Choose a location with enough space. A relative path begins from 
        the directory from which the application is launched. Expansion 
        variables %i (IP address) and %h (hostname) can be used in the name
        of the target directory. -->
        
           <logs>myLogs/logs-%i</logs>
         </clients>
       </tc-config>
     </environment-settings>
   
   </terracotta-hibernate-cache-configuration>
````````````````````````````````````````````````````````````````````````````````

To add eviction and control the size of the cache, edit the values of the following elements and tune these values based on results of performance tests:

* <time-to-idle-seconds> --  The maximum number of seconds an element can exist in the cache without being accessed. The element expires at this limit and will no longer be returned from the cache. 0 means no TTI eviction takes place (infinite lifetime).
* <time-to-live-seconds> -- The maximum number of seconds an element can exist in the cache regardless of use. The element expires at this limit and will no longer be returned from the cache. 0 means no TTL eviction takes place (infinite lifetime).
* <target-max-in-memory-count> -- The maximum number of elements allowed in a region in any one client (any one application server). If this target is exceeded, eviction occurs to bring the count within the allowed target. 0 means no eviction takes place (infinite size is allowed).
* <target-max-total-count> -- The maximum total number of elements allowed for a region in all clients (all application servers). If this target is exceeded, eviction occurs to bring the count within the allowed target. 0 means no eviction takes place (infinite size is allowed).

For more information, including definitions of advanced configuration properties not covered in this README, see the Terracotta Second-Level Cache for Hibernate Reference Guide (http://www.terracotta.org/web/display/Terracotta+Second-Level+Cache+for+Hibernate+Reference).

Configuring Hibernate
---------------------
Add the following to your hibernate.cfg.xml file:

   <property name="hibernate.cache.use_second_level_cache">true</property>
   <property name="hibernate.cache.provider_class">org.terracotta.hibernate.TerracottaHibernateCacheProvider</property>

If your application does not use the Hibernate query cache, you can boost performance by turning the query cache off in the following property:

   <property name="hibernate.cache.use_query_cache">false</property>


Preparing Your Application for Caching
--------------------------------------
Add the @Cache annotation to all entities in your application code that should be cached:

   @Cache(usage=CacheConcurrencyStrategy.READ_WRITE)

Alternatively, you can use the cache-mapping configuration entry in the hibernate configuration file. See section 19.2.1 of the Hibernate documentation for more information.

The following cache concurrency strategies are supported:

* CacheConcurrencyStrategy.READ_ONLY
* CacheConcurrencyStrategy.READ_WRITE
* CacheConcurrencyStrategy.NONSTRICT_READ_WRITE

For more on concurrency strategies, see the Terracotta Second-Level Cache for Hibernate Reference Guide (http://www.terracotta.org/web/display/Terracotta+Second-Level+Cache+for+Hibernate+Reference).


Starting Your Application with the Cache
----------------------------------------
You must start both your application and a Terracotta server.

Start the Terracotta server with the following command (UNIX/Linux):

   ${TERRACOTTA_HOME}/bin/start-tc-server.sh &

In Microsoft Windows, use the following command:

   ${TERRACOTTA_HOME}\bin\start-tc-server.bat

Start your application (com.example.MyApp) with the following command:

   java -javaagent:/path/to/terracotta-hibernate-agent-<version>.jar com.example.MyApp

You can start each application server running com.example.MyApp (or other applications) in the same way.

NOTE: You can run other applications with their own second-level cache in the same JVMs.

Your application should now be running with the Terracotta second-level cache. To view the cluster along with the cache, run the following command to start the Terracotta Developer Console (UNIX/Linux):

   ${TERRACOTTA_HOME}/bin/dev-console.sh &

In Microsoft Windows, use the following command:

   ${TERRACOTTA_HOME}\bin\dev-console.bat
   

More Information
----------------
Terracotta Developers Console Guide (http://www.terracotta.org/web/display/docs/Terracotta+Developer+Console)
