package org.jacoco.core.dao;

/**
 *
 * @author yanpengfang
 * create 2019-12-30 3:49 PM
 */
public class CoverageRateDto {
    private Long id;
    private String project;
    private String className;
    private String method;
    private int covered;

    public CoverageRateDto() {
    }

    public CoverageRateDto(String project, String className,
            String method, int covered) {
        this.project = project;
        if (className.length() > 128) {
            this.className = className.substring(0, 128);
        } else {
            this.className = className;
        }
        if (method.length() > 64) {
            this.method = method.substring(0, 64);
        } else {
            this.method = method;
        }
        this.covered = covered;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public int getCovered() {
        return covered;
    }

    public void setCovered(int covered) {
        this.covered = covered;
    }
}
