package io.github.sokrato.gradle.plugin;

/**
 * to customize manifest key:
 * <p>
 * import io.github.sokrato.gradle.plugin.GitManifestPluginExtension
 * the&lt;GitManifestPluginExtension&gt;().revisionKey = "GitRevision"
 */
public class GitManifestPluginExtension {
    public String revisionKey = "Git-Revision";
    public String remoteKey = "Git-Remote";
    public String branchKey = "Git-Branch";
    public String useRemote = "origin";
}
