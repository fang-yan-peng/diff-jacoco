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
package org.jacoco.report.internal.html.page;

import static org.jacoco.core.internal.diff.ASTGenerator.MD5Encode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.analysis.ISourceNode;
import org.jacoco.core.dao.CoverageDto;
import org.jacoco.core.dao.CoverageRateDto;
import org.jacoco.core.data.MethodCoverPair;
import org.jacoco.core.internal.analysis.SourceFileCoverageImpl;
import org.jacoco.core.internal.diff.ClassInfo;
import org.jacoco.report.internal.html.HTMLElement;
import org.jacoco.report.internal.html.resources.Styles;

/**
 * Creates a highlighted output of a source file.
 */
final class SourceHighlighter {

    private final Locale locale;

    private String lang;

    /**
     * Creates a new highlighter with default settings.
     *
     * @param locale
     *            locale for tooltip rendering
     */
    public SourceHighlighter(final Locale locale) {
        this.locale = locale;
        lang = "java";
    }

    /**
     * Specifies the source language. This value might be used for syntax
     * highlighting. Default is "java".
     *
     * @param lang
     *            source language identifier
     */
    public void setLanguage(final String lang) {
        this.lang = lang;
    }

    /**
     * Highlights the given source file.
     *
     * @param parent
     *            parent HTML element
     * @param source
     *            highlighting information
     * @param contents
     *            contents of the source file
     * @throws IOException
     *             problems while reading the source file or writing the output
     */
    public void render(final HTMLElement parent, final ISourceNode source,
            final Reader contents) throws IOException {
        final HTMLElement pre = parent.pre(Styles.SOURCE + " lang-" + lang
                + " linenums");
        final BufferedReader lineBuffer = new BufferedReader(contents);
        String classPath = ((SourceFileCoverageImpl) source).getPackageName() + "." + source.getName().replaceAll(".java", "");
        classPath = classPath.replaceAll("/", ".");
        String line;
        int nr = 0;
        Set<String> hasRecord = new HashSet<>();
        while ((line = lineBuffer.readLine()) != null) {
            nr++;
            renderCodeLine(pre, line, source.getLine(nr), nr, classPath, hasRecord);
        }
    }

