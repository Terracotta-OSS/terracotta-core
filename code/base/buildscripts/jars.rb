# A JarDirectory object is pointed at a directory, and can return a PathSet
# object that points to all of the JARs contained in that directory.
class JarDirectory
    def initialize(root)
        @root = root.to_s
    end
    
    def to_classpath
        out = PathSet.new
        
        if FileTest.directory?(@root)
            Dir.foreach(@root) do |filename|
                unless filename == '.' || filename == '..'
                    out << FilePath.new(@root, filename).canonicalize.to_s if filename.downcase.ends_with?(".jar")
                end
            end
        end
        
        out
    end
end