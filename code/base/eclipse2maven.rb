require 'rexml/document'
require 'set'

SKIP_ARTIFACT_LIST = [
	'common',
]

ART_2_GRP = {
	'treemap-tc'=>'org.terracotta',
	'jfreechart'=>'jfree',
	'jmxremote'=>'jmxremote',
	'jmock-cglib'=>'jmock',
}

POM_DEF = <<POM
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <parent>
    <groupId>org.terracotta</groupId>
    <artifactId>parent</artifactId>
    <version>3.6.0-SNAPSHOT</version>
  </parent>
  <artifactId>${artifactId}</artifactId>
  ${dependencies}
</project>
POM

class Project
	attr_reader :groupId, :artifactId, :deps
	def initialize( groupId, artifactId )
		@groupId, @artifactId = groupId, artifactId
		@deps = Set.new
	end
	def add( dep )
		@deps.add( dep )
	end
	def generate_pom()
		pom = POM_DEF.sub( /\$\{artifactId\}/, @artifactId )
		pom = pom.sub( /\$\{dependencies\}/, generate_deps() )
		return pom
	end
	def generate_deps()
		unless @deps.nil? or @deps.empty?
			dep_str = "<dependencies>\n"
			@deps.each { |dep|
				dep_str += dep.to_s_versionless
			}
			dep_str += "  </dependencies>"
		else
			return ""
		end
	end
end

class Dependency
	attr_reader :groupId, :artifactId, :version
	def initialize( groupId, artifactId, version )
		@groupId = groupId
		@groupId = ART_2_GRP[ artifactId ] if @groupId.nil?
		@groupId = artifactId if @groupId.nil?
		@artifactId, @version = artifactId, version
		@version = "${tcVersion}" if version == ""
	end
	def to_s
		str = "      <dependency>\n"
		str += "        <groupId>#{@groupId.to_s}</groupId>\n"
		str += "        <artifactId>#{@artifactId.to_s}</artifactId>\n"
		str += "        <version>#{@version.to_s}</version>\n"
		str += "      </dependency>\n"
	end
	def to_s_versionless
		str = "      <dependency>\n"
		str += "        <groupId>#{@groupId.to_s}</groupId>\n"
		str += "        <artifactId>#{@artifactId.to_s}</artifactId>\n"
		str += "      </dependency>\n"
	end
end

def find_projects( proj_dir_set, dir_name )
	# check if contains a .project and .classpath
	if File.exist?( "#{dir_name}/.classpath" ) and File.exist?( "#{dir_name}/.project" )
		proj_dir_set.add( dir_name )
	else
		Dir.foreach( dir_name ) { |current_dir_name|
			next if current_dir_name =~ /^\.(\.)?$/
			current_dir_name = "#{dir_name}/#{current_dir_name}"
			next unless File.directory?( current_dir_name )
			find_projects( proj_dir_set, current_dir_name )
		}
	end
	return proj_dir_set
end

begin
	proj_dir_set = find_projects( Set.new, File.expand_path( ARGV.length < 1 ? "." : ARGV[0] ) )

	all_deps = Array.new

	proj_dir_set.each { |project_dir|
		File.open( "#{project_dir}/.classpath" ) { |file|
			project_name = project_dir[ project_dir.rindex('/')+1, project_dir.length-1 ]

			project = Project.new( "org.terracotta", project_name )

			doc = REXML::Document.new( file )
			doc.root.each_element( "classpathentry" ) { |kind|
				# libs
				if kind.attributes['kind'] == 'lib'
					path = kind.attributes['path']
					unless path.nil?
						path = path.to_s
						path = path[ path.rindex('/')+1, path.length-1 ]
						version = ""
						artifactId = path.scan( /(.*?)\-\d.*?$/ ).join.to_s
						version = path.scan( /.*?\-(\d.*?)\.jar$/ ).join.to_s
						unless version.nil? or version.length == 0
							dep = Dependency.new( nil, artifactId, version )
							project.add dep
							all_deps.push dep.to_s
						end
					end
				end
				# other projects
				if kind.attributes['kind'] == 'src'
					path = kind.attributes['path']
					unless path.nil?
						path = path.to_s
						if path[0] == 47
							path = path[ path.rindex('/')+1, path.length-1 ].to_s
							dep = Dependency.new( "org.terracotta", path, "" )
							project.add dep
							all_deps.push dep.to_s
						end
					end
				end
				all_deps.push Dependency.new( "org.terracotta", project_name, "" ).to_s
			}

			#write out to file...
			next if SKIP_ARTIFACT_LIST.include?( project_name )
#			File.delete( "#{project_dir}/pom.xml" )
			File.open( "#{project_dir}/pom.xml", File::CREAT|File::RDWR|File::TRUNC ) { |pom_file|
				pom_file.write( project.generate_pom )
			}
		}
	}

	all_deps.uniq!
	all_deps.sort!

	puts "      <!-- parent pom dep list -->"
	all_deps.each { |dep_str|
		puts dep_str
	}
end
