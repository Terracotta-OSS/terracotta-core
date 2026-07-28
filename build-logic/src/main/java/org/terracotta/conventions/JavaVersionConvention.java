package org.terracotta.conventions;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.JvmTestSuitePlugin;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.testing.base.TestingExtension;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.terracotta.build.plugins.JavaVersionPlugin;

public class JavaVersionConvention implements Plugin<Project> {

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaVersionPlugin.class);

    JavaVersionPlugin.JavaVersions javaVersions = project.getExtensions().getByType(JavaVersionPlugin.JavaVersions.class);

    project.getPlugins().withType(JavaPlugin.class, javaPlugin ->
        project.getExtensions().configure(JavaPluginExtension.class, java ->
            java.toolchain(toolchain ->
                toolchain.getLanguageVersion().convention(javaVersions.getCompileVersion()))));

    project.getPlugins().withType(JvmTestSuitePlugin.class).configureEach(plugin -> {
        project.getExtensions().configure(TestingExtension.class, testing -> {
            testing.getSuites().withType(JvmTestSuite.class).configureEach(testSuite -> {
                testSuite.getTargets().configureEach(target -> {
                    target.getTestTask().configure(test -> {
                        test.getJavaLauncher().set(project.getExtensions().getByType(JavaToolchainService.class)
                                .launcherFor(spec ->
                                        spec.getLanguageVersion().convention(javaVersions.getTestVersion())));
                        test.environment("JAVA_HOME", test.getJavaLauncher().get().getMetadata().getInstallationPath());
                    });
                });
            });
        });
    });
  }
}
