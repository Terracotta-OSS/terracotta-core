Terracotta 3.3.0-SNAPSHOT - Simple Scale for Enterprise Java
===================================================

Terracotta offers powerful solutions for scaling Java applications and 
accelerating their performance. Some of these solutions are found in this kit,
including Ehcache (/ehcache), Terracotta for Hibernate (/ehcache),
Terracotta Job Store for Quartz (/quartz) and Terracotta for Web Sessions (/sessions).

This document helps you find the information needed to implement these 
solutions for your use case.


Getting Started
---------------

For more information on getting started with Terracotta, begin with the 
Terracotta website at http://www.terracotta.org.

To locate Terracotta documentation, see 
http://www.terracotta.org/kit/reflector?kitID=3.3.0&pageID=Documentation


Join the Terracotta Community
-----------------------------
Enjoy the following benefits by signing up for a Terracotta account:

   -> Access all services with a site-wide ID
   -> Post questions to the Forums
   -> Submit JIRA reports
   -> Vote on bugs to be fixed, or features for the next release 

Sign up at http://www.terracotta.org/kit/reflector?kitID=3.3.0&pageID=Register


Resources
---------
The latest release notes are available on the web at 
http://www.terracotta.org/kit/reflector?kitID=3.3.0&pageID=ReleaseNotes

The following directories under the Terracotta home directory contain useful 
examples and information:

 - docs: tc-config-reference.xml, an annotated Terracotta configuration file;
   HTML files that link to online documentation; Javadoc.
 - config-examples: Example Terracotta configuration files for specific use 
   cases.
 - tools: Scripts for running the sample POJO and sessions applications
   are in the samples directory.
 - samples: Sample applications that demonstrate the Terracotta platform,
   including configuration files and source code.
 - bin: Scripts for administering, monitoring, and maintaining a Terracotta
   cluster, including the Terracotta Developer Console (dev-console.sh or 
   dev-console.bat), which provides deep visibility and diagnostics. Scripts are 
   documented in the Tools Catalog 
   (http://www.terracotta.org/kit/reflector?kitID=3.3.0&pageID=ToolsOverview)
 
Help and additional information is available on the web at the following URLs:

 - Product Documentation: 
    http://www.terracotta.org/kit/reflector?kitID=3.3.0&pageID=ProductDocumentation 
 - Forums:        http://forums.terracotta.org
 - Mailing Lists: http://lists.terracotta.org
 - Services:      
    http://www.terracotta.org/kit/reflector?kitID=3.3.0&pageID=SupportServices

     
Java Virtual Machine
--------------------

Terracotta clusters can be run with any supported JRE.  The product guide
contains a list of supported JREs.  The JRE used by Terracotta Clients need not
match the one used by the Terracotta Server. Control the JVM that the Terracotta
uses by setting the JAVA_HOME environment variable.

----
All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved. 
http://www.terracotta.org

