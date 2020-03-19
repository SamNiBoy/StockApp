create table if not exists usr(
openID varchar(100 ) not null,
host_flg int not null,
add_dt date not null,
mail varchar(100 ),
buy_sell_enabled int not null,
suggest_stock_enabled int not null,
CONSTRAINT usr_PK PRIMARY KEY (OpenID)
);

create table if not exists usrStk(
openID varchar(100 ) not null,
id varchar(6 ) not null,
gz_flg int not null,
sell_mode_flg int not null,
suggested_by varchar(100 ) not null,
add_dt date not null,
CONSTRAINT usrStk_PK PRIMARY KEY (OpenID, id)
);

/* msg received/send*/
create table if not exists msg(
msg_id int not null primary key,
msgId varchar(50) not null,
frmUsrID varchar(100 ) not null,
toUsrID varchar(100 ) not null,
crtTime int not null,
mstType varchar(20 ) not null,
content varchar(1000 ) not null
);

/* monitor stock user id*/
create table if not exists monStk(
ID varchar(6 ) not null,
openID varchar (100 ) not null,
CONSTRAINT monStk_PK PRIMARY KEY (ID, openID)
);

create table if not exists stk(
id varchar(6 ) not null primary key,
area varchar(2 ) not null,
name varchar(20 ) not null,
py varchar(4 ),
bu varchar(12 )
);

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
create table if not exists stkDat(
ft_id int not null primary key,
id varchar(6 ) not null,
td_opn_pri int not null,
yt_cls_pri int not null,
cur_pri int not null,
td_hst_pri int not null,
td_lst_pri int not null,
b1_bst_pri int not null,
s1_bst_pri int not null,
dl_stk_num int not null,
dl_mny_num int not null,
b1_num int not null,
b1_pri int not null,
b2_num int not null,
b2_pri int not null,
b3_num int not null,
b3_pri int not null,
b4_num int not null,
b4_pri int not null,
b5_num int not null,
b5_pri int not null,
s1_num int not null,
s1_pri int not null,
s2_num int not null,
s2_pri int not null,
s3_num int not null,
s3_pri int not null,
s4_num int not null,
s4_pri int not null,
s5_num int not null,
s5_pri int not null,
dl_dt date not null,
dl_tm varchar(8 ) not null,
ft_dt date not null
);

create table if not exists arc_stkDat
(
ft_id int not null primary key,
id varchar(6 ) not null,
td_opn_pri int not null,
yt_cls_pri int not null,
cur_pri int not null,
td_hst_pri int not null,
td_lst_pri int not null,
b1_bst_pri int not null,
s1_bst_pri int not null,
dl_stk_num int not null,
dl_mny_num int not null,
b1_num int not null,
b1_pri int not null,
b2_num int not null,
b2_pri int not null,
b3_num int not null,
b3_pri int not null,
b4_num int not null,
b4_pri int not null,
b5_num int not null,
b5_pri int not null,
s1_num int not null,
s1_pri int not null,
s2_num int not null,
s2_pri int not null,
s3_num int not null,
s3_pri int not null,
s4_num int not null,
s4_pri int not null,
s5_num int not null,
s5_pri int not null,
dl_dt date not null,
dl_tm varchar(8 ) not null,
ft_dt date not null
) TABLESPACE HISDAT;


/* stkDat2 stores data converted from stkdat by removing duplicates*/
create table if not exists stkDat2(
ft_id int not null primary key,
id varchar(6 ) not null,
td_opn_pri decimal(8, 2) not null,
yt_cls_pri decimal(8, 2) not null,
cur_pri decimal(8, 2) not null,
td_hst_pri decimal(8, 2) not null,
td_lst_pri decimal(8, 2) not null,
b1_bst_pri decimal(8, 2) not null,
s1_bst_pri decimal(8, 2) not null,
dl_stk_num int not null,
dl_mny_num int not null,
b1_num int not null,
b1_pri decimal(8, 2) not null,
b2_num int not null,
b2_pri decimal(8, 2) not null,
b3_num int not null,
b3_pri decimal(8, 2) not null,
b4_num int not null,
b4_pri decimal(8, 2) not null,
b5_num int not null,
b5_pri decimal(8, 2) not null,
s1_num int not null,
s1_pri decimal(8, 2) not null,
s2_num int not null,
s2_pri decimal(8, 2) not null,
s3_num int not null,
s3_pri decimal(8, 2) not null,
s4_num int not null,
s4_pri decimal(8, 2) not null,
s5_num int not null,
s5_pri decimal(8, 2) not null,
dl_dt date not null
);


