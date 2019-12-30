package org.jacoco.core.data;

/**
 *
 * @author yanpengfang
 * create 2019-12-30 2:52 PM
 */
public class MethodCoverPair {
    private String methodName;
    private String md5;
    private double coverageRate;

    public MethodCoverPair(String methodName, String md5, double coverageRate) {
        this.methodName = methodName;
        this.md5 = md5;
        this.coverageRate = coverageRate;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public double getCoverageRate() {
        return coverageRate;
    }

    public void setCoverageRate(double coverageRate) {
        this.coverageRate = coverageRate;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }
}
