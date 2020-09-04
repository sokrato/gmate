package io.github.sokrato.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;

public class GmatePlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        PluginContainer container = project.getPlugins();
        container.apply(GitManifestPlugin.class);
        container.apply(ClassPathSaverPlugin.class);
    }
}
