class ExternalResourceResolver
  def initialize(flavor, repositories, use_local_maven_repo = false)
    @flavor = flavor.to_s.downcase
    @repositories = repositories
    @use_local_maven_repo = use_local_maven_repo
    @download_util = DownloadUtil.new(Registry[:static_resources].global_cache_directory)
  end

  def use_local_maven_repo?
    @use_local_maven_repo
  end

  def retrieve(resource, dest_dir)
    dest_dir = File.join(dest_dir, resource['destination'])
    FileUtils.mkdir_p(dest_dir)

    file = get(resource, dest_dir)
    explode(file, dest_dir, resource) if resource['explode'] == true
    post_actions(dest_dir, resource['post_actions']) if resource['post_actions']
  end

  private

  LOCAL_MAVEN_REPO = "#{ENV['HOME']}/.m2/repository"
  
  def post_actions(dest_dir, actions)
    actions.each do |action|
      next unless (action['kit_edition'] == nil) || (action['kit_edition'] == @flavor)
      puts "Running post action: #{action['ant']}"
      if action['ant'] == 'replace'
        arguments = action['arguments']
        arguments[:dir] = dest_dir
        ant.replace(arguments)
      end
    end
  end

  def ant
    Registry[:ant]
  end

  def get(resource, dest_dir)
    success = false
    result = nil

    candidate_urls = if resource['maven_artifact'] == true
      artifact_path = maven_artifact_path(resource)
      repos = (use_local_maven_repo? ? [LOCAL_MAVEN_REPO] : []) + @repositories
      repos.map {|repo| "#{repo}/#{artifact_path}"}
    else
      [resource['url']]
    end

    candidate_urls.each do |url|
      puts "trying #{url}"
      dest_file = File.join(dest_dir, resource['name'] || File.basename(url))
      result = download(url, dest_file)
      if result
        success = true
        break
      end
    end

    raise("Couldn't find artifact #{resource['name'] || resource['artifactId']} on any of the repos") unless success
    result
  end

  def is_local_file?(url)
    url[0] == '/'[0]
  end

  def explode(archive, dest_dir, options = {})
    unless options.has_key?('delete_archive')
      options['delete_archive'] = true
    end
    tmp_dir = File.join("build", "tmp")
    FileUtils.mkdir_p(tmp_dir)
    exploded_dir = File.join(tmp_dir, "exploded")
    FileUtils.mkdir_p(exploded_dir)

    excludes = options['excludes'] || ''
    
    case archive
    when /\.tar\.gz$/
      ant.untar(:src => archive, :dest => exploded_dir, :compression => "gzip")
    when /\.(jar|zip)$/
      ant.unzip(:src => archive, :dest => exploded_dir)
    else
      raise("Don't know how to unpack file #{archive}")
    end

    # recover execution bits
    ant.chmod(:dir => exploded_dir, :perm => "ugo+x",
      :includes => "**/*.sh **/*.bat **/*.exe **/bin/** **/lib/**")
    
    if options['remove_root_folder'] == true
      # assume the zip file contains a root folder
      root_dir = nil
      Dir.new(exploded_dir).each do |e|
        next if e =~ /^\./
        root_dir = File.expand_path(File.join(exploded_dir, e))
      end
      ant.move(:todir => dest_dir) do
        ant.fileset(:dir => root_dir, :excludes => excludes)
      end
    else
      ant.move(:todir => dest_dir) do
        ant.fileset(:dir => exploded_dir, :excludes => excludes)
      end
    end
    FileUtils.rm_rf(tmp_dir)
    FileUtils.remove(archive) if options['delete_archive']
  end
  
  def maven_artifact_path(artifact)
    extension = artifact['type'] || 'jar'
    classifier = artifact['classifier'] ? "-#{artifact['classifier']}" : ''
    filename = "#{artifact['artifactId']}-#{artifact['version']}#{classifier}.#{extension}"
  
    "#{artifact['groupId'].gsub('.', '/')}/#{artifact['artifactId']}/#{artifact['version']}/#{filename}"
  end

  def download(url, dest_file)
    if is_local_file?(url) && File.exist?(url)
      puts("Copying #{url}")
      FileUtils.copy(url, dest_file)
    elsif is_live?(url)
      puts("Fetching #{url}")
      @download_util.get(url, dest_file)
    else
      return nil
    end
    dest_file
  end
end