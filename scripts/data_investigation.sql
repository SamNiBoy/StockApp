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