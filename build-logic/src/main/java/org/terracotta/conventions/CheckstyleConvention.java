package org.terracotta.conventions;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.quality.Checkstyle;
import org.gradle.api.plugins.quality.CheckstyleExtension;
import org.gradle.api.plugins.quality.CheckstylePlugin;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.gradle.api.provider.Property;
import org.gradle.api.services.ServiceReference;

import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP;

/**
 * Convention plugin for the Checkstyle analysis tool.
 * This convention:
 * <ul>
 *     <li>establishes the variable substitution used for the project specific supressions file location.
 *     You can see this variable substitution in {@code /config/checkstyle/checkstyle.xml}.</li>
 *     <li>registers the {@code checkstyle} uber-task that depends on all the checkstyle analysis tasks</li>
 *     <li>configures checkstyle tasks to only check modified files (staged, unstaged, and untracked)</li>
 * </ul>
 */
public abstract class CheckstyleConvention implements Plugin<Project> {

  @ServiceReference("git")
  public abstract Property<org.terracotta.build.services.Git> getGit();

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(CheckstylePlugin.class);
    project.getExtensions().configure(CheckstyleExtension.class, checkstyle ->
            checkstyle.getConfigProperties().put("project_config", project.file("config/checkstyle").getAbsolutePath()));

    // Get modified files from git
    Set<String> modifiedFiles = getModifiedFiles(project);

    // Configure all checkstyle tasks to only check modified files
    project.getTasks().withType(Checkstyle.class).configureEach(checkstyleTask -> {
      checkstyleTask.doFirst(task -> {
        // Filter the source files to only include modified files
        checkstyleTask.setSource(checkstyleTask.getSource().filter(file -> {
          String relativePath = project.getRootProject().getProjectDir().toPath()
                  .relativize(file.toPath()).toString();
          return modifiedFiles.contains(relativePath);
        }));
      });
      
      checkstyleTask.onlyIf(task -> {
        if (modifiedFiles.isEmpty()) {
          // No modified files at all, skip checkstyle
          project.getLogger().info("Skipping checkstyle - no modified files in repository");
          return false;
        }
        
        // Check if this specific task will have any files to check
        // We need to filter the source to see if any files match
        long matchingFiles = checkstyleTask.getSource().getFiles().stream()
          .filter(file -> {
            String relativePath = project.getRootProject().getProjectDir().toPath()
                    .relativize(file.toPath()).toString();
            return modifiedFiles.contains(relativePath);
          })
          .count();
        
        if (matchingFiles == 0) {
          project.getLogger().info("Skipping checkstyle for " + project.getName() + " - no modified files in this module");
          return false;
        }
        
        return true;
      });
    });

    project.getTasks().register("checkstyle", task -> {
      task.setDescription("Run Checkstyle analysis on modified files");
      task.setGroup(VERIFICATION_GROUP);
      task.dependsOn(project.getTasks().withType(Checkstyle.class));
    });
  }

  private Set<String> getModifiedFiles(Project project) {
    Set<String> modifiedFiles = new HashSet<>();

    try {
      // Get files that differ from master branch
      modifiedFiles.addAll(executeGit("diff", "--name-only", "master"));
      
      // Get untracked files
      modifiedFiles.addAll(executeGit("ls-files", "--others", "--exclude-standard"));
      
    } catch (Exception e) {
      project.getLogger().warn("Failed to get modified files from git: " + e.getMessage());
      project.getLogger().warn("Checkstyle will run on all files");
    }

    return modifiedFiles;
  }
  
  private Set<String> executeGit(String... command) throws Exception {
    String result = getGit().get().execute(spec -> {
      spec.args(command);
    });
    
    if (result.isEmpty()) {
      return new HashSet<>();
    }
    
    return new HashSet<>(Arrays.asList(result.split("\n")));
  }
}
