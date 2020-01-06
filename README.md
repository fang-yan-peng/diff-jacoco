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

  #### 一，Jacoco包的配置

  ##### 1，执行mvn clean install -Dmaven.javadoc.test=true -Dmaven.test.skip=true
  自行打包。注意事项IDE下载后，在IDE中执行maven打包命令可能不成功。建议在命令行中执行maven打包命令。打包成功后，会在 JacocoPlus/org.jacoco
  .startup/target目录下生成org.jacoco.startup-0.8.4.tar.gz。解压后会有三个目录，分别是bin、lib和conf。其中conf/jacoco.conf文件是jacoco运行所需要的配置信息，如数据库的jdbcUrl、用户名密码和堆栈大小等。
  
  ##### 2，执行bin目录下jacoco.sh，以tag比较为例
   ./jacoco.sh --git-work-dir /Users/yanpengfang/sqyc/ai-charge --branch master --tag jacoco_1 
   --compare-tag jacoco_3 --report-dir /Users/yanpengfang/Desktop/sq_jacoco/coveragereport --source-dirs /Users/yanpengfang/sqyc/ai-charge/charge-api/src/main/java,/Users/yanpengfang/sqyc/ai-charge/charge-core/src/main/java --class-dirs /Users/yanpengfang/sqyc/ai-charge/charge-api/target/classes,/Users/yanpengfang/sqyc/ai-charge/charge-core/target/classes --remote-host localhost --remote-port 8044 --exec-dir /Users/yanpengfang/Desktop/sq_jacoco

  ####  二，代码差分比较分支说明
   ##### 1，基于分支的比较
   --branch 指定当前代码分支， --compare-branch 指定要对比的的代码分支，如果不指定默认为master。
   ##### 2，基于tag的比较
   --branch 指定是哪个分支上的tag， --tag 当前tag，--compare-tag 指定要对比的tag。
  #### 三，参数说明
  * git-work-dir 指定git的工作目录
  * report-dir 生成报告的目录
  * source-dirs 源码目录，多个目录用逗号分隔
  * class-dirs 字节码目录，多个目录用逗号分隔
  * remote-host jacoco的agent运行的地址
  * remote-port jacoco的agent运行的端口号
  * exec-dir exec文件生成的目录，如果不指定默认是report-dir指定的目录/exec。
  * branch 当前代码分支
  * compare-branch 要对比的分支
  * tag 当前tag
  * compare-tag 要对比的tag
  #### 四，代码颜色说明
  * 红色 未覆盖的代码
  * 绿色 覆盖的代码
  * 白色 分支对比或者tag对比没有发生变化的代码，一般不用关注

执行完后就可以生成报告了。就可以随时导出Ecec文件，生成报告，查看测试覆盖情况了。

##### 五 生成的差分报告展示

通过率概览：

![图片](https://inside-git.01zhuanche.com/component/sq-jacoco/blob/master/report1.png)

覆盖的代码显示为绿色, 未覆盖的代码显示为红色, 白色的代码是没有发生更改的。

![图片](https://inside-git.01zhuanche.com/component/sq-jacoco/blob/master/report2.png)
