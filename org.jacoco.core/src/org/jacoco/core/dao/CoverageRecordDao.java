package org.jacoco.core.dao;

import org.jfaster.mango.annotation.DB;
import org.jfaster.mango.annotation.SQL;

/**
 *
 * @author yanpengfang
 * @create 2019-12-30 3:45 PM
 */
@DB(table = "coverage_record")
public interface CoverageRecordDao {

    String COLS = "project,class_name,method,line,method_md5,line_md5,type";

    @SQL("select type from #table where project=:1 and class_name=:2 and method_md5=:3 and line_md5=:4")
    String getType(String project, String className,String methodMd5, String lineMd5);

    @SQL("insert into #table(" + COLS + ") values(:project,:className,:method,:line,:methodMd5," +
            ":lineMd5,:type) ON DUPLICATE KEY UPDATE type=:type")
    int addOrUpdate(CoverageDto coverage);

    @SQL("delete from #table where project=:1 and class_name=:2 and method=:3")
    int deletePreRecord(String project, String className, String method);
}
