Terracotta
==========

Documentation: http://www.terracotta.org/web/display/orgsite/Documentation 
Forums:        http://forums.terracotta.org
Mailing Lists: http://lists.terracotta.org
Services:      http://www.terracotta.org/web/display/enterprise/Support


Getting Started
---------------

Below are abbreviated install steps for Terracotta.

Terracotta
   Unix/Linux:
      - Unzip/untar the Terracotta software.
      - cd into the top-level directory.
      - Launch the Sample Application Launcher by entering the command:
           ./tools/pojo/samples.sh
   Windows:
      - Run the Terracotta installer.
      - Launch the Sample Application Launcher by entering the command:
           tools\pojo\samples.bat
           
Terracotta DSO Eclipse Plug-in
   Follow the installation directions at:
      - http://www.terracotta.org/web/display/docs/Eclipse+Plugin+Guide


Sign Up
--------------
To get the most out of your Terracotta experience, please sign up for a Terracotta account:
    - http://www.terracotta.org/web/display/orgsite/Sign-up

With a Terracotta account, you can:
    - Access all services with a site-wide ID
    - Post questions to the Forums
    - Submit JIRA reports
    - Vote on bugs to be fixed, or features for the next release 
    
    
Configuration File Samples
--------------------------

The tc-config-reference.xml file can be found in "docs" directory.

This "config-examples" directory contains sample Terracotta configuration files
appropriate for a typical deployment.

The XML schema describing the configuration file format is documented in the
"schema" directory; see schema/index.html.

Sample Applications
-------------------

Terracotta ships with sample applications to show by example how to use
Terracotta to cluster the JVM.  The sample applications for Terracotta Sessions
are available from links in the Session Configurator tool:

Unix/Linux:
   ./tools/sessions/sessions-configurator.sh
Windows:
   tools\sessions\sessions-configurator.bat

For Terracotta Pojos, the sample applications are in the "samples/pojo" directory.
You can exercise the samples and read the source code and config files via the Sample
Application Launcher:

Unix/Linux:
   ./tools/sessions/samples.sh
Windows:
   tools\sessions\samples.bat



Tools
--------------------

The "bin" directory contains a script to start the Terracotta Developer Console.
Launch the Console with the following command:

Unix/Linux:
   ./dev-console.sh

Windows:
   dev-console.bat

The Console provides a view inside the Terracotta Server, letting you see
activity inside Terracotta at run-time.


Terracotta Forge
--------------------
Terracotta has integration modules (TIMs) that enable integration with many common frameworks,
such as Hibernate, EHCache and Quartz. For the latest information please visit:

    - http://www.terracotta.org/web/display/orgsite/Integration+Guides

Projects for the Forge can be found at:
    - http://forge.terracotta.org 

     
Java Virtual Machine
--------------------

Terracotta clusters can be run with any supported JRE.  The product guide
contains a list of supported JREs.  The JRE used by Terracotta Clients need not
match the one used by the Terracotta Server. Control the JVM that the Terracotta
uses by setting the JAVA_HOME environment variable.

----
Copyright (c) 2009, Terracotta, Inc.
http://www.terracotta.org
