create table coverage_record(
id bigint primary key auto_increment,
project varchar(64),
class_name varchar(128),
method varchar(64),
line varchar(128),
method_md5 char(32),
line_md5 char(32),
type char(2),
unique key p_c_m_l(project, class_name, method_md5, line_md5),
key p_c_m(project, class_name, method)
);

create table coverage_rate_record(
id bigint primary key auto_increment,
project varchar(64),
class_name varchar(128),
method varchar(64),
covered int,
unique key p_c_m(project, class_name, method)
);