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
  MODULES_URL = 'com.tc.l1.modules.repositories'

  TC_BASE_DIR = 'tc.base-dir'

  ENTERPRISE = 'ENTERPRISE'
  OPENSOURCE = 'OPENSOURCE'
end

module MavenConstants
  TIM_API_VERSION_CONFIG_KEY = 'tim-api.version'
  MAVEN_REPO_CONFIG_KEY = 'maven.repo'
  MAVEN_REPO_ID_CONFIG_KEY = 'maven.repositoryId'
  MAVEN_VERSION_CONFIG_KEY = 'maven.version'
  MAVEN_SNAPSHOT_CONFIG_KEY = 'maven.snapshot'
  MAVEN_CLASSIFIER_CONFIG_KEY = 'maven.classifier'
  MAVEN_REPO_LOCAL = 'local'
  DEFAULT_GROUP_ID = 'org.terracotta'
  MODULES_GROUP_ID = 'org.terracotta.modules'
  MAVEN_USE_LOCAL_REPO_KEY = 'maven.useLocalRepo'

  TERRACOTTA_SNAPSHOTS_REPO_ID = 'terracotta-snapshots'
  TERRACOTTA_STAGING_REPO_ID = 'terracotta-staging'
  TERRACOTTA_RELEASES_REPO_ID = 'terracotta-releases'
  TERRACOTTA_PATCHES_REPO_ID = 'terracotta-patches'

  TERRACOTTA_EE_SNAPSHOTS_REPO_ID = 'terracotta-ee-snapshots'
  TERRACOTTA_EE_RELEASES_REPO_ID = 'terracotta-ee-releases'

  TERRACOTTA_SNAPSHOTS_REPO = 'http://nexus:8080/content/repositories/terracotta-snapshots'
  TERRACOTTA_STAGING_REPO = 'http://nexus:8080/content/repositories/terracotta-staging'
  TERRACOTTA_RELEASES_REPO = 'http://nexus:8080/content/repositories/terracotta-releases'
  TERRACOTTA_PATCHES_REPO = 'http://nexus:8080/content/repositories/terracotta-patches'

  TERRACOTTA_EE_SNAPSHOTS_REPO = 'http://nexus:8080/content/repositories/terracotta-ee-snapshots'
  TERRACOTTA_EE_RELEASES_REPO = 'http://nexus:8080/content/repositories/terracotta-ee-releases'
end
