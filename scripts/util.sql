# load csv into oracle db
create control file:
load data
infile 'd:\test.csv'
into table "test"
fields terminated by ','
(A,B)

sqlldr userid=user/password@database control=D:\MFC\stockapp\scripts\test.ctl log=D:\MFC\stockapp\scripts\test.log

# Get 5% increase stock.
select distinct mx.id, (mx.cur_pri - mn.cur_pri) / mn.cur_pri pct from stkdat mx, stkdat mn
where mx.id = mn.id
  and not exists (select 'x' from stkdat s1 where s1.id = mx.id and s1.ft_id > mx.ft_id and s1.cur_pri > 0)
  and not exists (select 'x' from stkdat s1 where s1.id = mn.id and s1.ft_id < mn.ft_id and s1.cur_pri > 0)
  and (mx.cur_pri - mn.cur_pri) / mn.cur_pri > 0.05
  and mn.cur_pri > 0
order by (mx.cur_pri - mn.cur_pri) / mn.cur_pri desc