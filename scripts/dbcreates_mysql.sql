create table if not exists param(
name varchar(50 ) not null,
cat varchar(50 ) not null,
intval int,
fltval decimal(10, 4),
str1 varchar(200),
str2 varchar(200),
comment varchar(500),
add_dt datetime not null,
mod_dt datetime not null,
CONSTRAINT param_name_PK PRIMARY KEY (name)
);

insert into param values('DFT_INIT_MNY', 'ACCOUNT',null, 50000, '', '', 'Default initial account money', sysdate(),sysdate());
insert into param values('DFT_MAX_USE_PCT', 'ACCOUNT',null, 0.8, '', '', 'Max percentage of money can be used.', sysdate(),sysdate());
insert into param values('DFT_MAX_MNY_PER_TRADE', 'ACCOUNT',null, 10000, '', '', 'Max amount of money can be used for one trade.', sysdate(),sysdate());
insert into param values('ACNT_SIM_PREFIX', 'ACCOUNT',null,null , 'SIM', '', 'Account prefix for simulation account.', sysdate(),sysdate());
insert into param values('ACNT_GF_PREFIX', 'ACCOUNT',null,null , 'GF', '', 'Account prefix for GF trader.', sysdate(),sysdate());

insert into param values('COMMISSION_RATE', 'VENDOR',null, 0.0014, '', '', 'Commissioin rate, as part of cost.', sysdate(),sysdate());

insert into param values('SIM_DAYS', 'SIMULATION', 1,null, '', '', 'How many days data for simulation.', sysdate(),sysdate());
insert into param values('SIM_THREADS_COUNT', 'SIMULATION', 2,null, '', '', 'How many thread in parallel to run the simulation.', sysdate(),sysdate());
insert into param values('SIM_STOCK_COUNT_FOR_EACH_THREAD', 'SIMULATION', 10,null, '', '', 'How many stocks to be run simulation per thread at one time.', sysdate(),sysdate());

insert into param values('ARCHIVE_DAYS_OLD', 'ARCHIVE', 15,null, '', '', 'How many days stock data to keep in prod db.', sysdate(),sysdate());
insert into param values('PURGE_DAYS_OLD', 'ARCHIVE', 30,null, '', '', 'How many days stock data to keep in archive db.', sysdate(),sysdate());

insert into param values('MAX_TRADE_TIMES_BUY_OR_SELL_PER_STOCK', 'TRADING', 100,null, '', '', 'Max number of buy/sell per stock each day.', sysdate(),sysdate());
insert into param values('MAX_TRADE_TIMES_PER_STOCK', 'TRADING', 500,null, '', '', 'Max number of trading per stock each day.', sysdate(),sysdate());
insert into param values('MAX_TRADE_TIMES_PER_DAY', 'TRADING', 5000,null, '', '', 'Max number of times total trade a day allowed.', sysdate(),sysdate());
insert into param values('HOUR_TO_KEEP_BALANCE', 'TRADING', 14,null, '', '', 'At which hour the market is going to close, so keep balance.', sysdate(),sysdate());
insert into param values('MINUTE_TO_KEEP_BALANCE', 'TRADING', 57,null, '', '', 'At which minute the market is going to close, so keep balance.', sysdate(),sysdate());
insert into param values('STOP_TRADE_IF_LOST_MORE_THAN_GAIN_TIMES', 'TRADING', 3,null, '', '', 'Stop trade if same stock lost than gain this times', sysdate(),sysdate());
insert into param values('SUGGESTED_BY_FOR_USER', 'TRADING',null,null , 'osCWfs-ZVQZfrjRK0ml-eEpzeop0', '', 'This is Same Ni WeChat account.', sysdate(),sysdate());
insert into param values('SYSTEM_ROLE_FOR_SUGGEST_AND_GRANT', 'TRADING',null,null , 'SYSTEM_SUGGESTER', 'SYSTEM_GRANTED_TRADER', 'SYSTEM means system recommand the stock but not enabled for trading, SYSTEMGRANTED means enabled trading', sysdate(),sysdate());
insert into param values('MAX_LOST_TIME_BEFORE_EXIT_TRADE', 'TRADING', 3,null, '', '', 'The threshold value for stoping trading the stock if it lost this number of times.', sysdate(),sysdate());
insert into param values('MAX_DAYS_WITHOUT_TRADE_BEFORE_EXIT_TRADE', 'TRADING', 7,null, '', '', 'Exit trade if no trading happened this number of days.', sysdate(),sysdate());
insert into param values('MAX_LOST_PCT_FOR_SELL_MODE', 'TRADING',null, -0.06, '', '', 'When lost this percentage, put to sell mode.', sysdate(),sysdate());
insert into param values('MAX_GAIN_PCT_FOR_DISABLE_SELL_MODE', 'TRADING', null,0.06, '', '', 'Put back stock for trade if stock price goes this high percentage.', sysdate(),sysdate());


