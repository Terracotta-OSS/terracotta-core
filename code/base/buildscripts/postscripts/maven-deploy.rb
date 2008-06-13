#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  include MavenConstants

  # Deploy artifacts created by 'dist' to a local or remote Maven repository.
  def postscript(ant, build_environment, product_directory, *args)
    if repo = config_source[MAVEN_REPO_CONFIG_KEY]
      maven = MavenDeploy.new(:repository_url => repo,
        :repository_id => config_source[MAVEN_REPO_ID_CONFIG_KEY],
        :snapshot => config_source[MAVEN_SNAPSHOT_CONFIG_KEY])
      args.each do |arg|
        next unless arg
        if arg['file']
          file = FilePath.new(product_directory, interpolate(arg['file'])).to_s
        else
          file = FilePath.new(arg['srcfile']).to_s
        end

        artifact = arg['artifact']
        version = arg[MAVEN_VERSION_CONFIG_KEY] || config_source[MAVEN_VERSION_CONFIG_KEY] ||
          config_source['version'] || build_environment.version
        maven.deploy_file(file, artifact, version, arg['pom'])
      end
    end
  end
end
