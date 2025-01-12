package net.microfalx.maven.model;

import org.apache.maven.project.MavenProject;

/**
 * A Maven project.
 */
public class Project extends Dependency {

    public Project() {
    }

    public Project(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
    }

    public Project(MavenProject project) {
        this(project.getGroupId(), project.getArtifactId(), project.getVersion());
        setName(project.getName());
        setDescription(project.getDescription());
    }
}
