#!/usr/bin/env ruby
#--
# Copyright 2004 by Jim Weirich (jim@weirichhouse.org).
# Copyright 2005 by Tim Azzopardi (tim@tigerfive.com).
# All rights reserved.

# Permission is granted for use, copying, modification, distribution,
# and distribution of modified versions of this work as long as the
# above copyright notice is included.
#++
require 'builder/xmlbase'

require 'java'
include_class 'org.leafcutter.core.TaskRunner'

module Builder
  
  class AntBuilder < XmlBase
    
    # Create an ant markup builder.  Parameters are specified by an
    # option hash.
    #
    # :target=><em>target_object</em>::
    #    Object receiving the markup.  +out+ must respond to the
    #    <tt><<</tt> operator.  The default is a plain string target.
    # :indent=><em>indentation</em>::
    #    Number of spaces used for indentation.  The default is no
    #    indentation and no line breaks.
    # :margin=><em>initial_indentation_level</em>::
    #    Amount of initial indentation (specified in levels, not
    #    spaces).
    #    
    def initialize(options={})
      indent = options[:indent] || 0
      margin = options[:margin] || 0
      super(indent, margin)
      @target = options[:target] || ""
      @debug = options[:debug] 
      @debug = false if @debug.nil?
    end
    
    # Return the target of the builder.
    def target!
      @target
    end
    
    def _check_for_circular_dependencies(call_stack, some_method_name) 
      # remove noise from the stack - all we want left are the ant task methods
      method_name_on_stack_regexp = /`\w+'/ 
      call_stack.reject!{|c| c =~ /#{__FILE__}/ or c=~ /`instance_eval'/ or c=~ /`each'/ or not c=~ method_name_on_stack_regexp }
      
      # collect just those lines that look like method names
      call_stack.collect!{|c| (method_name_on_stack_regexp.match(c))[0] }
      
      # check whether some_method_name is on the stack
      call_stack.each do |c|
          if c=~ /`#{some_method_name}'/
            # circular dependecy detected 
            err = call_stack.inject(some_method_name){|message,method_name|  "#{message} <- #{method_name}"}
            raise RuntimeError, "\nBUILD FAILED\nCircular dependency: #{err}", []
          end
      end
    end

    # implement the ant depends clause    
    def depends(*ant_task_dependecies) 
  
      # who called me?
      my_caller_matcher = caller[0].match(/`(\w+)'/)
      my_caller = my_caller_matcher[1] if not my_caller_matcher.nil?

      # Call the dependencies if they haven't been called already
      for ant_task_dependency in ant_task_dependecies
      
            if not respond_to?(ant_task_dependency) 
              raise RuntimeError, "\nBUILD FAILED\nTarget '#{ant_task_dependency}' does not exist in this project. It is used from target `#{my_caller}'.", []
            end
            _check_for_circular_dependencies(caller, ant_task_dependency.to_s) 
            
            cmd = "puts \"\n#{ant_task_dependency}:\" if  not @__#{ant_task_dependency}__.nil?; @__#{ant_task_dependency}__ ||= #{ant_task_dependency} || true"
            puts "#{cmd}" if @debug 
            instance_eval(cmd)
      end
      
      # Now print the name of the calling task, from the first line of stack
      puts
      puts "#{my_caller}:"
      
      # and make sure we also record the fact that the caller has been called (as we have done for its dependencies)
      instance_eval("@__#{my_caller}__ = true ")

    end    

    # Subclasses ONLY can use this at the end of their class definition to run targets from the command line like ant does.
    # For example: 
    # "jruby build.rb clean compile release" is like saying "ant clean compile release # this is any comment"
    def AntBuilder.run_targets_from_command_line
      # self would be expected to be a subclass here, because AntBuilder doesnt define any 'ant_target'
      builder = self.new
      ARGV.each do |ant_target| 
        return if ant_target == "#"
        if builder.respond_to?(ant_target)
          builder.instance_eval("builder.#{ant_target}") 
        else
          raise RuntimeError, "\nBUILD FAILED\nTarget '#{ant_target}' does not exist in this project.", []
        end
      end
    end
    
    private

    ########################################################################  
    # NOTE: All private methods of a builder object are prefixed when
    # a "_" character to avoid possible conflict with ant tags.
    ########################################################################
  
    def _execute ()
      # Remove the outer brackets as requied by the leafcutter syntax
      cmd = @target.slice(1,@target.size-2)
      puts __FILE__ << " _execute " << cmd if @debug
      case cmd
      when  /^([\w]+) id=([\w.]+)(\()/
        # leafcutter supports ant references explicitly http://ant.apache.org/manual/using.html#references
        # An ant reference of form for example path id=project.class.path
        reference = $1 +$3 + $'
        reference_name = $2
        puts __FILE__ << " _execute " << "TaskRunner.addReference(\"#{reference_name}\",\"#{reference}\");" if@debug
        TaskRunner.addReference(reference_name, reference);    
      when /taskdef .* name=(\w+)/
        name=$1
        # match on classname
        cmd =~ /classname=([\w.]+)/
        classname = $1
        puts __FILE__ << " _execute " << "TaskRunner.addTaskDef(\"#{name}\",\"#{classname}\");" if@debug
        TaskRunner.addTaskDef(name, classname)
      else
        # The "normal" case of some ant command
        
        # get the ant properties before the ant command
        ant_properties_before = _get_ant_properties_as_hash
        
        begin
          puts __FILE__ << " _execute " << "TaskRunner.run(\"#{cmd}\");" if@debug
          TaskRunner.run(cmd);    
        rescue
          raise RuntimeError, "BUILD FAILED\n#{$!}", []
        end
        
        # get the ant properties after the ant command
        ant_properties_after = _get_ant_properties_as_hash

        # get the new ant properties after the command (if any). 
        # (Remember ant properties can't be redefined so we only need the new ones.)
        ant_properties_new = ant_properties_after.select{|k, v| not ant_properties_before.has_key?(k) }
        
        # define the ant properies as ruby instance variables
        ant_properties_new.each do | k,v | 

          # property name may contain alot of different wacky characters which are not legal in ruby instance variable names
          # so replace all wacky characters with underscores
          k.gsub!(/\W/, "_")

          # Replace \ with \\ : this is hell : some windows env vars can look like "C:\some\windows\dir"
          v.gsub!(/\\/, "\\\\\\\\")
          v.gsub!(/"/, "\\\"")
          decl = "@#{k}=\"#{v}\""
          puts "About to define ruby instance variable: " << decl if @debug 
          instance_eval(decl) 
          puts "Defined ruby instance variable: " << decl << " read back as: " << instance_eval("@#{k}") if @debug 
        end        
        
      end
      
      @target = ''
    end
  
    def _get_ant_properties_as_hash
      hash = Hash.new 
      TaskRunner.getAntProjectProperties.each { | k,v | hash[k] = v }
      hash
    end
  
    # Insert text directly in to the builder's target.
    def _text(text)
      @target << text
    end
    
    def _indent
      raise ArgumentError, "It looks like this object has not been initialized. Did the object get an initialize call?" if @target.nil?
      super
    end
    
    def method_missing(sym, *args, &block)
      ret = super
      _execute if @level==0
      ret
    end
    
    
    # Start an tag.  If <tt>end_too</tt> is true, then the start
    # tag is also the end tag 
    def _start_tag(sym, attrs, end_too=false)
      @target << "(#{sym}"
      _insert_attributes(attrs)
      @target << ")" if end_too
      # @target << ")"
    end
    
    # Insert an ending tag.
    def _end_tag(sym)
      @target << ")"
    end
    
    # Insert the attributes (given in the hash).
    def _insert_attributes(attrs)
      return if attrs.nil?
      attrs.each do |k, v|
        # if the attribute value contains spaces then put single quotes around the string
        v = "'#{v}'" if v =~ " " 
        @target << %{ #{k}=#{v}}
      end
    end
  
  end # class

end # module
