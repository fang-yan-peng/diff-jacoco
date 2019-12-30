JaCoCo Java Code Coverage Library
=================================

[![Build Status](https://travis-ci.org/jacoco/jacoco.svg?branch=master)](https://travis-ci.org/jacoco/jacoco)
[![Build status](https://ci.appveyor.com/api/projects/status/g28egytv4tb898d7/branch/master?svg=true)](https://ci.appveyor.com/project/JaCoCo/jacoco/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/org.jacoco/jacoco.svg)](http://search.maven.org/#search|ga|1|g%3Aorg.jacoco)

JaCoCo is a free Java code coverage library distributed under the Eclipse Public
License. Check the [project homepage](http://www.jacoco.org/jacoco)
for downloads, documentation and feedback.

Please use our [mailing list](https://groups.google.com/forum/?fromgroups=#!forum/jacoco)
for questions regarding JaCoCo which are not already covered by the
[extensive documentation](http://www.jacoco.org/jacoco/trunk/doc/).

Note: 欢迎一起开发，有问题提issue
-------------------------------------------------------------------------

JaCoCo二次开发基于Git分支差分实现增量代码覆盖率

- 原理：通过使用org.eclipse
.jgit
比较新旧分支代码差分，取得变更行信息，生成报告时高亮线上变更行信息，未检出变更行不做处理。从而达到，增量显示代码覆盖率的目的。当然当不传代码分支时默认展示全量覆盖率。测试过的代码报告会存入mysql中，如果更改某类中的方法内容，不会影响同类中其他未更改方法的测试报告。

- 使用方法：

  ##### 一，Jacoco包的配置(具体配置百度Jacoco的Tomcat配置)

  ###### 1，自己动手模式：下载我的代码，执行mvn clean package -Dmaven.javadoc.test=true -Dmaven.test
  .skip=true自行打包。注意事项IDE下载后，在IDE中执行maven打包命令可能不成功。建议在命令行中执行maven打包命令。打包成功后，用官网包lib/jacocoagent.jar 替换自己新打包生成的jacocoagent.jar文件，否则可能引起Tomcat无法启动。

#####      二，代码差分比较分支说明
​     以这个测试代码为例,假设daily为变更后要Release的代码，master分支为基线分支。测试的目的是想测试新开发的代码也即daily分支的代码是否全部已测试覆盖。我们测试时把daily下载后打包发布完成测试（在第一步Jacoco已经再Tomcat下配置好的情况下）。

#####      三，下载jacoco.exec（这里主要考虑到集成到Devops平台，通过代码获取）

```java
package org.jacoco.startup;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;

import org.jacoco.core.tools.ExecDumpClient;
import org.jacoco.core.tools.ExecFileLoader;

/**
 * This example connects to a coverage agent that run in output mode
 * <code>tcpserver</code> and requests execution data. The collected data is
 * dumped to a local file.
 *
 * --git_workdir=xx  --current-branch=xx --compare-branch=xx (--current-tag=xx --compare-tag=xx)
 * --source-dirs=xx,xx,xxxx --class-dirs=scs,sdca,sssss --remote-host=xxxxxx --remote-port=8044
 * --exec-dir=xx --report-dir=xxx --mysql-host=xxx --mysql-port=3306 --mysql-user=root
 * --mysql-password=1234
 */
public final class ExecutionDataClient {

    private boolean dump;
    private boolean reset;
    private File destFile;
    private String address;
    private int port;
    private int retryCount;
    private boolean append;

    public ExecutionDataClient(File destFile, String address, int port) {
        this(true, false, destFile, address, port, 10, true);
    }

    public ExecutionDataClient(boolean dump, boolean reset, File destFile, String address, int
            port, int retryCount, boolean append) {
        this.dump = dump;
        this.reset = reset;
        this.destFile = destFile;
        this.address = address;
        this.port = port;
        this.retryCount = retryCount;
        this.append = append;
    }

    public void dump() {
        if (port <= 0) {
            throw new IllegalArgumentException("Invalid port value");
        }
        if (dump && destFile == null) {
            throw new IllegalArgumentException(
                    "Destination file is required when dumping execution data");
        }

        final ExecDumpClient client = new ExecDumpClient() {
            @Override
            protected void onConnecting(final InetAddress address,
                    final int port) {
                System.out.println(format("Connecting to %s:%d", address, port));
            }

            @Override
            protected void onConnectionFailure(final IOException exception) {
                exception.printStackTrace();
            }
        };
        client.setDump(dump);
        client.setReset(reset);
        client.setRetryCount(retryCount);

        try {
            final ExecFileLoader loader = client.dump(address, port);
            if (dump) {
                System.out.println(format("Dumping execution data to %s",
                        destFile.getAbsolutePath()));

                loader.save(destFile, append);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Starts the execution data request.
     *
     * @param args
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        ExecutionDataClient client = new ExecutionDataClient(
                new File("/Users/yanpengfang/Desktop/sq_jacoco/jacoco-client.exec"),
                "localhost",
                8044);
        client.dump();
    }
}

```
这样就非常方便的不停服务的情况下，去导出exec文件了。

##### 四，基于Git分支差分解析jacoco.exec生成报告，并且根据之前存储报告信息只生成增量方法报告。

```java
package org.jacoco.startup;

import java.io.File;
import java.io.IOException;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.html.HTMLFormatter;

/**
 * This example creates a HTML report for eclipse like projects based on a
 * single execution data store called jacoco.exec. The report contains no
 * grouping information.
 *
 * The class files under test must be compiled with debug information, otherwise
 * source highlighting will not work.
 */
public class ReportGenerator {

    private final String title;

    private final File executionDataFile;
    private final File classesDirectory;
    private final File sourceDirectory;
    private final File reportDirectory;

    private ExecFileLoader execFileLoader;

    /**
     * Create a new generator based for the given project.
     *
     * @param projectDirectory
     */
    public ReportGenerator(final File projectDirectory) {
        this.title = projectDirectory.getName();
        this.executionDataFile = new File(
                "/Users/yanpengfang/Desktop/sq_jacoco/jacoco-client.exec");
        this.classesDirectory = new File(projectDirectory, "target/classes");
        this.sourceDirectory = new File(projectDirectory, "src/main/java");
        this.reportDirectory = new File("/Users/yanpengfang/Desktop/sq_jacoco/coveragereport");
    }

    /**
     * Create the report.
     *
     * @throws IOException
     */
    public void create() throws IOException {

        // Read the jacoco.exec file. Multiple data files could be merged
        // at this point
        loadExecutionData();

        // Run the structure analyzer on a single class folder to build up
        // the coverage model. The process would be similar if your classes
        // were in a jar file. Typically you would create a bundle for each
        // class folder and each jar you want in your report. If you have
        // more than one bundle you will need to add a grouping node to your
        // report
        final IBundleCoverage bundleCoverage = analyzeStructure();

        createReport(bundleCoverage);

    }

    private void createReport(final IBundleCoverage bundleCoverage)
            throws IOException {

        // Create a concrete report visitor based on some supplied
        // configuration. In this case we use the defaults
        final HTMLFormatter htmlFormatter = new HTMLFormatter();
        final IReportVisitor visitor = htmlFormatter
                .createVisitor(new FileMultiReportOutput(reportDirectory));

        // Initialize the report with all of the execution and session
        // information. At this point the report doesn't know about the
        // structure of the report being created
        visitor.visitInfo(execFileLoader.getSessionInfoStore().getInfos(),
                execFileLoader.getExecutionDataStore().getContents());

        // Populate the report structure with the bundle coverage information.
        // Call visitGroup if you need groups in your report.
        visitor.visitBundle(bundleCoverage, new DirectorySourceFileLocator(
                sourceDirectory, "utf-8", 4));

        // Signal end of structure information to allow report to write all
        // information out
        visitor.visitEnd();

    }

    private void loadExecutionData() throws IOException {
        execFileLoader = new ExecFileLoader();
        execFileLoader.load(executionDataFile);
    }

    private IBundleCoverage analyzeStructure() throws IOException {
        final CoverageBuilder coverageBuilder = new CoverageBuilder(
                "/Users/yanpengfang/sqyc/ai-charge", "jacoco_test");
        CoverageBuilder.setProject(title);
        final Analyzer analyzer = new Analyzer(
                execFileLoader.getExecutionDataStore(), coverageBuilder);

        analyzer.analyzeAll(classesDirectory);
        return coverageBuilder.getBundle(title);
    }

    /**
     * Starts the report generation process
     *
     * @param args
     *            Arguments to the application. This will be the location of the
     *            eclipse projects that will be used to generate reports for
     * @throws IOException
     */
    public static void main(final String[] args) throws IOException {
        for (int i = 0; i < args.length; i++) {
            final ReportGenerator generator = new ReportGenerator(new File(
                    args[i]));
            generator.create();
        }
    }

}

```
执行完后就可以生成报告了。通过第一步，第二步结合，就可以随时导出Ecec文件，生成报告，查看测试覆盖情况了。那种需要启停服务的

##### 五 生成的差分报告展示

通过率概览：

![图片](https://inside-git.01zhuanche.com/component/sq-jacoco/blob/master/report1.png)

覆盖的代码显示为绿色, 未覆盖的代码显示为红色

![图片](https://inside-git.01zhuanche.com/component/sq-jacoco/blob/master/report2.png)
