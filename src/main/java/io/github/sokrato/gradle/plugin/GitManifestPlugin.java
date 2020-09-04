package io.github.sokrato.gradle.plugin;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.java.archives.Attributes;
import org.gradle.api.tasks.bundling.Jar;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public class GitManifestPlugin implements Plugin<Project> {

    private final Map<String, GitInfo> cache = new HashMap<>();

    private GitManifestPluginExtension ext;

    @Override
    public void apply(Project project) {
        ext = project.getExtensions()
                .create(GitManifestPluginExtension.class,
                        "gitManifest",
                        GitManifestPluginExtension.class);

        project.afterEvaluate(p -> {
            p.getTasks()
                    .withType(Jar.class)
                    .forEach(jar -> updateManifest(p, jar));
        });
    }

    private void updateManifest(Project project, Jar jar) {
        jar.doFirst(task -> {
            GitInfo gitInfo = gitInfo(project);
            ((Jar) task).manifest(mf -> {
                Attributes attrs = mf.getAttributes();
                attrs.put(ext.revisionKey, gitInfo.revision);
                attrs.put(ext.remoteKey, gitInfo.remoteURL);
            });
        });
    }

    private GitInfo gitInfo(Project project) {
        final String rootDir = project.getRootDir().toString();
        GitInfo info = cache.get(rootDir);
        if (info == null || info.expired()) {
            info = new GitInfo();
            try {
                if (hasUnCommitted(rootDir))
                    project.getLogger()
                            .warn("You have uncommitted content! {} will not be accurate!", ext.revisionKey);

                info.setRevision(gitRevision(rootDir));
                info.setRemoteURL(gitRemote(rootDir));
            } catch (IOException | InterruptedException ex) {
                project.getLogger().error(ex.getMessage());
            }
            cache.put(rootDir, info);
        }
        return info;
    }

    private static final Pattern uncommittedPattern =
            Pattern.compile(".*Changes (not staged for commit|to be committed).*", Pattern.DOTALL);

    private boolean hasUnCommitted(String dir) throws IOException, InterruptedException {
        return ShellUtil.runAndGetOutput(Arrays.asList(
                "git", "-C", dir, "status"
        )).anyMatch(line -> uncommittedPattern.matcher(line).matches());
    }

    private static final Pattern revisionPattern = Pattern.compile("[a-z0-9]{7,}");

    private String gitRevision(String dir) throws IOException, InterruptedException {
        Optional<String> firstLine = ShellUtil.runAndGetOutput(Arrays.asList(
                "git", "-C", dir, "rev-parse", "--short", "HEAD"
        )).findFirst();

        if (!firstLine.isPresent()) {
            throw new IOException("failed to get current revision");
        }
        String line = firstLine.get().trim();
        if (revisionPattern.matcher(line).matches())
            return line;
        throw new IOException("failed to get current revision: " + line);
    }

    private String gitRemote(String dir) throws IOException, InterruptedException {
        final Map<String, String> remotes = new HashMap<>();
        ShellUtil.runAndGetOutput(Arrays.asList(
                "git", "-C", dir, "remote", "-v"))
                .filter(line -> line.startsWith(ext.useRemote))
                .forEach(line -> {
                    String[] parts = line.split("\\s+");
                    remotes.put(parts[2], parts[1]);
                });

        if (remotes.containsKey("(fetch)"))
            return remotes.get("(fetch)");

        return remotes.values().stream().findFirst().orElse("");
    }

    private static class GitInfo {
        private String revision;
        private String remoteURL;
        private final long checkedAt = System.currentTimeMillis();

        public GitInfo() {
            this("", "");
        }

        public GitInfo(String revision, String remoteURL) {
            this.revision = revision;
            this.remoteURL = remoteURL;
        }

        public String getRevision() {
            return revision;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        public String getRemoteURL() {
            return remoteURL;
        }

        public void setRemoteURL(String remoteURL) {
            this.remoteURL = remoteURL;
        }

        public long getCheckedAt() {
            return checkedAt;
        }

        public boolean expired() {
            long elapsed = System.currentTimeMillis() - checkedAt;
            return elapsed < 0 || elapsed > 5_000;
        }
    }
}
