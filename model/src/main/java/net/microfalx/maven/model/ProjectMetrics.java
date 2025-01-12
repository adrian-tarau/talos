package net.microfalx.maven.model;

import org.apache.maven.project.MavenProject;

import java.time.ZonedDateTime;

/**
 * Holds metrics about a project execution.
 */
public class ProjectMetrics extends Project {

    private ZonedDateTime startTime;
    private ZonedDateTime endTime;

    protected ProjectMetrics() {
    }

    public ProjectMetrics(MavenProject project) {
        super(project);
    }

    public ZonedDateTime getStartTime() {
        return startTime;
    }

    public ProjectMetrics setStartTime(ZonedDateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    public ZonedDateTime getEndTime() {
        return endTime;
    }

    public ProjectMetrics setEndTime(ZonedDateTime endTime) {
        this.endTime = endTime;
        return this;
    }
}
