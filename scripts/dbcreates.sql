create table stk(
id varchar2(6 byte) not null primary key,
area varchar2(2 byte) not null,
name varchar2(12 byte) not null,
py varchar2(4 byte),
bu varchar2(12 byte)
);

create sequence SEQ_STKDAT_PK
  minvalue 1
  maxvalue 99999999
  start with 1
  increment by 1
  cache 20;
/*
1:86.30 今日开盘价
2:86.31 昨日收盘价
3:84.41 当前价格
4:86.30 今日最高价
5:83.70 今日最低价
6:84.38 买一
7:84.40 卖一
8:156070902 成交量
9:13235768984 成交额
10:2200
11:84.38
12:20300
13:84.37
14:12800
15:84.36
16:24100
17:84.35
18:3000
19:84.33
20:40750
21:84.40
22:54800
23:84.42
24:400
25:84.44
26:3300
27:84.45
28:2500
29:84.46
30:2015-05-15
31:15:04:06
*/
create table stkDat(
ft_id number not null primary key,
id varchar2(6 byte) not null,
td_opn_pri number not null,
yt_cls_pri number not null,
cur_pri number not null,
td_hst_pri number not null,
td_lst_pri number not null,
b1_bst_pri number not null,
s1_bst_pri number not null,
dl_stk_num number not null,
dl_mny_num number not null,
b1_num number not null,
b1_pri number not null,
b2_num number not null,
b2_pri number not null,
b3_num number not null,
b3_pri number not null,
b4_num number not null,
b4_pri number not null,
b5_num number not null,
b5_pri number not null,
s1_num number not null,
s1_pri number not null,
s2_num number not null,
s2_pri number not null,
s3_num number not null,
s3_pri number not null,
s4_num number not null,
s4_pri number not null,
s5_num number not null,
s5_pri number not null,
dl_dt date not null,
dl_tm varchar2(8 byte) not null,
ft_dt date not null
);

/*View of cur_pri 1st diff */
create view curpri_df_vw as 
(select t1.id,
        t2.cur_pri- t1.cur_pri cur_pri_df,
        t2.ft_id
   from stkdat t1,
        stkdat t2
  where t1.id  = t2.id
    and t2.ft_id > t1.ft_id
    and not exists
        (select 'x'
           from stkdat t3
          where t3.id  = t1.id
            and t3.ft_id > t1.ft_id
            and t3.ft_id < t2.ft_id
         )
   and t1.cur_pri > 0
   and t2.cur_pri > 0
  );

/* Need below index to improve performance of curpri_df2_vw*/
create index ft_idd_idx on stkDat (id, ft_id, cur_pri);

  /*View of cur_pri 2nd diff */
create view curpri_df2_vw as 
(select t1.id,
        t2.cur_pri_df- t1.cur_pri_df cur_pri_df2,
        t2.ft_id
   from curpri_df_vw t1,
        curpri_df_vw t2
  where t1.id  = t2.id
    and t2.ft_id > t1.ft_id
    and not exists
        (select 'x'
           from curpri_df_vw t3
          where t3.id  = t1.id
            and t3.ft_id > t1.ft_id
            and t3.ft_id < t2.ft_id
         ) 
  );