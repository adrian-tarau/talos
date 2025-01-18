package net.microfalx.maven.model;

import net.microfalx.lang.ObjectUtils;
import net.microfalx.lang.UriUtils;
import org.apache.maven.project.MavenProject;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static net.microfalx.lang.StringUtils.isNotEmpty;

/**
 * A Maven project.
 */
public class Project extends Dependency {

    private URI uri;
    private final Map<String, String> properties = new HashMap<>();

    public Project() {
    }

    public Project(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
    }

    public Project(MavenProject project) {
        this(project.getGroupId(), project.getArtifactId(), project.getVersion());
        init(project);
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    private void init(MavenProject project) {
        setName(project.getName());
        setDescription(project.getDescription());
        try {
            uri = isNotEmpty(project.getUrl()) ? UriUtils.parseUri(project.getUrl()) : null;
        } catch (Exception e) {
            // ideally, the project home page should be valid, but if not, ignore any failure
        }
        project.getProperties().forEach((k, v) -> properties.put(ObjectUtils.toString(k), ObjectUtils.toString(v)));
    }
}
