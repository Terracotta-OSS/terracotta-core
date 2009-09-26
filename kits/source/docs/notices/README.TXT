Terracotta 3.1.1 - Simple Scale for Enterprise Java
===================================================

Terracotta offers powerful solutions for scaling Java applications and 
accelerating their performance.

This document helps you find the information needed to implement these 
solutions for your use case.


Getting Started
---------------

For use-case specific information on getting started with Terracotta, refer to 
the Terracotta website at 
http://www.terracotta.org/kit/reflector?kitID=3.1&pageID=GettingStarted


Join the Terracotta Community
-----------------------------
Enjoy the following benefits by signing up for a Terracotta account:

   -> Access all services with a site-wide ID
   -> Post questions to the Forums
   -> Submit JIRA reports
   -> Vote on bugs to be fixed, or features for the next release 

Sign up at http://www.terracotta.org/web/display/orgsite/Sign-up


Resources
---------
The latest release notes are available on the web at 
http://www.terracotta.org/web/display/docs/Release+Notes

The following directories under the Terracotta home directory contain useful 
examples and information:

 - docs: tc-config-reference.xml, an annotated Terracotta configuration file;
   HTML files that link to online documentation; Javadoc.
 - config-examples: Example use-case-specific Terracotta configuration files.
 - tools: Scripts for running the sample POJO and sessions applications
   are in the samples directory.
 - samples: Sample applications that demonstrate the Terracotta platform,
   including configuration files and source code.
 - bin: Scripts for administering, monitoring, and maintaining a Terracotta
   cluster, including the Terracotta Developer Console (dev-console.sh or 
   dev-console.bat), which provides deep visibility and diagnostics. Scripts are 
   documented at 
   http://www.terracotta.org/kit/reflector?kitID=3.1&pageID=ToolsOverview
 
Help and additional information is available on the web at the following URLs:

 - Documentation: http://www.terracotta.org/web/display/orgsite/Documentation 
 - Forums:        http://forums.terracotta.org
 - Mailing Lists: http://lists.terracotta.org
 - Services:      http://www.terracotta.org/web/display/enterprise/Support

     
Java Virtual Machine
--------------------

Terracotta clusters can be run with any supported JRE.  The product guide
contains a list of supported JREs.  The JRE used by Terracotta Clients need not
match the one used by the Terracotta Server. Control the JVM that the Terracotta
uses by setting the JAVA_HOME environment variable.

----
Copyright (c) 2009, Terracotta, Inc.
http://www.terracotta.org