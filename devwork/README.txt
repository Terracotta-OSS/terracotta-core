- Setup

  % cd trunk/community/devwork
  % ./setup.sh

- Build everything

  % cd ..
  % mvn clean install -DskipTests -DskipJavadoc

- Build the Terracotta EE kit

  % cd terracotta-kit
  % mvn clean install -P enterprise,kit

- Daily Exercise

  % cd trunk
  % svn up
  % cd community/devwork
  % svn up *
  % cd ..
  % mvn clean install -DskipTests -DskipJavadoc
  % cd terracotta-kit
  % mvn clean install -P enterprise,kit

  The kit will be in the target directory.

- Copy a license file into the root of the kit

  cp trunk/ent-system-tests/src/test/resources/terracotta-license.key target/terracotta*
