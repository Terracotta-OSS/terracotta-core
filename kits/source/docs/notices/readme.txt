Terracotta
==========

Documentation: http://docs.terracotta.org
Forums:        http://www.terracottatech.com/forums/
Mailing Lists: http://lists.terracotta.org
Services:      http://www.terracottatech.com/support_services.shtml


Getting Started
---------------

Consult the appropriate Terracotta product guide for detailed installation
instructions.  Below are abbreviated install steps for each product.

Terracotta Sessions
   Unix/Linux:
      - Unzip/untar the Terracotta software.
      - cd into the "sessions/bin" directory.
      - Launch the Welcome tool by entering the command:
           ./tc-welcome.sh
   Windows:
      - Run the Terracotta installer.
      - Launch the Welcome tool from the start menu:
           Programs --> Terracotta --> Sessions --> Terracotta Sessions Welcome

Terracotta for Spring
   Unix/Linux:
      - Unzip/untar the Terracotta software.
      - cd into the "spring/bin" directory.
      - Launch the Welcome tool by entering the command:
           ./tc-welcome.sh
   Windows:
      - Run the Terracotta installer.
      - Launch the Welcome tool from the start menu:
           Programs --> Terracotta --> Spring --> Terracotta for Spring Welcome

Terracotta DSO
   Unix/Linux:
      - Unzip/untar the Terracotta software.
      - cd into the "dso/bin" directory.
      - Launch the Welcome tool by entering the command:
           ./tc-welcome.sh
   Windows:
      - Run the Terracotta installer.
      - Launch the Welcome tool from the start menu:
           Programs --> Terracotta --> DSO --> Terracotta DSO Welcome

Terracotta DSO Eclipse Plug-in
   Follow the installation directions at:
      - http://www.terracotta.org/eclipse


Configuration File Samples
--------------------------

The "config-sample" directory for each product contains a sample Terracotta
configuration file with example settings appropriate for a typical deployment.
The directory also contains a reference configuration file for a complete
explanation of the options available.


Sample Applications
-------------------

Terracotta products ship with sample applications to show by example how to
use Terracotta to cluster the JVM.  The sample applications for Terracotta
Sessions are available from links in Configurator.  For Terracotta for Spring
and Terracotta DSO, the sample applications are in the "samples" directory.


Administration Tools
--------------------

The "bin" directory for each product contains a script to start the Terracotta
Administration Console.  Launch the Console tool with the following command.

Unix/Linx:
   ./tc-admin.sh

Windows:
   tc-admin.bat

The Console tool provides a view inside the Terracotta Server, letting you see
activity inside Terracotta at run-time.


Java Virtual Machine
--------------------

The Terracotta Server runs with the Java Runtime Environment (JRE) that ships
with Terracotta.

Terracotta Clients can be run with any supported JRE.  The product guide
contains a list of supported JREs.  The JRE used by Terracotta Clients need not
match the one used by the Terracotta Server.

It is possible to control the JVM that the Terracotta Server uses by
setting the TC_JAVA_HOME environment variable.  This should be used with
caution, as doing so may cause the Terracotta Server to run with an unsupported
JVM.


----
Copyright (c) 2006, Terracotta, Inc.
http://www.terracotta.org
