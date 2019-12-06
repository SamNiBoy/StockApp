drop table if exists usr;
create table usr(
id int(5) not null auto_increment comment 'User ID',
username varchar(20) not null,
password varchar(8) not null,
account_type int(1) not null, /*0: Consultant, 1: Company*/
mail varchar(100),
phone varchar(100),
address varchar(200),
company varchar(200),
add_dt datetime not null,
PRIMARY KEY (id)
);

insert into usr (username, password, account_type, mail, phone, address, company, add_dt) values('Sam Ni', 'SUPER', 0, 'yl_nxj@163.com', '1391638409', '嘉定依玛路389弄', 'LamaiSys', now());
insert into usr (username, password, account_type, mail, phone, address, company, add_dt) values('Yelang', 'SUPER', 1,'yl_nxj@163.com', '1391638409', '嘉定依玛路389弄', 'LamaiSys', now());
insert into usr (username, password, account_type, mail, phone, address, company, add_dt) values('NiXiaoJun', 'SUPER', 2, 'yl_nxj@163.com', '1391638409', '嘉定依玛路389弄', 'Lamai', now());


drop table if exists sr_hdr;
create table sr_hdr(
id int(15) not null auto_increment comment 'Support Request Header ID',
logger_id int(5) not null,
assignee_id int(5),
title varchar(100) not null,
summary varchar(5000) not null,
sr_type int(1) not null, /*0: onsite support, 1: feature development, 2: Issue fix request */
status int(1) not null, /*0: Open, 1: Assigned, 2: Inprogress, 3: NMI, 4:Reopened, 5: pendpay, 6:Closed */
add_dt datetime not null,
PRIMARY KEY (id)
);

drop table if exists sr_dtl;
create table sr_dtl(
id int(20) not null,
sr_id int(15) not null,
assignee_id int(5),
comment varchar(5000) not null,
frm_status int(1) not null,
to_status int(1) not null,
add_dt datetime not null,
PRIMARY KEY (id)
);
