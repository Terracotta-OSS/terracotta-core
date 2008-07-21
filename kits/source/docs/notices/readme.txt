Terracotta
==========

Documentation: http://www.terracotta.org/kit/reflector?kitID=2.6&pageID=Documentation 
Forums:        http://www.terracottatech.com/forums/
Mailing Lists: http://lists.terracotta.org
Services:      http://www.terracottatech.com/support_services.shtml


Getting Started
---------------

Below are abbreviated install steps for Terracotta.

Terracotta
   Unix/Linux:
      - Unzip/untar the Terracotta software.
      - cd into the the top-level directory.
      - Launch the Welcome tool by entering the command:
           ./welcome.sh
   Windows:
      - Run the Terracotta installer.
      - After a successful installation, the Welcome tool is launched.
      - Alternatively, you can launch the Welcome tool from the start menu:
           Programs --> Terracotta --> Terracotta Pojos Welcome

Terracotta DSO Eclipse Plug-in
   Follow the installation directions at:
      - http://www.terracotta.org/eclipse


Sign Up
--------------
To get the most out of your Terracotta experience, please sign up for a Terracotta account:
    - http://www.terracotta.org/confluence/display/orgsite/Sign+Up

With a Terracotta account, you can:
    - Access all services with a site-wide ID
    - Post questions to the Forums
    - Submit JIRA reports
    - Vote on bugs to be fixed, or features for the next release 
    
    
Configuration File Samples
--------------------------

The tc-config-reference.xml file can be found in docs directory.
This sample Terracotta configuration file with example settings appropriate 
for a typical deployment. The directory also contains a reference configuration 
file for a complete explanation of the options available.


Sample Applications
-------------------

Terracotta ships with sample applications to show by example how to use
Terracotta to cluster the JVM.  The sample applications for Terracotta Sessions
are available from links in Configurator.  For Terracotta for Spring, the sample
applications are in the "samples/spring" directory. For Terracotta Pojos, the
sample applications are in the "samples/pojo" directory.


Administration Tools
--------------------

The "bin" directory contains a script to start the Terracotta Administration 
Console.  Launch the Console tool with the following command.

Unix/Linx:
   ./admin.sh

Windows:
   admin.bat

The Console tool provides a view inside the Terracotta Server, letting you see
activity inside Terracotta at run-time.


Terracotta Forge
--------------------
Terracotta has integration modules (TIMs) that enable integration with many common frameworks,
such as EHCache and Quartz. For the latest information please visit:

    - http://www.terracotta.org/confluence/display/integrations/Home

Projects for the Forge can be found at:
    - http://forge.terracotta.org 

     
Java Virtual Machine (Windows only)
--------------------

The Terracotta Server runs with the Java Runtime Environment (JRE) that ships
with Terracotta.

Terracotta Clients can be run with any supported JRE.  The product guide
contains a list of supported JREs.  The JRE used by Terracotta Clients need not
match the one used by the Terracotta Server.

It is possible to control the JVM that the Terracotta Server uses by
setting the JAVA_HOME environment variable.  This should be used with caution,
as doing so may cause the Terracotta Server to run with an unsupported JVM.


----
Copyright (c) 2008, Terracotta, Inc.
http://www.terracotta.org
