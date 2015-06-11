create table usr(
openID varchar2(100 byte) not null,
host_flg number not null,
add_dt date not null,
CONSTRAINT "usr_PK" PRIMARY KEY (OpenID)
);

/* msg received/send*/
create table msg(
msgId number not null,
frmUsrID varchar2(100 byte) not null,
toUsrID varchar2(100 byte) not null,
crtTime number not null,
mstType varchar2(20 byte) not null,
content varchar2(1000 byte) not null,
CONSTRAINT "msg_PK" PRIMARY KEY (msgId)
);

/* monitor stock user id*/
create table monStk(
ID varchar2(6 byte) not null,
openID varchar2 (100 byte) not null,
CONSTRAINT "monStk_PK" PRIMARY KEY (ID, openID)
);

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
1:86.30 ���տ��̼�
2:86.31 �������̼�
3:84.41 ��ǰ�۸�
4:86.30 ������߼�
5:83.70 ������ͼ�
6:84.38 ��һ
7:84.40 ��һ
8:156070902 �ɽ���
9:13235768984 �ɽ���
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

/* stkDat2 stores data converted from stkdat by removing duplicates*/
create table stkDat2(
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
dl_dt date not null
);
create index stkdat2_id_dldt_idx on stkdat2 (id, ft_id, dl_dt)

/* get day highest stock*/
select lst.id from stkdat2 fst, stkdat2 lst
   where to_char(lst.dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd')
     and to_char(lst.dl_dt, 'yyyy-mm-dd') = to_char(fst.dl_dt, 'yyyy-mm-dd')
     and fst.id = lst.id
     and fst.ft_id = (select min(ft_id) from stkdat2 d1 where d1.id = lst.id and to_char(d1.dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd'))
     and lst.ft_id = (select max(ft_id) from stkdat2 d2 where d2.id = lst.id and to_char(d2.dl_dt, 'yyyy-mm-dd') = to_char(sysdate, 'yyyy-mm-dd'))
     and (lst.cur_pri - fst.td_opn_pri)/decode(lst.td_opn_pri, 0, 1, lst.td_opn_pri) > 0.08
     and lst.td_opn_pri > 0

/*usrStk stores user intrested stocks */
create table usrStk(
id varchar2(6 byte) not null,
usrID varchar2(100 byte) not null,
actTyp varchar2(5 byte) not null,
CONSTRAINT "usrStk_PK" PRIMARY KEY (id, usrID)
);
/* stkDDF stores data diffs*/
create table stkDDF(
ft_id number not null,
gap number not null,
id varchar2(6 byte) not null,
cur_pri_df number not null,
dl_stk_num_df number not null,
dl_mny_num_df number not null,
b1_num_df number not null,
b1_pri_df number not null,
b2_num_df number not null,
b2_pri_df number not null,
b3_num_df number not null,
b3_pri_df number not null,
b4_num_df number not null,
b4_pri_df number not null,
b5_num_df number not null,
b5_pri_df number not null,
s1_num_df number not null,
s1_pri_df number not null,
s2_num_df number not null,
s2_pri_df number not null,
s3_num_df number not null,
s3_pri_df number not null,
s4_num_df number not null,
s4_pri_df number not null,
s5_num_df number not null,
s5_pri_df number not null,
dl_dt date not null,
CONSTRAINT "stkddf_PK" PRIMARY KEY (ft_id, gap, id)
);



create index stkdff_id_dldt_idx on stkddf (id, dl_dt)

/*View of 1st diff */
create view dfs_vw as 
(select t1.id,
        t2.cur_pri- t1.cur_pri cur_pri_df,
        t2.ft_id
   from stkdat2 t1,
        stkdat2 t2
  where t1.id  = t2.id
    and t2.ft_id > t1.ft_id
    and not exists
        (select 'x'
           from stkdat2 t3
          where t3.id  = t1.id
            and t3.ft_id > t1.ft_id
            and t3.ft_id < t2.ft_id
         )
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