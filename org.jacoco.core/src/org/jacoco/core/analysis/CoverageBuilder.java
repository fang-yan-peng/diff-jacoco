/*******************************************************************************
 * Copyright (c) 2009, 2019 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package org.jacoco.core.analysis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jacoco.core.dao.CoverageRecordDao;
import org.jacoco.core.data.MethodCoverPair;
import org.jacoco.core.internal.analysis.BundleCoverageImpl;
import org.jacoco.core.internal.analysis.SourceFileCoverageImpl;
import org.jacoco.core.internal.diff.ClassInfo;
import org.jacoco.core.internal.diff.CodeDiff;
import org.jacoco.core.internal.diff.MethodInfo;
import org.jfaster.mango.operator.Mango;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Builder for hierarchical {@link ICoverageNode} structures from single
 * {@link IClassCoverage} nodes. The nodes are feed into the builder through its
 * {@link ICoverageVisitor} interface. Afterwards the aggregated data can be
 * obtained with {@link #getClasses()}, {@link #getSourceFiles()} or
 * {@link #getBundle(String)} in the following hierarchy:
 *
 * <pre>
 * {@link IBundleCoverage}
 * +-- {@link IPackageCoverage}*
 *     +-- {@link IClassCoverage}*
 *     +-- {@link ISourceFileCoverage}*
 * </pre>
 */
public class CoverageBuilder implements ICoverageVisitor {

    private static Map<String, IClassCoverage> classes;

    private static Map<String, ISourceFileCoverage> sourcefiles;

    public static Map<String, ClassInfo> classInfos;

    public static volatile CoverageRecordDao coverageRecordDao;

    public static String project;

    public static void init(String mysqlJdbcUrl, String userName, String password, String title) {
        if (coverageRecordDao == null) {
            synchronized (CoverageBuilder.class) {
                if (coverageRecordDao == null) {
                    HikariConfig config = new HikariConfig();
                    config.setDriverClassName("com.mysql.jdbc.Driver");
                    /*config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/store?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=UTF-8");*/
                    config.setJdbcUrl(mysqlJdbcUrl);
                    config.setMaximumPoolSize(10);
                    config.setMinimumIdle(5);
                    config.setUsername(userName);
                    config.setPassword(password);
                    config.setConnectionTimeout(3000);
                    config.setPoolName("JacocoCoverage");
                    Mango mango = Mango.newInstance(new HikariDataSource(config));
                    coverageRecordDao = mango.create(CoverageRecordDao.class);
                    project = title;
                }
            }
        }
    }

    /**
     * Create a new builder.
     *
     */
    public CoverageBuilder() {
        classes = new HashMap<>();
        sourcefiles = new HashMap<>();
        classInfos = null;
    }

    /**
     * compare new  branch withe master branch
     * @param gitPath local gitPath
     * @param branchName new test branch name
     */
    public CoverageBuilder(String gitPath, String branchName) {
        classes = new HashMap<>();
        sourcefiles = new HashMap<>();
        classInfos = converToMap(CodeDiff.diffBranchToBranch(gitPath, branchName, CodeDiff.MASTER));

    }

    private Map<String, ClassInfo> converToMap(List<ClassInfo> classInfos) {
        return classInfos.stream().collect(Collectors.toMap(
                c -> c.getPackages() + "." + c.getClassName(), c -> c));
    }

    /**
     * compare new  newBranchName withe oldBranchName
     * @param gitPath local gitPath
     * @param newBranchName newBranchName
     * @param oldBranchName oldBranchName
     */
    public CoverageBuilder(String gitPath, String newBranchName, String oldBranchName) {
        classes = new HashMap<>();
        sourcefiles = new HashMap<>();
        classInfos = converToMap(CodeDiff.diffBranchToBranch(gitPath, newBranchName, oldBranchName));
    }

    /**
     * compare new Tag withe old Tag
     * @param gitPath local gitPath
     * @param branchName develop branchName
     * @param newTag new Tag
     * @param oldTag old Tag
     */
    public CoverageBuilder(String gitPath, String branchName, String newTag, String oldTag) {
        classes = new HashMap<>();
        sourcefiles = new HashMap<>();
        classInfos = converToMap(CodeDiff.diffTagToTag(gitPath, branchName, newTag, oldTag));

    }

    /**
     * Returns all class nodes currently contained in this builder.
     *
     * @return all class nodes
     */
    public Collection<IClassCoverage> getClasses() {
        return Collections.unmodifiableCollection(classes.values());
    }

    /**
     * Returns all source file nodes currently contained in this builder.
     *
     * @return all source file nodes
     */
    public Collection<ISourceFileCoverage> getSourceFiles() {
        return Collections.unmodifiableCollection(sourcefiles.values());
    }

    /**
     * Creates a bundle from all nodes currently contained in this bundle.
     *
     * @param name
     *            Name of the bundle
     * @return bundle containing all classes and source files
     */
    public IBundleCoverage getBundle(final String name) {
        return new BundleCoverageImpl(name, classes.values(),
                sourcefiles.values());
    }

    /**
     * Returns all classes for which execution data does not match.
     *
     * @see IClassCoverage#isNoMatch()
     * @return collection of classes with non-matching execution data
     */
    public Collection<IClassCoverage> getNoMatchClasses() {
        final Collection<IClassCoverage> result = new ArrayList<>();
        for (final IClassCoverage c : classes.values()) {
            if (c.isNoMatch()) {
                result.add(c);
            }
        }
        return result;
    }

    // === ICoverageVisitor ===

    public void visitCoverage(final IClassCoverage coverage) {
        final String name = coverage.getName();
        final IClassCoverage dup = classes.put(name, coverage);
        if (dup != null) {
            if (dup.getId() != coverage.getId()) {
                throw new IllegalStateException(
                        "Can't add different class with same name: " + name);
            }
        } else {
            final String source = coverage.getSourceFileName();
            if (source != null) {
                final SourceFileCoverageImpl sourceFile = getSourceFile(source,
                        coverage.getPackageName());
                sourceFile.increment(coverage);
            }
        }
    }

    private SourceFileCoverageImpl getSourceFile(final String filename,
            final String packageName) {
        final String key = packageName + '/' + filename;
        SourceFileCoverageImpl sourceFile = (SourceFileCoverageImpl) sourcefiles
                .get(key);
        if (sourceFile == null) {
            sourceFile = new SourceFileCoverageImpl(filename, packageName);
            sourcefiles.put(key, sourceFile);
        }
        return sourceFile;
    }

    public static MethodCoverPair belongToMethodMd5(String className, int lineNr) {
        String classPath = className.replaceAll("\\.", "/");
        IClassCoverage classCoverage = classes.get(classPath);
        if (classCoverage == null) {
            return null;
        }
        IMethodCoverage belongMethodCoverage = null;
        for (IMethodCoverage methodCoverage : classCoverage.getMethods()) {
            if (methodCoverage.getFirstLine() <= lineNr && lineNr <= methodCoverage.getLastLine()) {
                belongMethodCoverage = methodCoverage;
                break;
            }
        }
        if (belongMethodCoverage == null) {
            return null;
        }
        ClassInfo classInfo = classInfos.get(className);
        if (classInfo == null || classInfo.getMethodInfos() == null) {
            return null;
        }

        MethodInfo methodInfo = classInfo.getMethodInfos().get(belongMethodCoverage.getName());
        if (methodInfo == null) {
            return null;
        }
        return new MethodCoverPair(methodInfo.getMethodName(),
                methodInfo.md5, classCoverage, belongMethodCoverage);
    }
}
