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