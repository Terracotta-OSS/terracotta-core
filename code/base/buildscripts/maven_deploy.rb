# Utility class for deploying JAR files to a Maven repository.
class MavenDeploy
  include MavenConstants

  def initialize(repository_url = 'local', group_id = DEFAULT_GROUP_ID)
    @packaging = 'jar'
    @generate_pom = true
    @group_id = group_id
    @repository_url = repository_url
  end

  def deploy_file(file, artifact_id, version, dry_run = false)
    unless File.exist?(file)
      raise("Bad 'file' argument passed to deploy_file.  File does not exist: #{file}")
    end

    command = dry_run ? ['echo'] : []
    command << FilePath.new('mvn').batch_extension.to_s

    command_args = {
      'packaging' => @packaging,
      'generatePom' => @generate_pom,
      'groupId' => @group_id,
      'artifactId' => artifact_id,
      'file' => file,
      'version' => version
    }

    if @repository_url.downcase == 'local'
      command << 'install:install-file'
    else
      command << 'deploy:deploy-file'
      command_args['url'] = @repository_url
    end

    full_command = command + command_args.map { |key, val| "-D#{key}=#{val}" }

    puts("Deploying #{File.basename(file)} to Maven repository at #@repository_url")
    Registry[:platform].exec(full_command.first, *full_command[1..-1])
  end
end
