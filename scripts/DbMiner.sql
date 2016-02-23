/*统计上涨幅度超过0.09的股票次数和整幅分布*/
select avg(stddev1) xx, cnt1, max(times) times, count(distinct id) stkNum
from 
(
select distinct s1.id, s2.cnt1, round(s3.scp, 4) stddev1, s2.times from stkdlyinfo s1, (
select id, sum(case when ((td_cls_pri-yt_cls_pri)/yt_cls_pri > 0.09 and (td_cls_pri-yt_cls_pri)/yt_cls_pri <= 0.1) then 1 else 0 end) cnt1,
count(*) times from stkdlyinfo group by id ) s2,
(select id, stddev((cur_pri - yt_cls_pri) / yt_cls_pri) scp from stkdat2 group by id) s3
where s1.id = s2.id
  and s1.id = s3.id
  --and s3.scp > 0.01
) temp
group by cnt1
order by cnt1