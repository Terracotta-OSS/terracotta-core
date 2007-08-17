#
# All content copyright (c) 2003-2006 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

class BaseCodeTerracottaBuilder <  TerracottaBuilder
  protected

  include MavenConstants

  # Deploy artifacts created by 'dist' to a local or remote Maven repository.
  def postscript(ant, build_environment, product_directory, *args)
    if repo = config_source[MAVEN_REPO_CONFIG_KEY]
      common_command_args = {
        'packaging' => 'jar',
        'generatePom' => 'true',
        'groupId' => 'org.terracotta'
      }

      command = ['mvn']

      if repo.downcase == MAVEN_REPO_LOCAL
        command << 'install:install-file'
      else
        command << 'deploy:deploy-file'
        common_command_args['url'] = repo
      end

      puts("Deploying '#{@product_code}' artifacts to Maven repository at #{repo}")
      args.each do |arg|
        next unless arg
        artifact = arg['artifact']
        file = FilePath.new(product_directory, interpolate(arg['file'])).to_s
        unless File.exist?(file)
          fail("Bad 'file' argument passed to maven-deploy.  File does not exist: #{file}")
        end

        command_args = {
          'artifactId' => artifact,
          'file' => file,
          'version' => arg['version'] || build_environment.version
        }.merge(common_command_args)

        full_command = command + command_args.map { |key, val| "-D#{key}=#{val}" }

        unless system(*full_command)
          fail("Maven deploy for artifact #{artifact} failed.")
        end
      end
    end
  end
end