Getting Started with Terracotta - Simple Scale for Enterprise Java
========================================================================

Terracotta offers powerful solutions for scaling Java applications and 
accelerating their performance.

This document helps you find the information needed to implement these 
solutions for your use case.

For a general installation of Terracotta, see the 

Installation
------------
General installation instructions are in INSTALL.TXT in the Terracotta home 
directory. 

*Terracotta for Hibernate*
Terracotta for Hibernate is an easy-to-install performance boost for Hibernate
applications.

If you are installing Terracotta for Hibernate, see INSTALL_HIBERNATE_ONLY.TXT
in the Terracotta home directory. More Terracotta for Hibernate documentation 
is at http://www.terracotta.org/web/display/orgsite/Terracotta+for+Hibernate.

*Terracotta for Hibernate (Full Terracotta Integration)*
Terracotta for Hibernate (Full Terracotta Integration) offers a performance 
boost along with mainstream Terracotta features, such as session clustering, 
for Hibernate applications.

If you are installing Terracotta for Hibernate (Full Terracotta Integration), 
see http://www.terracotta.org/web/display/orgsite/
Configuring+Terracotta+With+Hibernate+Full+Integration.


Distributed Cache
-----------------
The Terracotta Distributed Cache is an interface providing a simple, efficient, 
coherent, and fail-safe distributed eviction solution for map elements.

For more information on how to use the Terracotta Distributed Cache, see
http://www.terracotta.org/web/display/docs/Terracotta+Distributed+Cache.
  

Join the Terracotta Community
-----------------------------
Enjoy the following benefits by signing up for a Terracotta account:

   -> Access all services with a site-wide ID
   -> Post questions to the Forums
   -> Submit JIRA reports
   -> Vote on bugs to be fixed, or features for the next release 

Sign up at http://www.terracotta.org/web/display/orgsite/Sign-up.


Resources
---------
Release notes for this release are in releasenotes.txt in the Terracotta home
directory. The latest release notes are also available on the web at 
http://www.terracotta.org/web/display/docs/Release+Notes.

The following directories under the Terracotta home directory contain useful 
examples and information:

 - docs: tc-config-reference.xml, an annotated Terracotta configuration file;
   HTML files that link to online documentation; Javadoc.
 - config-examples: Example use-case-specific Terracotta configuration files.
 - tools: Scripts for running the sample POJO and sessions applications
   are in the samples directory.
 - samples: Sample applications, including configuration files and source code.
 - bin: Scripts for administering, monitoring, and maintaining a Terracotta
   cluster, including the Terracotta Developer Console (dev-console.sh or 
   dev-console.bat), which provides deep visibility and diagnostics. Scripts are 
   documented at http://www.terracotta.org/web/display/docs/Tools+Overview.
 
Help and additional information is available on the web at the following URLs:

 - Documentation: http://www.terracotta.org/web/display/orgsite/Documentation 
 - Forums:        http://forums.terracotta.org
 - Mailing Lists: http://lists.terracotta.org
 - Services:      http://www.terracotta.org/web/display/enterprise/Support

Installation instructions for Terracotta DSO Eclipse Plug-in are at
http://www.terracotta.org/web/display/docs/Eclipse+Plugin+Guide.

     
Java Virtual Machine
--------------------

Terracotta clusters can be run with any supported JRE.  The product guide
contains a list of supported JREs.  The JRE used by Terracotta Clients need not
match the one used by the Terracotta Server. Control the JVM that the Terracotta
uses by setting the JAVA_HOME environment variable.

----
Copyright (c) Terracotta, Inc.
http://www.terracotta.org
