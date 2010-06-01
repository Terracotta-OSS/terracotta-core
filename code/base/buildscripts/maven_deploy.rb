# Utility class for deploying JAR files to a Maven repository.
class MavenDeploy
  include MavenConstants

  def initialize(options = {})
    @generate_pom = options.boolean(:generate_pom, true)
    @repository_url = options[:repository_url] || MAVEN_REPO_LOCAL
    @repository_id = options[:repository_id]
    @snapshot = options.boolean(:snapshot)
  end

  def deploy_file(file, group_id, artifact_id, classifier, version, pom_file = nil, packaging = nil, dry_run = false)
    unless File.exist?(file)
      raise("Bad 'file' argument passed to deploy_file.  File does not exist: #{file}")
    end

    packaging ||= case file
    when /pom.*\.xml$/: 'pom'
    when /\.jar/: 'jar'
    else
      File.extname(file)[1..-1]
    end

    command = dry_run ? ['echo'] : []
    command << FilePath.new('mvn').batch_extension.to_s << '-B' << '-N'

    if @snapshot
      #version.sub!(/.SNAPSHOT$/, '-SNAPSHOT') || version += '-SNAPSHOT'
      if version !~ /.SNAPSHOT$/
        loud_message("SKIPPING NON-SNAPSHOT ARTIFACT: #{group_id}.#{artifact_id}-#{version}")
        return
      end
    end

    command_args = {
      'packaging' => packaging,
      'groupId' => group_id,
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

    if classifier
      command_args['classifier'] = classifier
    end
    
    if @repository_url.downcase == 'local'
      command << 'install:install-file'
    else
      command << 'deploy:deploy-file'
      command_args['url'] = @repository_url
    end

    command_args['repositoryId'] = @repository_id if @repository_id

    full_command = command + command_args.map { |key, val| "-D#{key}=#{val}" }

    if ENV['OS'] =~ /Windows/i
      full_command = ['cmd.exe', '/C'] +  full_command
    end

    puts("Deploying #{File.basename(file)} to Maven repository at #@repository_url")
    puts "Cmd: #{full_command.join(' ')}"
    
    unless system(*full_command)
      fail("deployment failed")
    end
  end
end