insert into param values('GZ_STOCK2_QUEUE_SIZE', 'TRADING',360,null , '', '', 'Defined how big the queue size for GZ Stock2 object to calculate statistics, this is critial param, if you fetch every one second, then it stores one minute data.', sysdate(),sysdate());
insert into param values('ALL_STOCK2_QUEUE_SIZE', 'TRADING',60,null , '', '', 'Defined how big the queue size for ALL Stock2 object to calculate statistics, this is critial param, if you fetch every one second, then it stores one minute data.', sysdate(),sysdate());
insert into param values('FETCH_EVERY_SECONDS', 'TRADING',10,null , '', '', 'Define how often we fetch stock data for all stocks as well as for gzed stocks.', sysdate(),sysdate());

insert into param values('VOLUME_PLUS_PCT', 'TRADING',null,0.5, '', '', 'Define the last delta trading volume is this above the pct of delta volumes in the queue then it means volume plused.', sysdate(),sysdate());
insert into param values('BUY_SELL_MAX_DIFF_CNT', 'TRADING', 2,null, '', '', 'Max extra times between buy and sell for same stock.', sysdate(),sysdate());
insert into param values('MAX_MINUTES_ALLOWED_TO_KEEP_BALANCE', 'TRADING', 60,null, '', '', 'How many minutes in maximum we need to buy/sell stock back for keep balance.', sysdate(),sysdate());
insert into param values('STOP_BREAK_BALANCE_IF_CURPRI_REACHED_PCT', 'TRADING',null, 0.06, '', '', 'If delta price go above this percentage, stop trading for breaking balance.', sysdate(),sysdate());
insert into param values('BUY_BASE_TRADE_THRESH', 'TRADING',null, 0.03, '', '', 'QtyBuyPointSelector: Stock min/max price must be bigger than this threshold value for trading.', sysdate(),sysdate());
insert into param values('SELL_BASE_TRADE_THRESH', 'TRADING',null, 0.03, '', '', 'QtySellPointSelector: Stock min/max price must be bigger than this threshold value for trading.', sysdate(),sysdate());
insert into param values('MARGIN_PCT_TO_TRADE_THRESH', 'TRADING',null, 0.2, '', '', 'How close to the margin of BASE_TRADE_THRESHOLD value.', sysdate(),sysdate());

insert into param values('NUM_STOCK_TO_SUGGEST', 'SUGGESTER',100,null, '', '', 'How many stock to suggest at one time.', sysdate(),sysdate());
insert into param values('MIN_JUMP_TIMES_FOR_GOOD_STOCK', 'SUGGESTER',10,null , '', '', 'Stock suggester param to define at min how many times the stock should cross price high/low areas for suggestion.', sysdate(),sysdate());
insert into param values('MIN_SHAKING_PCT', 'SUGGESTER',null,0.06, '', '', 'Stock suggester param to define min percentage the stokc price must be shaking for suggestion.', sysdate(),sysdate());
insert into param values('PCT_BUYSELL_THRESH_DIFF_DISABLE_TRADE', 'SUGGESTER',null,0.04, '', '', 'Do not trading on stock when the buy/sell treshold values are very different.', sysdate(),sysdate());


insert into param values('TRADING_AT_LOCAL', 'TRADING',1,null, '', '', 'Do trading at local with GF trader, other then send to Tradex trading system.', sysdate(),sysdate());
insert into param values('TRADING_AT_LOCAL_WITH_SIM', 'TRADING',1,null, '', '', 'Do trading at local with in simulation mode, other then send to Tradex trading system or with GF trader.', sysdate(),sysdate());

insert into param values('NUM_STOCK_IN_TRADE', 'TRADING',10,null, '', '', 'Do you want how many stocks in trading for suggest stock job to maintain.', sysdate(),sysdate());

