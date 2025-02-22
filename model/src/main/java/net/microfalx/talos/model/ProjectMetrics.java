package net.microfalx.talos.model;

import org.apache.maven.project.MavenProject;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Holds metrics about a project execution.
 */
public class ProjectMetrics extends Project {

    private ZonedDateTime startTime = ZonedDateTime.now();
    private ZonedDateTime endTime = startTime;

    private FailureMetrics failureMetrics;

    protected ProjectMetrics() {
    }

    public ProjectMetrics(MavenProject project) {
        super(project, true);
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

    public FailureMetrics getFailureMetrics() {
        return failureMetrics;
    }

    public void setFailureMetrics(FailureMetrics failureMetrics) {
        this.failureMetrics = failureMetrics;
    }

    public Duration getDuration() {
        if (endTime == null) endTime = ZonedDateTime.now();
        return Duration.between(startTime, endTime);
    }
}
