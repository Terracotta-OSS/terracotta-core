#
# All content copyright (c) 2003-2008 Terracotta, Inc.,
# except as may otherwise be noted in a separate copyright notice.
# All rights reserved
#

# Contains classes that let you represent file paths and sets of paths (think
# CLASSPATH) as objects which expose platform-neutral methods to operate on
# them.
# ---
# <b>THIS CODE MUST RUN IN BOTH RUBY AND JRUBY.</b> It is used by both the
# buildsystem and the monkey-generation logic. Be extremely careful to test
# it in both and to not write code that won't work on one or the other.

# We depend on certain other files, but have to use a cross-platform-safe way
# of including them -- hence all this magic.
our_directory = File.dirname(__FILE__)
our_directory = "." if our_directory.nil? || our_directory.strip.length == 0

require "%s/cross_platform" % our_directory
require "%s/additions" % our_directory
require 'utils.rb'

# This class represents a path on the filesystem. It provides operations that
# let you do things with that path in a platform-independent manner. For example,
# Ruby uses strings to represent file paths, but writing code to determine things
# like "is this an absolute pathname?" is very difficult to do with strings in a
# platform-independent manner. If you use instances of FilePath instead, you'll
# be able to perform these operations without worrying about which platform you're
# on.
#
# Be sure you call #to_s on an object that might be a FilePath before you pass it
# to any Ant tasks or any other code that may call directly into Java (from JRuby,
# that is). As of this writing, you get nasty Java stack traces (that omit all
# JRuby filenames and line numbers) otherwise, and these aren't very fun or easy
# to track down.
#
# Feel free to add methods to this class to do anything else you might need. It's
# not meant to be complete in general; it's meant to be complete for what we need,
# right now.
class FilePath

  # Get our directory separators in different ways: if JRuby, from java.io.File;
  # if native Ruby, from various Ruby properties.
  if CrossPlatform::is_jruby?
    require 'java'

    # The native directory separator ('/' on Unix, '\' on Windows), if we're
    # running on JRuby.
    NATIVE_DIRECTORY_SEPARATOR = JavaFile.separator

    if JavaSystem.getProperty('os.name').downcase =~ /^.*windows.*$/i
      # The extension for executable files; this variant is for Windows
      EXECUTABLE_EXTENSION = '.exe'
      BATCH_EXTENSION = '.bat'
      SCRIPT_EXTENSION = '.bat'
    else
      # The extension for executable files; this variant is for Unix
      EXECUTABLE_EXTENSION = ''
      BATCH_EXTENSION = ''
      SCRIPT_EXTENSION = '.sh'
    end
  else
    if ENV['OS'] =~ /windows/i
      # The native directory separator; this variant is for Windows, normal Ruby
      NATIVE_DIRECTORY_SEPARATOR = '\\'

      # The extension for executable files; this variant is for Windows
      EXECUTABLE_EXTENSION = '.exe'
      BATCH_EXTENSION = '.bat'
      SCRIPT_EXTENSION = '.bat'
    else
      # The native directory separator; this variant is for Unix, normal Ruby
      NATIVE_DIRECTORY_SEPARATOR = File::SEPARATOR

      # The extension for executable files; this variant is for Unix
      EXECUTABLE_EXTENSION = ''
      BATCH_EXTENSION = ''
      SCRIPT_EXTENSION = '.sh'
    end
  end

  # Should absolute paths be prefixed with our directory separator prefix
  # (like on Unix, where absolute paths start with '/'), or not (like on Windows,
  # where you just start with the drive letter?).
  ABSOLUTE_GETS_NATIVE_DIRECTORY_SEPARATOR_PREFIX=(NATIVE_DIRECTORY_SEPARATOR != '\\')

  # Creates a new FilePath. You can pass any number of strings, FilePath objects, or
  # any other objects that respond to #to_s with a string representing a pathname or
  # pathname component; they'll all be aggregated together into a single pathname.
  # Also, any duplicated path separators are automatically compressed into a single
  # separator, and empty components are removed. For example, you can do
  #
  #     FilePath.new('/', '//', 'foo/bar', 'baz/', '/quux//', '', '/marph')
  #
  # and nicely end up with '/foo/bar/baz/quux/marph' when you call #to_s on the
  # resulting object.
  def initialize(*parts)
    @parts = [ ]
    @seen_one = false
    @is_absolute = false

    self.<<(*parts)
  end

  # Does this FilePath refer to the same path as other?
  def ==(other)
    self.canonicalize.to_s == other.canonicalize.to_s
  end

  # Appends additional components to this FilePath. This follows the same rules as the
  # constructor, meaning you can pass in all kinds of garbage and it will, generally
  # speaking, clean it up for you.
  def <<(*parts)
    parts.each do |part|
      part = part.to_s
      unless part.blank?
        @is_absolute = true if (! @seen_one) && is_absolute_part(part)
        @seen_one = true

        this_parts = part.split(/[\/\\]+/)
        this_parts.each do |this_part|
          unless this_part.blank?
            @parts << this_part
          end
        end
      end
    end

    self
  end

  # Returns this path as a string escaped properly for embedding in Java property
  # files. (See java.util.Properties.load().)
  def to_propertyfile_escaped_s
    to_s.to_propertyfile_escaped_s
  end

  # The filename of this path -- that is, just the last component.
  def filename
    @parts[-1]
  end

  # The directory name of this path -- that is, everything but the last component.
  def directoryname
    new_parts = @parts[0..-2]
    new_parts[0] = absolute_prefix + new_parts[0]
    FilePath.new(*new_parts).to_s
  end

  # Returns the contents of the file named by this path, as a string.
  def suck_file
    if FileTest.file?(to_s)
      out = ""
      File.open(to_s, "r") do |file|
        file.each { |line| out << line << "\n" }
      end

      out
    else
      nil
    end
  end

  # Does a file, directory, or other entity (e.g., a named pipe) exist at this path?
  def exist?
    FileTest.exist?(to_s)
  end

  # Does this path denote a directory that exists?
  def directory?
    FileTest.directory?(to_s)
  end

  # Does this path denote a directory that exists, and is empty (contains no files or
  # directories)?
  def empty_directory?
    directory? && Dir.entries(to_s).reject { |entry| entry == '.' || entry == '..' }.empty?
  end

  # What's the relative path to this file (or directory), starting from the other file (or directory)?
  def relative_path_from(other_path)
    out = [ ]

    matching_prefix = true
    0.upto(@parts.length - 1) do |index|
      matching_prefix = false unless @parts[index] == other_path.part(index)
      out << @parts[index] unless matching_prefix
    end

    FilePath.new(*out)
  end

  # Deletes this file or directory.
  def delete
    FileUtils.rm_rf(to_s)
    self
  end

  # Create this path as a directory (unless it already is one), including any leading directories.
  def ensure_directory
    FileUtils.mkdir_p(to_s)
    self
  end

  # This works differently depending on whether you're in JRuby or Ruby...
  if CrossPlatform::is_jruby?
    # (JRuby version): Returns a FilePath that is a canonicalized version of this pathname:
    # the result will always be absolute, have directory components like '.' or '..' removed,
    # contain no symlinks, and so on.
    def canonicalize
      FilePath.new(JavaFile.new(to_s).getCanonicalPath)
    end

    # Use a java.io.File object to convert the path into a URL
    include_class('java.io.File') { 'JavaFile' }

    # Returns a String that is the URL form of this FilePath.
    def to_url
      JavaFile.new(self.canonicalize.to_s).toURL().to_s
    end
  else
    # (Ruby version): Returns a FilePath that is a canonicalized version of this pathname:
    # the result will always be absolute, have directory components like '.' or '..' removed,
    # contain no symlinks, and so on.
    def canonicalize
      FilePath.new(File.expand_path(to_s))
    end
  end

  # Is this an absolute path?
  def absolute?
    @is_absolute
  end

  # Returns a version of this FilePath object with the executable extension appended. This does
  # nothing on Unix, but adds '.exe' on Windows.
  def executable_extension
    dup.add_extension(EXECUTABLE_EXTENSION)
  end

  # Returns a version of this FilePath object with the script extension appended.
  # On windows, the script extension is '.bat', while on other platforms it is '.sh'
  def script_extension
    dup.add_extension(SCRIPT_EXTENSION)
  end

  # Returns a version of this FilePath object with the '.bat' extensions
  # appended if and only if on Windows.
  def batch_extension
    dup.add_extension(BATCH_EXTENSION)
  end

  # Turns this FilePath into a string representation of itself. This is how you end up using a
  # FilePath for opening files, copying them, and so on.
  def to_s
    out = absolute_prefix
    out += @parts.join(NATIVE_DIRECTORY_SEPARATOR)
    out
  end

  protected
  # Returns the given component of this path; just like Ruby arrays, 0 is the first
  # element, 1 is the second, -1 is the last, -2 is the next-to-last, and so on.
  def part(index)
    @parts[index]
  end

  # Returns this path with the given extension appended.
  def add_extension(extension)
    @parts[-1] += extension
    self
  end

  private
  # Does the given part of a pathname designate a filesystem root? On Unix, this means
  # anything that starts with a slash; on Windows, it means anything that starts with
  # [A-Za-z]:/.
  def is_absolute_part(part)
    part.strip.starts_with?('/') || part.strip.starts_with?('\\') ||
      part.strip =~ /^[A-Za-z]:([\/\\])?/
  end

  # Add the 'absolute path' prefix to this path, if required. This is the leading slash ('/')
  # on Unix, and nothing on Windows. (Windows absolute paths already contain the drive letter
  # as their first component.)
  def absolute_prefix
    out = ""
    out = NATIVE_DIRECTORY_SEPARATOR if ABSOLUTE_GETS_NATIVE_DIRECTORY_SEPARATOR_PREFIX && absolute?
    out
  end

  # Returns a new FilePath object, containing the same path as this one but with count trailing
  # components removed.
  def strip_trailing_components(count)
    new_parts = case
    when @parts.length > 1 then @parts[0..(@parts.length - (count + 1))]
    when @parts.length == 1 then [ "" ]
    end

    new_parts[0] = absolute_prefix + new_parts[0]
    FilePath.new(*new_parts)
  end