    private void renderCodeLine(final HTMLElement pre, final String linesrc,
            final ILine line, final int lineNr, final String classPath, Set<String> hasRecord) throws IOException {
        if (CoverageBuilder.classInfos == null || CoverageBuilder.classInfos.isEmpty()) {
            //	全量覆盖
            highlight(pre, line, lineNr, null).text(linesrc);
            pre.text("\n");
        } else {
            //	增量覆盖
            boolean existFlag = true;
            ClassInfo classInfo = CoverageBuilder.classInfos.get(classPath);
            if (classInfo != null) {
                //	新增的类
                if ("ADD".equalsIgnoreCase(classInfo.getType())) {
                    highlight(pre, line, lineNr, null).text("+ " + linesrc);
                    pre.text("\n");
                } else {
                    //	修改的类
                    MethodCoverPair pair = CoverageBuilder.belongToMethodMd5(classPath, lineNr);
                    if (pair == null || pair.getCoverageRate() > 0) {
                        boolean flag = false;
                        List<int[]> addLines = classInfo.getAddLines();
                        for (int[] ints : addLines) {
                            if (ints[0] <= lineNr && lineNr <= ints[1]) {
                                flag = true;
                                break;
                            }
                        }
                        if (flag) {
                            highlight(pre, line, lineNr, null).text("+ " + linesrc);
                            pre.text("\n");
                        } else {
                            highlight(pre, line, lineNr, null).text(" " + linesrc);
                            pre.text("\n");
                        }
                        String type;
                        if (pair != null && pair.getCoverageRate() > 0 && (type = getType(line)) != null) {
                            CoverageDto dto = new CoverageDto(CoverageBuilder.project,
                                    classPath,
                                    pair.getMethodName(),
                                    linesrc,
                                    pair.getMd5(),
                                    MD5Encode(linesrc), type);
                            String uniqueKey = classPath + "_" + pair.getMethodName();
                            if (!hasRecord.contains(uniqueKey)) {
                                CoverageRateDto rateDto = new CoverageRateDto(CoverageBuilder.project,
                                        classPath,
                                        pair.getMethodName(), pair.getMethodCovered());
                                try {
                                    CoverageBuilder.coverageRateRecordDao.addOrUpdate(rateDto);
                                    hasRecord.add(uniqueKey);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                CoverageBuilder.coverageRecordDao.addOrUpdate(dto);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        String type = CoverageBuilder.coverageRecordDao.getType(CoverageBuilder.project,
                                classPath, pair.getMd5(), MD5Encode(linesrc));
                        if (type == null) { //如果不存在，有可能方法修改了，需要删除以前的记录
                            String uniqueKey = classPath + "_" + pair.getMethodName();
                            if (!hasRecord.contains(uniqueKey)) {
                                CoverageBuilder.coverageRecordDao.deletePreRecord(
                                        CoverageBuilder.project, classPath, pair.getMethodName());
                                CoverageBuilder.coverageRateRecordDao.deletePreRateRecord
                                        (CoverageBuilder.project, classPath, pair.getMethodName());
                                hasRecord.add(uniqueKey);
                            }
                        } else {
                            //修改覆盖率。
                            String uniqueKey = classPath + "_" + pair.getMethodName();
                            if (!hasRecord.contains(uniqueKey)) {
                                Integer covered = CoverageBuilder.coverageRateRecordDao.getCovered(
                                        CoverageBuilder.project, classPath, pair.getMethodName());
                                if (covered != null) {
                                    pair.updateCoverCounter(covered);
                                }
                                hasRecord.add(uniqueKey);
                            }

                        }
                        boolean flag = false;
                        List<int[]> addLines = classInfo.getAddLines();
                        for (int[] ints : addLines) {
                            if (ints[0] <= lineNr && lineNr <= ints[1]) {
                                flag = true;
                                break;
                            }
                        }
                        if (flag) {
                            highlight(pre, line, lineNr, type).text("+ " + linesrc);
                            pre.text("\n");
                        } else {
                            highlight(pre, line, lineNr, type).text(" " + linesrc);
                            pre.text("\n");
                        }

                    }
                }
                existFlag = false;
            }
            if (existFlag) {
                highlight(pre, line, lineNr, null).text(" " + linesrc);
                pre.text("\n");
            }
        }
    }

    HTMLElement highlight(final HTMLElement pre, final ILine line,
            final int lineNr, String preType) throws IOException {
        final String style = preType == null ? getType(line) : preType;
        if (style == null) {
            return pre;
        }
        final String lineId = "L" + Integer.toString(lineNr);
        final ICounter branches = line.getBranchCounter();
        switch (branches.getStatus()) {
            case ICounter.NOT_COVERED:
                return span(pre, lineId, style, Styles.BRANCH_NOT_COVERED,
                        "All %2$d branches missed.", branches);
            case ICounter.FULLY_COVERED:
                return span(pre, lineId, style, Styles.BRANCH_FULLY_COVERED,
                        "All %2$d branches covered.", branches);
            case ICounter.PARTLY_COVERED:
                return span(pre, lineId, style, Styles.BRANCH_PARTLY_COVERED,
                        "%1$d of %2$d branches missed.", branches);
            default:
                return pre.span(style, lineId);
        }
    }

    private String getType(final ILine line) {
        switch (line.getStatus()) {
            case ICounter.NOT_COVERED:
                return Styles.NOT_COVERED;
            case ICounter.FULLY_COVERED:
                return Styles.FULLY_COVERED;
            case ICounter.PARTLY_COVERED:
                return Styles.PARTLY_COVERED;
            default:
                return null;
        }
    }

    private HTMLElement span(final HTMLElement parent, final String id,
            final String style1, final String style2, final String title,
            final ICounter branches) throws IOException {
        final HTMLElement span = parent.span(style1 + " " + style2, id);
        final Integer missed = branches.getMissedCount();
        final Integer total = branches.getTotalCount();
        span.attr("title", String.format(locale, title, missed, total));
        return span;
    }

}