insert into param values('GENERATION_SIZE', 'GAPARAM',20,null, '', '', 'How many entities per generation for GA algorithm.', sysdate(),sysdate());
insert into param values('MAX_TRAIN_LOOP', 'GAPARAM',5,null, '', '', 'How many iterations to train the GA parameters.', sysdate(),sysdate());
insert into param values('TOPN', 'GAPARAM',5,null, '', '', 'Keep Top N entites for mutation and crossover.', sysdate(),sysdate());
insert into param values('MAX_TRAIN_THREAD_CNT', 'GAPARAM',5,null, '', '', 'How many threads to do GA parameter trainning.', sysdate(),sysdate());


create table if not exists stockIndex(
indexid varchar(20 ) not null,
id int,
indexval decimal(10, 4),
delindex decimal(10, 4),
deltapct decimal(10, 4),
delamt int,
delmny decimal(20, 4),
add_dt datetime not null,
CONSTRAINT stockIndex_PK PRIMARY KEY (indexid, id)
);


create table if not exists pendingTrade(
stock varchar(20 ) not null,
id int,
qty int,
price decimal(10, 4),
success_qty int,
success_price decimal(10, 4),
status varchar(10) not null, //N: New, C: Cancelled, S:Success, P:Partial 
order_id varchar(20), 
is_buy_flg int,
add_dt datetime not null,
mod_dt datetime not null,
CONSTRAINT pendingTrade_PK PRIMARY KEY (stock, id)
);

create table if not exists stockParam(
stock varchar(20 ) not null,
name varchar(50 ) not null,
cat varchar(50 ) not null,
intval int,
fltval decimal(10, 4),
str1 varchar(200),
str2 varchar(200),
comment varchar(500),
add_dt datetime not null,
mod_dt datetime not null,
CONSTRAINT stock_param_name_PK PRIMARY KEY (stock, name)
);

create table if not exists stockParamSearch(
stock varchar(20 ) not null,
id int,
score decimal(10, 4),
param_as_string varchar(500),
add_dt datetime not null,
CONSTRAINT stock_param_search_PK PRIMARY KEY (stock, id)
);

create table if not exists param(
name varchar(50 ) not null,
cat varchar(50 ) not null,
intval int,
fltval decimal(10, 4),
str1 varchar(200),
str2 varchar(200),
comment varchar(500),
add_dt datetime not null,
mod_dt datetime not null,
CONSTRAINT param_name_PK PRIMARY KEY (name)
);


create table if not exists usr(
openID varchar(100 ) not null,
host_flg int not null,
add_dt datetime not null,
mail varchar(100 ),
buy_sell_enabled int not null,
suggest_stock_enabled int not null,
client_id varchar(20), 
client_pwd varchar(20),
trade_unit varchar(20),
CONSTRAINT usr_PK PRIMARY KEY (OpenID)
);

update usr set client_id = '5001093', client_pwd = 'Admin@12345', trade_unit = '6001045' where openID = 'osCWfs-ZVQZfrjRK0ml-eEpzeop0';

create table if not exists usrStk(
openID varchar(100 ) not null,
id varchar(6 ) not null,
gz_flg int not null,
stop_trade_mode_flg int not null,
suggested_by varchar(100 ) not null,
suggested_by_selector varchar(100 ) not null,
suggested_comment varchar(500 ) not null,
add_dt datetime not null,
mod_dt datetime not null,
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
dl_stk_num bigint not null,
dl_mny_num decimal(20, 2) not null,
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
dl_stk_num bigint not null,
dl_mny_num decimal(20, 2) not null,
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
dl_dt datetime not null
);

/* stkDat2 stores data converted from stkdat by removing duplicates*/
create table if not exists arc_stkDat2(
ft_id int not null primary key,
id varchar(6 ) not null,
td_opn_pri decimal(8, 2) not null,
yt_cls_pri decimal(8, 2) not null,
cur_pri decimal(8, 2) not null,
td_hst_pri decimal(8, 2) not null,
td_lst_pri decimal(8, 2) not null,
b1_bst_pri decimal(8, 2) not null,
s1_bst_pri decimal(8, 2) not null,
dl_stk_num bigint not null,
dl_mny_num decimal(20, 2) not null,
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
dl_dt datetime not null
);


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
dl_dt datetime not null,
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
add_dt datetime not null
);

create table if not exists CashAcnt(
acntId varchar(20 ) not null,
init_mny decimal(8, 2) not null,
used_mny decimal(8, 2) not null,
used_mny_hrs decimal(8, 2) not null,
pft_mny decimal(8, 2),
max_mny_per_trade decimal(8, 2) not null,
max_useable_pct decimal(8, 2) not null,
add_dt datetime not null,
CONSTRAINT CashAcnt_PK PRIMARY KEY (acntId)
);

