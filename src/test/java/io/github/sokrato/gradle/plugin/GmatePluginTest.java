package io.github.sokrato.gradle.plugin;

import org.gradle.api.Project;
import org.gradle.api.plugins.PluginContainer;
import org.gradle.testfixtures.ProjectBuilder;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class GmatePluginTest {
    @Test
    public void apply() {
        Project project = ProjectBuilder.builder().build();
        PluginContainer container = project.getPlugins();
        GmatePlugin plugin = container.apply(GmatePlugin.class);

        assertNotNull(plugin);
        assertNotNull(container.getPlugin(GitManifestPlugin.class));
        assertNotNull(container.getPlugin(ClassPathSaverPlugin.class));
    }
}