end

# Represents an ordered list of paths on the filesystem -- think CLASSPATH. Provides
# platform-independent methods for getting at the individual paths contained within,
# as well as for turning the entire thing into a string formatted correctly for your
# platform.
class PathSet
  include Enumerable
  attr_reader :parts
  
  # The separator for paths on your platform. That is, ':' for Unix, ';' for Windows.
  NATIVE_PATH_SEPARATOR=File::PATH_SEPARATOR

  if NATIVE_PATH_SEPARATOR == ";"
    # The regular expression to split incoming paths on if we're on Windows. We can only safely
    # split on semicolons, since we'll have colons as drive-letter separators in
    # the path itself.
    SPLIT_REGEX=/;+/
  else
    # The regular expression to split incoming paths on if we're on Unix. We can split
    # on semicolons and colons; this makes us more likely to tolerate odd input, at
    # the cost of preventing people from having directory or file names with a
    # semicolon in them. (But that's pretty much a uniformly bad idea anyway.)
    SPLIT_REGEX=/[:;]+/
  end

  # Creates a new PathSet. You can pass any number of input arguments, each of which
  # can be any object that responds to #to_s with a string representing a filesystem
  # path, or set of filesystem paths separated by the correct separator. The constructor
  # will pull these all apart and glue them back together properly, omitting any
  # consecutive separators or empty components. For example, you can do
  #
  #     PathSet.new('/foo/bar:/baz:quux::', '', 'aaa', ':b::c::', "::', '/d/e:')
  #
  # and nicely end up with /foo/bar:/baz:quux:aaa:b:c:/d/e when you call #to_s on the
  # resulting PathSet.
  def initialize(*parts)
    @parts = [ ]
    self.<<(*parts)
  end

  # Runs a block on each path in the PathSet, in order.
  def each(&proc)
    @parts.each(&proc)
  end

  # Are there no actual paths in this PathSet?
  def empty?
    @parts.empty?
  end

  # How many paths are in this PathSet?
  def size
    @parts.size
  end

  # Adds new paths to this PathSet. Arguments are processed exactly as for the
  # constructor, so you can pass in all kinds of garbage and it'll get cleaned up
  # properly.
  def <<(*parts)
=begin
      parts.each do |part|
        part = part.to_s
        unless part.blank?
          this_parts = part.split(SPLIT_REGEX)
          this_parts.each do |this_part|
            unless this_part.blank?
              canonicalized = FilePath.new(this_part).canonicalize.to_s
              unless @set.include?(canonicalized)
                @parts << this_part
                @set << canonicalized
              end
            end
          end
        end
      end
=end
    parts.each do |part|
      part = part.to_s
      unless part.blank?
        this_parts = part.split(SPLIT_REGEX)
        this_parts.each do |this_part|
          unless this_part.blank?
            @parts << this_part
          end
        end
      end
    end
  end

  # Turns this PathSet into the correct string representation for this platform.
  def to_s
    @parts.join(NATIVE_PATH_SEPARATOR)
  end

  def append(otherPathSet)
    self.<< otherPathSet.parts
    self
  end
  
  def prepend(otherPathSet)
    @parts.insert(0, otherPathSet.parts)
    self
  end
  # Turns this PathSet into the correct string representation for this platform,
  # further escaping it according to the rules necessary for it to work correctly
  # with java.util.Properties.load().
  def to_propertyfile_escaped_s
    to_s.to_propertyfile_escaped_s
  end
end