create table if not exists TradeHdr(
acntId varchar(20 ) not null,
stkId varchar(6 ) not null,
in_hand_stk_mny decimal(8, 2) not null,
in_hand_qty int not null,
in_hand_stk_price decimal(8, 2) not null,
total_amount decimal(20, 2),
com_rate decimal(8, 4),
commission_mny decimal(8, 2),
add_dt datetime not null,
CONSTRAINT TradeHdr_PK PRIMARY KEY (acntId, stkId)
);

create table if not exists TradeDtl(
acntId varchar(20 ) not null,
stkId varchar(6 ) not null,
seqnum int not null,
price decimal(8, 2) not null,
amount int not null,
dl_dt datetime not null,
buy_flg int not null,
order_id int,
trade_selector_name varchar(50) not null,
trade_selector_comment varchar(500) not null,
CONSTRAINT TradeDtl_PK PRIMARY KEY (acntId, stkId, seqnum)
);


create table if not exists arc_CashAcnt(
acntId varchar(20 ) not null,
init_mny decimal(8, 2) not null,
used_mny decimal(8, 2) not null,
used_mny_hrs decimal(8, 2) not null,
pft_mny decimal(8, 2),
max_mny_per_trade decimal(8, 2) not null,
max_useable_pct decimal(8, 2) not null,
add_dt datetime not null,
CONSTRAINT arc_CashAcnt_PK PRIMARY KEY (acntId)
);

create table if not exists arc_TradeHdr(
acntId varchar(20 ) not null,
stkId varchar(6 ) not null,
in_hand_stk_mny decimal(8, 2) not null,
in_hand_qty int not null,
in_hand_stk_price decimal(8, 2) not null,
total_amount decimal(20, 2),
com_rate decimal(8, 4),
commission_mny decimal(8, 2),
add_dt datetime not null,
CONSTRAINT arc_TradeHdr_PK PRIMARY KEY (acntId, stkId)
);

create table if not exists arc_TradeDtl(
acntId varchar(20 ) not null,
stkId varchar(6 ) not null,
seqnum int not null,
price decimal(8, 2) not null,
amount int not null,
dl_dt datetime not null,
buy_flg int not null,
order_id int,
trade_selector_name varchar(50) not null,
trade_selector_comment varchar(500) not null,
CONSTRAINT arc_TradeDtl_PK PRIMARY KEY (acntId, stkId, seqnum)
);

create table if not exists SellBuyRecord(
stkId varchar(6 ) not null primary key,
price decimal(8, 2) not null,
qty int not null,
buy_flg int not null,
dl_dt datetime not null
);

create table if not exists arc_SellBuyRecord(
stkId varchar(6 ) not null primary key,
price int not null,
qty int not null,
buy_flg int not null,
dl_dt datetime not null
);

create table if not exists SimResult(
strategy_name varchar(50) not null,
add_dt datetime not null,
acnt_traded int not null,
totUsedMny decimal(10, 2) not null,
totUsedMny_Hrs decimal(8, 2) not null,
avgPft decimal(8, 2) not null,
totPft decimal(8, 2) not null,
tot_commission_mny decimal(8, 2) not null,
netPft decimal(8, 2) not null,
fundPftPct decimal(15, 5) not null,
buyCnt int not null,
sellCnt int not null,
CONSTRAINT CashAcnt_PK PRIMARY KEY (strategy_name, add_dt)
);

create index stkdat2_id_dldt_idx on stkDat2 (id, ft_id, dl_dt);
create index stkdat2_idx3 on stkDat2 (id, ft_id, cur_pri, yt_cls_pri,dl_stk_num);
create index stkdat2_dldt on stkDat2 (dl_dt);
create index arc_stkdat2_id_dldt_idx on arc_stkDat2 (id, ft_id, dl_dt);
create index arc_stkdat2_idx3 on arc_stkDat2 (id, ft_id, cur_pri, yt_cls_pri,dl_stk_num);
//create index stkdat2_idx4 on stkDat2 (dl_dt, id, ft_id, td_opn_pri, cur_pri, dl_mny_num, dl_stk_num, yt_cls_pri, td_hst_pri, td_lst_pri, b1_num, b1_pri, s1_num, s1_pri);
create index stkdff_id_dldt_idx on stkDDF (id, dl_dt);
/* Need below index to improve performance of curpri_df2_vw*/
create index ft_idd_idx on stkDat (id, ft_id, cur_pri);