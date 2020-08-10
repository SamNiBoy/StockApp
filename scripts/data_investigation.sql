//find out the percentage of stock hst/lst price range:
select count(*) cnt, lvl from (select case when (hst_pri - lst_pri) / yt_cls_pri < 0.01 then 1
when (hst_pri - lst_pri) / yt_cls_pri < 0.02 then 2
when (hst_pri - lst_pri) / yt_cls_pri < 0.03 then 3
when (hst_pri - lst_pri) / yt_cls_pri < 0.04 then 4
when (hst_pri - lst_pri) / yt_cls_pri < 0.05 then 5
when (hst_pri - lst_pri) / yt_cls_pri < 0.06 then 6
when (hst_pri - lst_pri) / yt_cls_pri < 0.07 then 7
when (hst_pri - lst_pri) / yt_cls_pri < 0.08 then 8
when (hst_pri - lst_pri) / yt_cls_pri < 0.09 then 9
when (hst_pri - lst_pri) / yt_cls_pri < 0.1 then 10
when (hst_pri - lst_pri) / yt_cls_pri > 0.1 then 11
end lvl
from (select max(td_hst_pri) hst_pri, min(td_lst_pri) lst_pri, max(yt_cls_pri) yt_cls_pri, id from stkdat2 where left(dl_dt, 10) = '2020-05-07' group by id) t)t2 group by lvl;


//Find how many pct stoks inc/dec after previous day increase 9%
select case when pct > 0 then 1 when pct < 0 then -1 else 0 end cat, 
count(*) cnt,
avg(pct) avg_pct,
dte
from (select s1.cur_pri, s1.yt_cls_pri, (s1.cur_pri - s1.yt_cls_pri) / s1.yt_cls_pri pct, left(s1.dl_dt, 10) dte from stkdat2 s1
join (select max(ft_id) max_ft_id, id, left(dl_dt, 10) mydte from stkdat2 group by id, left(dl_dt, 10)) t
 on s1.id = t.id
 and s1.ft_id = t.max_ft_id
join stkdat2 s2 
on s2.id = s1.id 
and left(s2.dl_dt, 10) = left(s1.dl_dt - interval 1 day, 10)
join (select max(ft_id) max_ft_id, id, left(dl_dt, 10) mydte from stkdat2 group by id, left(dl_dt, 10)) t2
  on left(s2.dl_dt, 10) = t2.mydte
and s2.id = t2.id
and s2.ft_id = t2.max_ft_id
where ((s2.cur_pri - s2.yt_cls_pri)/s2.yt_cls_pri > 0.09)) t
group by case when pct > 0 then 1 when pct < 0 then -1 else 0 end, dte;


//find b1_num, s1_num 10 times bigger stock:
select s1.* from (select k.name, k.id, max(b1_num) mx_b1_num, max(s1_num) mx_s1_num, left(dl_dt, 10) dte1
from stkdat2 s 
join stk k
on s.id = k.id 
group by k.name, k.id, left(dl_dt, 10)) s1
join (select max(b1_num) mx_b1_num, max(s1_num) mx_s1_num, id, left(dl_dt, 10) dte2 from stkdat2 group by id, left(dl_dt, 10)) s2
on s1.id = s2.id
and s1.dte1 > s2.dte2
and not exists (select 'x' from (select max(b1_num) mx_b1_num, max(s1_num) mx_s1_num, id, left(dl_dt, 10) dte3 from stkdat2 group by id, left(dl_dt, 10)) s3 where s3.id = s1.id and s3.dte3 < s1.dte1 and s3.dte3 > s2.dte2)
where s1.mx_b1_num > 10 * s2.mx_b1_num and s1.mx_s1_num > 10 * s2.mx_s1_num;

//Find stock raised most from stkdat2 table:

select stk.id, stk.name, (s1.cur_pri - s2.cur_pri) / s2.yt_cls_pri pct 
from stkdat2 s1
join (select id, max(ft_id) mx_ft_id, min(ft_id) mn_ft_id from stkdat2 group by id) t2
on s1.id = t2.id
and s1.ft_id = t2.mx_ft_id
join stkdat2 s2
on s2.id = t2.id
and s2.ft_id = t2.mn_ft_id
join stk on s1.id = stk.id
order by pct desc;