create table if not exists arc_stkDat2
(
ft_id int not null primary key,
id varchar(6 ) not null,
td_opn_pri int not null,
yt_cls_pri int not null,
cur_pri int not null,
td_hst_pri int not null,
td_lst_pri int not null,
b1_bst_pri int not null,
s1_bst_pri int not null,
dl_stk_num int not null,
dl_mny_num int not null,
b1_num int not null,
b1_pri int not null,
b2_num int not null,
b2_pri int not null,
b3_num int not null,
b3_pri int not null,
b4_num int not null,
b4_pri int not null,
b5_num int not null,
b5_pri int not null,
s1_num int not null,
s1_pri int not null,
s2_num int not null,
s2_pri int not null,
s3_num int not null,
s3_pri int not null,
s4_num int not null,
s4_pri int not null,
s5_num int not null,
s5_pri int not null,
dl_dt date not null
) TABLESPACE HISDAT;

create table if not exists stkDlyInfo(
id varchar(6 ) not null,
dt varchar(10 ) not null,
yt_cls_pri int not null,
td_cls_pri int not null,
td_opn_pri int not null,
td_hst_pri int not null,
td_lst_pri int not null,
dl_stk_num int not null,
dl_mny_num int not null,
CONSTRAINT stkDlyInfo PRIMARY KEY (id, dt)
);

/* stkDDF stores data diffs*/
create table if not exists stkDDF(
ft_id int not null,
gap int not null,
id varchar(6 ) not null,
cur_pri_df int not null,
dl_stk_num_df int not null,
dl_mny_num_df int not null,
b1_num_df int not null,
b1_pri_df int not null,
b2_num_df int not null,
b2_pri_df int not null,
b3_num_df int not null,
b3_pri_df int not null,
b4_num_df int not null,
b4_pri_df int not null,
b5_num_df int not null,
b5_pri_df int not null,
s1_num_df int not null,
s1_pri_df int not null,
s2_num_df int not null,
s2_pri_df int not null,
s3_num_df int not null,
s3_pri_df int not null,
s4_num_df int not null,
s4_pri_df int not null,
s5_num_df int not null,
s5_pri_df int not null,
dl_dt date not null,
CONSTRAINT stkddf_PK PRIMARY KEY (ft_id, gap, id)
);

create table if not exists stkPriStat(
id varchar(6 ) not null primary key,
lst_pri int not null,
hst_pri int not null,
c1 int not null,
c2 int not null,
c3 int not null,
c4 int not null,
c5 int not null,
c6 int not null,
c7 int not null,
c8 int not null,
c9 int not null,
c10 int not null,
add_dt date not null
);

create table if not exists CashAcnt(
acntId varchar(20 ) not null,
init_mny int not null,
used_mny int not null,
pft_mny int,
split_num int not null,
max_useable_pct int not null,
dft_acnt_flg int not null,
add_dt date not null,
CONSTRAINT CashAcnt_PK PRIMARY KEY (acntId, dft_acnt_flg)
);

create table if not exists arc_CashAcnt(
acntId varchar(20 ) not null,
init_mny int not null,
used_mny int not null,
pft_mny int,
split_num int not null,
max_useable_pct int not null,
dft_acnt_flg int not null,
add_dt date not null
);

insert into cashacnt values('testCashAct001',20000,0,0,4,0.5,1,sysdate());
  
create table if not exists TradeHdr(
acntId varchar(20 ) not null,
stkId varchar(6 ) not null,
pft_mny int not null, /* the money of your stock */
in_hand_qty int not null,
pft_price int not null,
add_dt date not null,
CONSTRAINT TradeHdr_PK PRIMARY KEY (acntId, stkId)
);

create table if not exists arc_TradeHdr(
acntId varchar(20 ) not null,
stkId varchar(6 ) not null,
pft_mny int not null, /* the money of your stock */
in_hand_qty int not null,
pft_price int not null,
add_dt date not null
);

create table if not exists TradeDtl(
acntId varchar(20 ) not null,
stkId varchar(6 ) not null,
seqnum int not null,
price int not null,
amount int not null,
dl_dt date not null,
buy_flg int not null,
CONSTRAINT TradeDtl_PK PRIMARY KEY (acntId, stkId, seqnum)
);

create table if not exists arc_TradeDtl(
acntId varchar(20 ) not null,
stkId varchar(6 ) not null,
seqnum int not null,
price int not null,
amount int not null,
dl_dt date not null,
buy_flg int not null
);

create table if not exists SellBuyRecord(
sb_id int not null primary key,
openId varchar(30 ) not null,
stkId varchar(6 ) not null,
price int not null,
qty int not null,
buy_flg int not null,
dl_dt date not null
);

create table if not exists arc_SellBuyRecord(
sb_id int not null primary key,
openId varchar(30 ) not null,
stkId varchar(6 ) not null,
price int not null,
qty int not null,
buy_flg int not null,
dl_dt date not null
);

create index stkdat2_id_dldt_idx on stkDat2 (id, ft_id, dl_dt);
create index stkdat2_idx3 on stkDat2 (id, ft_id, cur_pri, yt_cls_pri,dl_stk_num);
create index stkdff_id_dldt_idx on stkDDF (id, dl_dt);
/* Need below index to improve performance of curpri_df2_vw*/
create index ft_idd_idx on stkDat (id, ft_id, cur_pri);