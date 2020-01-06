package org.jacoco.core.dao;

import org.jfaster.mango.annotation.DB;
import org.jfaster.mango.annotation.SQL;

/**
 *
 * @author yanpengfang
 * @create 2019-12-30 3:45 PM
 */
@DB(table = "coverage_rate_record")
public interface CoverageRateRecordDao {

    String COLS = "project,class_name,method,covered";

    @SQL("select covered from #table where project=:1 and class_name=:2 and method=:3")
    Integer getCovered(String project, String className, String method);

    @SQL("insert into #table(" + COLS + ") values(:project,:className,:method,:covered) ON DUPLICATE KEY UPDATE covered=:covered")
    int addOrUpdate(CoverageRateDto coverage);

    @SQL("delete from #table where project=:1 and class_name=:2 and method=:3")
    int deletePreRateRecord(String project, String className, String method);
}