//3 days up, then
select count(1) cnt,
cat,
avg(pct) from (
select s4.id,
	s.name,
       case when s4.cur_pri > s4.yt_cls_pri then 1 else -1 end cat,
	   (s4.cur_pri - s4.yt_cls_pri) / s4.yt_cls_pri pct,
       left(s4.dl_dt, 10) s4dte
from stkdat2 s1
join (select max(ft_id) mx_ft_id, id, left(dl_dt, 10) dt from stkdat2 group by id, left(dl_dt, 10)) t1
  on s1.id = t1.id
 and left(s1.dl_dt, 10) = t1.dt
 and s1.ft_id = t1.mx_ft_id
join stkdat2 s2
on s1.id = s2.id
and left(s1.dl_dt + interval 1 day, 10) = left(s2.dl_dt, 10)
 join (select max(ft_id) mx_ft_id, id, left(dl_dt, 10) dt from stkdat2 group by id, left(dl_dt, 10)) t2
  on s2.id = t2.id
  and left(s2.dl_dt, 10) = t2.dt
  and s2.ft_id = t2.mx_ft_id
join stkdat2 s3
on s1.id = s3.id
and left(s2.dl_dt + interval 1 day, 10) = left(s3.dl_dt, 10)
join (select max(ft_id) mx_ft_id, id, left(dl_dt, 10) dt from stkdat2 group by id, left(dl_dt, 10)) t3
 on s3.id = t3.id
 and left(s3.dl_dt, 10) = t3.dt
  and s3.ft_id = t3.mx_ft_id
join stkdat2 s4
on s1.id =s4.id
and left(s3.dl_dt + interval 1 day, 10) = left(s4.dl_dt, 10)
join (select max(ft_id) mx_ft_id, id, left(dl_dt, 10) dt from stkdat2 group by id, left(dl_dt, 10)) t4
 on s4.id = t4.id
 and left(s4.dl_dt, 10) = t4.dt
  and s4.ft_id = t4.mx_ft_id
join stk s
on s1.id = s.id
where s1.cur_pri > s1.td_opn_pri
  and s2.cur_pri > s2.td_opn_pri
  and s3.cur_pri > s3.td_opn_pri)t
  group by cat;


//golden sql, it return p1, first day increase pct, p2, next day increase pct relationship.
select count(1) cnt,
cat,
round(pct1, 2) p1,
round(pct2, 2) p2 from (
select s2.id,
	s.name,
       case when s1.td_opn_pri > s1.yt_cls_pri then 1 else -1 end cat,
	   (s1.td_opn_pri - s1.yt_cls_pri) / s1.yt_cls_pri pct1,
       (s2.td_opn_pri - s2.yt_cls_pri) / s2.yt_cls_pri pct2,
       left(s2.dl_dt, 10) s2dte
from stkdat2 s1
join (select max(ft_id) mx_ft_id, id, left(dl_dt, 10) dt from stkdat2 group by id, left(dl_dt, 10)) t1
  on s1.id = t1.id
 and left(s1.dl_dt, 10) = t1.dt
 and s1.ft_id = t1.mx_ft_id
-- and exists (select 'x' from stkdat2 tmp where tmp.id =s1.id and left(s1.dl_dt, 10) = left(tmp.dl_dt, 10) and (tmp.cur_pri - tmp.yt_cls_pri) / tmp.yt_cls_pri >= 0.08 and (tmp.cur_pri - tmp.yt_cls_pri) / tmp.yt_cls_pri < 0.09)
join stkdat2 s2
on s1.id = s2.id
and left(s1.dl_dt + interval 1 day, 10) = left(s2.dl_dt, 10)
 join (select max(ft_id) mx_ft_id, id, left(dl_dt, 10) dt from stkdat2 group by id, left(dl_dt, 10)) t2
  on s2.id = t2.id
  and left(s2.dl_dt, 10) = t2.dt
  and s2.ft_id = t2.mx_ft_id
join stk s
on s1.id = s.id) t
  group by cat, round(pct1, 2), round(pct2, 2)
  order by cat, p1, p2;

//investigate 09:25
select distinct left(s1.dl_dt, 16) dl_dt, s1.b1_num, s1.s1_num, s1.b1_num/s1.s1_num rt, (s2.cur_pri - s2.td_opn_pri) / s2.yt_cls_pri pct, s1.b1_pri, s1.s1_pri,  s2.cur_pri
from stkdat2 s1
join stkdat2 s2
  on s1.id = s2.id
 and right(left(s1.dl_dt, 16), 5) = '09:25'
and left(s1.dl_dt, 10) = left(s2.dl_dt, 10)
join (select max(ft_id) max_ft_id, id, left(dl_dt, 10) dte from stkdat2 where id = '603439' group by left(dl_dt, 10), id ) t
  on s2.id = t.id
and left(s2.dl_dt, 10) = t.dte
and s2.ft_id = t.max_ft_id
 where s1.id = '603439'
  -- and left(s1.dl_dt, 10) = '2020-06-10'
 order by dl_dt desc;

