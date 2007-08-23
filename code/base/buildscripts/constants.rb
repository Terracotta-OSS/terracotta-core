module PropertyNames
  # The prefix that we use for dynamically-generated properties -- that is,
  # those that are created by the buildsystem out of its own internal
  # information, and not simply information manually specified by the user and
  # merely ferried across.
  DYNAMICALLY_GENERATED_PROPERTIES_PREFIX = 'tc.tests.info.'

  # The prefix that we use for statically-generated properties -- that is, those
  # that are specified by the user (or monkey, or configuration file) and simply
  # ferried across to the tests.
  STATIC_PROPERTIES_PREFIX = 'tc.tests.configuration.'

  # The name of the system property that contains the URL for modules.
  MODULES_URL = STATIC_PROPERTIES_PREFIX + 'modules.url'

  TC_BASE_DIR = 'tc.base-dir'
end

module MavenConstants
  MAVEN_REPO_CONFIG_KEY = 'maven.repo'
  MAVEN_REPO_ID_CONFIG_KEY = 'maven.repositoryId'
  MAVEN_VERSION_CONFIG_KEY = 'maven.version'
  MAVEN_SNAPSHOT_CONFIG_KEY = 'maven.snapshot'
  MAVEN_REPO_LOCAL = 'local'
  DEFAULT_GROUP_ID = 'org.terracotta'
  MODULES_GROUP_ID = 'org.terracotta.modules'
end
