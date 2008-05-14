# Utility class for deploying JAR files to a Maven repository.
class MavenDeploy
  include MavenConstants

  def initialize(options = {})
    @packaging = options[:packaging] || 'jar'
    @generate_pom = options.boolean(:generate_pom, true)
    @group_id = options[:group_id] || DEFAULT_GROUP_ID
    @repository_url = options[:repository_url] || MAVEN_REPO_LOCAL
    @repository_id = options[:repository_id]
    @snapshot = options.boolean(:snapshot)
  end

  def deploy_file(file, artifact_id, version, pom_file = nil, dry_run = false)
    unless File.exist?(file)
      raise("Bad 'file' argument passed to deploy_file.  File does not exist: #{file}")
    end

    command = dry_run ? ['echo'] : []
    command << FilePath.new('mvn').batch_extension.to_s << '-B' << '-N'

    if @snapshot
      #version.sub!(/.SNAPSHOT$/, '-SNAPSHOT') || version += '-SNAPSHOT'
      if version !~ /.SNAPSHOT$/
        loud_message("SKIPPING NON-SNAPSHOT ARTIFACT: #{@group_id}.#{artifact_id}-#{version}")
        return
      end
    end

    command_args = {
      'packaging' => @packaging,
      'groupId' => @group_id,
      'artifactId' => artifact_id,
      'file' => file,
      'version' => version,
      'uniqueVersion' => false
    }

    if pom_file
      command_args['pomFile'] = pom_file
    else
      command_args['generatePom'] = @generate_pom
    end

    if @repository_url.downcase == 'local'
      command << 'install:install-file'
    else
      command << 'deploy:deploy-file'
      command_args['url'] = @repository_url
    end

    command_args['repositoryId'] = @repository_id if @repository_id

    full_command = command + command_args.map { |key, val| "-D#{key}=#{val}" }

    puts("Deploying #{File.basename(file)} to Maven repository at #@repository_url")
    unless system(*full_command)
      fail("deployment failed")
    end
  end
end
