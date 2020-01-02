/*******************************************************************************
 * Copyright (c) 2009, 2019 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Brock Janiczak - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.startup;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jgit.util.StringUtils;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.MultiSourceFileLocator;
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

    /**
     * Create the report.
     *
     * @throws IOException
     */
    public static void create(String title,
            File executionDataFile,
            File reportDirectory,
            File[] sourceDirs,
            File[] classDirs,
            String gitPath,
            String branch,
            String compareBranch,
            String tag,
            String compareTag)
            throws IOException {

        // Read the jacoco.exec file. Multiple data files could be merged
        // at this point
        ExecFileLoader execFileLoader = loadExecutionData(executionDataFile);

        // Run the structure analyzer on a single class folder to build up
        // the coverage model. The process would be similar if your classes
        // were in a jar file. Typically you would create a bundle for each
        // class folder and each jar you want in your report. If you have
        // more than one bundle you will need to add a grouping node to your
        // report
        final IBundleCoverage bundleCoverage = analyzeStructure(title, execFileLoader,
                gitPath, branch, compareBranch, tag, compareTag, classDirs);

        createReport(bundleCoverage, reportDirectory, execFileLoader, sourceDirs);

    }

    private static void createReport(final IBundleCoverage bundleCoverage, File reportDirectory,
            ExecFileLoader execFileLoader, File[] sourceDirs)
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

        ISourceFileLocator sourceFileLocator;
        if (sourceDirs.length == 1) {
            sourceFileLocator = new DirectorySourceFileLocator(sourceDirs[0], "utf-8", 4);
        } else {
            //多源码路径
            MultiSourceFileLocator sourceLocator = new MultiSourceFileLocator(4);
            for (File sourceFileDir : sourceDirs) {
                sourceLocator.add(new DirectorySourceFileLocator(sourceFileDir, "utf-8", 4));
            }
            sourceFileLocator = sourceLocator;
        }
        visitor.visitBundle(bundleCoverage, sourceFileLocator);
        // Signal end of structure information to allow report to write all
        // information out
        visitor.visitEnd();


    }

    private static ExecFileLoader loadExecutionData(File executionDataFile) throws IOException {
        ExecFileLoader execFileLoader = new ExecFileLoader();
        execFileLoader.load(executionDataFile);
        return execFileLoader;
    }

    private static IBundleCoverage analyzeStructure(String title, ExecFileLoader execFileLoader,
            String gitPath,
            String branch,
            String compareBranch,
            String tag,
            String compareTag,
            File[] classDirs) throws IOException {
        CoverageBuilder coverageBuilder = null;
        if (StringUtils.isEmptyOrNull(tag)) {
            if (StringUtils.isEmptyOrNull(compareBranch)) {
                coverageBuilder = new CoverageBuilder(
                        gitPath, branch);
            } else {
                coverageBuilder = new CoverageBuilder(
                        gitPath, branch, compareBranch);
            }
        } else if (StringUtils.isEmptyOrNull(compareTag)) {
            System.out.println("compareTag is null");
            System.exit(-1);
        } else {
            coverageBuilder = new CoverageBuilder(
                    gitPath, branch, tag, compareTag);
        }

        final Analyzer analyzer = new Analyzer(
                execFileLoader.getExecutionDataStore(), coverageBuilder);
        for (File classDir : classDirs) {
            analyzer.analyzeAll(classDir);
        }
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
    private static final String GIT_WORK_DIR = "git-work-dir";
    private static final String BRANCH = "branch";
    private static final String COMPARE_BRANCH = "compare-branch";
    private static final String TAG = "tag";
    private static final String COMPARE_TAG = "compare-tag";
    private static final String SOURCE_DIRS = "source-dirs";
    private static final String CLASS_DIRS = "class-dirs";
    private static final String REMOTE_HOST = "remote-host";
    private static final String REMOTE_PORT = "remote-port";
    private static final String EXEC_DIR = "exec-dir";
    private static final String REPORT_DIR = "report-dir";
    private static final String MYSQL_JDBC_URL = "mysql-jdbc-url";
    private static final String MYSQL_USER = "mysql-user";
    private static final String MYSQL_PASSWORD = "mysql-password";

    public static void main(final String[] args) {
        try {
            CommandLine commandLine = parseCmmandLine(args);
            String execFile;
            String reportDir = commandLine.getOptionValue(REPORT_DIR);
            if (!commandLine.hasOption(EXEC_DIR)) {
                execFile = reportDir.endsWith("/") ? reportDir + "exec/sq_jacoco.exec" :
                        reportDir + "/exec/sq_jacoco.exec";
            } else {
                String execDir = commandLine.getOptionValue(EXEC_DIR);
                execFile = execDir.endsWith("/") ? execDir + "sq_jacoco.exec" :
                        execDir + "/sq_jacoco.exec";
            }
            //下载exec文件
            ExecutionDataClient client = new ExecutionDataClient(
                    new File(execFile),
                    commandLine.getOptionValue(REMOTE_HOST),
                    Integer.parseInt(commandLine.getOptionValue(REMOTE_PORT)));
            client.dump();

            //生成报告
            String branch = commandLine.getOptionValue(BRANCH);
            String compareBranch = commandLine.getOptionValue(COMPARE_BRANCH);
            String gitWorkDir = commandLine.getOptionValue(GIT_WORK_DIR);
            String tag = commandLine.getOptionValue(TAG);
            String compareTag = commandLine.getOptionValue(COMPARE_TAG);
            String sourceDirsStr = commandLine.getOptionValue(SOURCE_DIRS);
            String[] sourceDirs = sourceDirsStr.split("\\s*,\\s*");
            File[] sourceDirFiles = new File[sourceDirs.length];
            String classDirsStr = commandLine.getOptionValue(CLASS_DIRS);
            String[] classDirs = classDirsStr.split("\\s*,\\s*");
            File[] classDirFiles = new File[classDirs.length];
            String title = new File(gitWorkDir).getName();
            for (int i = 0; i < classDirs.length; ++i) {
                classDirFiles[i] = new File(classDirs[i]);
            }
            for (int i = 0; i < sourceDirs.length; ++i) {
                sourceDirFiles[i] = new File(sourceDirs[i]);
            }
            String mysqlJdbcUrl = commandLine.getOptionValue(MYSQL_JDBC_URL);
            String userName = commandLine.getOptionValue(MYSQL_USER);
            String password = commandLine.getOptionValue(MYSQL_PASSWORD);
            CoverageBuilder.init(mysqlJdbcUrl, userName, password, title);
            create(title, new File(execFile),
                    new File(reportDir),
                    sourceDirFiles,
                    classDirFiles,
                    gitWorkDir,
                    branch,
                    compareBranch,
                    tag,
                    compareTag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static CommandLine parseCmmandLine(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption(null, GIT_WORK_DIR, true, "项目git根目录");
        options.addOption(null, BRANCH, true, "git当前分支");
        options.addOption(null, COMPARE_BRANCH, true, "git对比的分支");
        options.addOption(null, TAG, true, "当前分支的tag");
        options.addOption(null, COMPARE_TAG, true, "git对比的tag");
        options.addOption(null, SOURCE_DIRS, true, "源码目录，多个目录用逗号分割");
        options.addOption(null, CLASS_DIRS, true, "字节码目录，多个用逗号分割");
        options.addOption(null, REMOTE_HOST, true, "远端地址");
        options.addOption(null, REMOTE_PORT, true, "远端端口");
        options.addOption(null, EXEC_DIR, true, "exec文件生成的目录");
        options.addOption(null, REPORT_DIR, true, "测试报告生成的目录");
        options.addOption(null, MYSQL_JDBC_URL, true, "mysql jdbc地址");
        options.addOption(null, MYSQL_USER, true, "连接mysql的用户");
        options.addOption(null, MYSQL_PASSWORD, true, "连接mysql的密码");
        return new DefaultParser().parse(options, args, true);
    }

}
