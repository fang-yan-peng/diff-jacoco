package org.jacoco.core.dao;

/**
 *
 * @author yanpengfang
 * create 2019-12-30 3:49 PM
 */
public class CoverageDto {
    private Long id;
    private String project;
    private String className;
    private String method;
    private String line;
    private String methodMd5;
    private String lineMd5;
    private String type;

    public CoverageDto() {
    }

    public CoverageDto(String project, String className,
            String method, String line, String methodMd5, String lineMd5, String type) {
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
        if (line.length() > 128) {
            this.line = line.substring(0, 128);
        } else {
            this.line = line;
        }
        this.methodMd5 = methodMd5;
        this.lineMd5 = lineMd5;
        this.type = type;
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

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public String getMethodMd5() {
        return methodMd5;
    }

    public void setMethodMd5(String methodMd5) {
        this.methodMd5 = methodMd5;
    }

    public String getLineMd5() {
        return lineMd5;
    }

    public void setLineMd5(String lineMd5) {
        this.lineMd5 = lineMd5;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
