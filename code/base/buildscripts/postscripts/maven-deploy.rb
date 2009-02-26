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
          file = FilePath.new(product_directory, interpolate(arg['file']))
        else
          file = FilePath.new(arg['srcfile'])
        end
        
        if arg['inject']
          # Copy jar to tmp jar in same dir
          replacement_file = FilePath.new(file.directoryname) << file.filename + '.tmp'
          @ant.copy(:tofile => replacement_file.to_s, :file => file.to_s)
          file = replacement_file
          
          arg['inject'].each do |inject| 
            # Inject resource into jar
            inject_file = FilePath.new(product_directory, interpolate(inject))
            ant.create_jar(replacement_file,
                         :update => 'true',
                         :basedir => inject_file.directoryname,
                         :includes => inject_file.filename)
            end
        end

        group = arg['groupId']
        artifact = arg['artifact']
        version = arg[MAVEN_VERSION_CONFIG_KEY] || config_source[MAVEN_VERSION_CONFIG_KEY] ||
          config_source['version'] || build_environment.version

        # Allow override of version if a version key is specified.  If so, the value of the key
        # is the property to look up as defined in build-config.global, etc.
        if arg['version']
          versionKey = arg['version']
          version = arg[versionKey] || config_source[versionKey]
        end

        maven.deploy_file(file.to_s, group, artifact, version, arg['pom'])
        
        # clean up injected file if it existed
        if arg['inject'] 
          file.delete
        end
      end
    end
  end
end
