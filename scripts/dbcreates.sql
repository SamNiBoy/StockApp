create table usr(
openID varchar2(100 byte) not null,
host_flg number not null,
add_dt date not null,
mail varchar2(100 byte),
buy_sell_enabled number not null,
CONSTRAINT "usr_PK" PRIMARY KEY (OpenID)
);

create table usrStk(
openID varchar2(100 byte) not null,
id varchar2(6 byte) not null,
gz_flg number not null,
add_dt date not null,
CONSTRAINT "usrStk_PK" PRIMARY KEY (OpenID, id)
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
name varchar2(20 byte) not null,
py varchar2(4 byte),
bu varchar2(12 byte),
gz_flg number not null
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
create index stkdat2_idx3 on stkdat2 (id, ft_id, to_char(dl_dt, 'yyyy-mm-dd'), cur_pri, yt_cls_pri,dl_stk_num);


create table stkDlyInfo(
id varchar2(6 byte) not null,
dt varchar2(10 byte) not null,
yt_cls_pri number not null,
td_cls_pri number not null,
td_opn_pri number not null,
td_hst_pri number not null,
td_lst_pri number not null,
dl_stk_num number not null,
dl_mny_num number not null,
CONSTRAINT "stkDlyInfo" PRIMARY KEY (id, dt)
);
/*create stkDlyInfo data*/
insert into stkDlyInfo (select id, 
        	                     to_char(dl_dt, 'yyyy-mm-dd'),  
        	                     yt_cls_pri,
        	                     cur_pri,
                               td_opn_pri,
                               td_hst_pri,
                               td_lst_pri,
                               dl_stk_num,
                               dl_mny_num
        	   from stkdat2  
        	   where not exists (select 'x' from stkDlyInfo scp where scp.id = stkdat2.id and to_char(stkdat2.dl_dt,'yyyy-mm-dd') = scp.dt) 
               and not exists (select 'x' from stkdat2 s2 where s2.id = stkdat2.id  and s2.ft_id > stkdat2.ft_id and to_char(s2.dl_dt,'yyyy-mm-dd') = to_char(stkdat2.dl_dt,'yyyy-mm-dd'))
             ); 



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

create table stkPriStat(
id varchar2(6 byte) not null primary key,
lst_pri number not null,
hst_pri number not null,
c1 number not null,
c2 number not null,
c3 number not null,
c4 number not null,
c5 number not null,
c6 number not null,
c7 number not null,
c8 number not null,
c9 number not null,
c10 number not null,
add_dt date not null
);

insert into stkPriStat 
select s2.id,
       t.lst_pri,
       t.hst_pri,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 1.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c1,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 2.0/ 10 and s2.cur_pri > t.lst_pri + (t.hst_pri - t.lst_pri) * 1.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c2,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 3.0/ 10 and s2.cur_pri > t.lst_pri + (t.hst_pri - t.lst_pri) * 2.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c3,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 4.0/ 10 and s2.cur_pri > t.lst_pri + (t.hst_pri - t.lst_pri) * 3.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c4,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 5.0/ 10 and s2.cur_pri > t.lst_pri + (t.hst_pri - t.lst_pri) * 4.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c5,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 6.0/ 10 and s2.cur_pri > t.lst_pri + (t.hst_pri - t.lst_pri) * 5.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c6,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 7.0/ 10 and s2.cur_pri > t.lst_pri + (t.hst_pri - t.lst_pri) * 6.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c7,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 8.0/ 10 and s2.cur_pri > t.lst_pri + (t.hst_pri - t.lst_pri) * 7.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c8,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 9.0/ 10 and s2.cur_pri > t.lst_pri + (t.hst_pri - t.lst_pri) * 8.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c9,
       sum(case when s2.cur_pri < t.lst_pri + (t.hst_pri - t.lst_pri) * 10.0/ 10 and s2.cur_pri > t.lst_pri + (t.hst_pri - t.lst_pri) * 9.0/ 10 then 1 else 0 end * (s2.dl_stk_num - s1.dl_stk_num)) c10,
       sysdate
  from stkdat2 s2, (select id, min(cur_pri) lst_pri, max(cur_pri) hst_pri from stkdat2 sx group by sx.id) t,
       stkdat2 s1
 where s2.id = s1.id
   and s2.ft_id > s1.ft_id
   and to_char(s2.dl_dt,'yyyy-mm-dd') = to_char(s1.dl_dt,'yyyy-mm-dd')
   and not exists (select 'x' from stkdat2 ss where ss.id = s2.id and ss.ft_id > s1.ft_id and ss.ft_id < s2.ft_id)
   and s2.id = t.id 
group by s2.id, t.lst_pri, t.hst_pri


create table CashAcnt(
acntId varchar2(20 byte) not null primary key,
init_mny number not null,
used_mny number not null,
pft_mny number,
split_num number not null,
max_useable_pct number not null,
dft_acnt_flg number not null,
add_dt date not null
);

insert into cashacnt values('testCashAct001',20000,0,0,4,0.5,1,sysdate);
  
create table TradeHdr(
acntId varchar2(20 byte) not null,
stkId varchar2(6 byte) not null,
pft_mny number not null, /* the money of your stock */
in_hand_qty number not null,
pft_price number not null,
add_dt date not null,
CONSTRAINT "TradeHdr_PK" PRIMARY KEY (acntId, stkId)
);

create table TradeDtl(
acntId varchar2(20 byte) not null,
stkId varchar2(6 byte) not null,
seqnum number not null,
price number not null,
amount number not null,
dl_dt date not null,
buy_flg number not null,
CONSTRAINT "TradeDtl_PK" PRIMARY KEY (acntId, stkId, seqnum)
);