//daily detail.
select s1.dl_dt, s1.b1_num, s1.s1_num, s1.b1_pri, s1.s1_pri, s1.cur_pri, (s1.cur_pri - s1.td_opn_pri) /s1.td_opn_pri pct_td_opn_pri, (s1.cur_pri - s1.yt_cls_pri) /s1.yt_cls_pri pct_yt_cls_pri, s2.dl_stk_num - s1.dl_stk_num dtl_stk_num,
s2.dl_mny_num - s1.dl_mny_num dlt_mny_num
from stkdat2 s1
join stkdat2 s2
  on s1.id = s2.id
and left(s1.dl_dt, 10) = left(s2.dl_dt, 10)
and s2.ft_id = (select min(ft_id) from stkdat2 s3 where s3.id = s1.id and left(s3.dl_dt, 10) = left(s1.dl_dt, 10) and s3.ft_id > s1.ft_id)
where s1.id = '002400'
and left(s1.dl_dt, 10) = '2020-06-04'
order by dl_dt;

//find most deal stocks right now.
select s1.dl_dt, k.name, s1.b1_num, s1.s1_num, s1.b1_pri, s1.s1_pri, s1.cur_pri, (s1.cur_pri - s1.td_opn_pri) /s1.td_opn_pri pct_td_opn_pri, (s1.cur_pri - s1.yt_cls_pri) /s1.yt_cls_pri pct_yt_cls_pri
from stkdat2 s1
join (select id, max(ft_id) max_ft_id from stkdat2 group by id) t
  on s1.id = t.id
  and s1.ft_id = t.max_ft_id
join stk k
on k.id = s1.id
where left(s1.dl_dt, 10) = '2020-06-10'
  and s1_num > 10000
order by b1_num / s1_num desc,
 dl_dt desc;
//find dl_stk_mny 10 times bigger than yesterday stock.
  select k.name, k.id, s1.dl_mny_num, s2.dl_mny_num, s2.dl_dt from stkdat2 s1
  join stkdat2 s2
    on s1.id = s2.id
  and left(s2.dl_dt, 10) = left(s1.dl_dt + interval 1 day, 10)
  and right(left(s1.dl_dt, 16), 5) = '15:00'
  and right(left(s2.dl_dt, 16), 5) = '15:00'
  join stk k
    on s1.id = k.id
  where left(s2.dl_dt, 10) = '2020-06-10'
  and s2.dl_mny_num / s1.dl_mny_num > 10;
  
  
  // create data for simulation.
  insert into stkdat2_sim select * from stkdat2 where dl_dt like '% 09:30%';
  
  //back transaction tables
  
  delete from arc_cashacnt;
  delete from arc_tradedtl;
  delete from arc_tradehdr;
  delete from arc_sellbuyrecord;
  
  
  delete from cashacnt;
  delete from tradedtl;
  delete from tradehdr;
  delete from sellbuyrecord;
  
  insert into arc_cashacnt select * from cashacnt;
  insert into arc_tradedtl select * from tradedtl;
  insert into arc_tradehdr select * from tradehdr;
  insert into arc_sellbuyrecord select * from sellbuyrecord;
  
  
  insert into cashacnt select * from arc_cashacnt where acntId in ('GF600697','GF002236');
  insert into tradedtl select * from arc_tradedtl where acntId in ('GF600697','GF002236');
  insert into tradehdr select * from arc_tradehdr where acntId in ('GF600697','GF002236');
  insert into sellbuyrecord select * from arc_sellbuyrecord where stkId in ('600697','002236');
  
  
  //convert sim data into real data and remove today transaction data.
  update cashacnt set acntId = concat('GF' , right(acntid, 6));
  update tradehdr set acntId = concat('GF' , right(acntid, 6));
  update tradedtl set acntId = concat('GF' , right(acntid, 6));
  update sellbuyrecord set crt_by = 'REAL';
  
  update usrstk set suggested_by = 'SYSTEM_GRANTED_TRADER';
  
  //manual create buy records:
  insert into cashacnt values ('GF000408',50000,0,0,0,3000,0.8, '2020-08-10 09:30:00');
  insert into tradehdr values ('GF000408','000408',300*8.657,300,8.657,300*8.657,0.0014,0,'2020-08-10 09:30:00');
  insert into tradedtl values ('GF000408','000408',0,8.657,300,'2020-08-10 09:30:00',1,null,'AvgPriceBrkBuyPointSelector','past 3 days lower than 5 days avipri, now bigger than 5 days avgpri, buy!','QtyTradeStrategy');
  insert into sellbuyrecord values ('000408',8.657,300,1, '2020-08-10 09:30:00','REAL');

