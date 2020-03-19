
//////Start script for backup trade records.
 declare 
day_to_purge int := 6;
begin
insert into arc_TradeHdr 
select * 
from TradeHdr 
where acntId in (select acntid from CashAcnt where dft_acnt_flg = 1);
delete from TradeHdr where  acntId in (select acntid from CashAcnt where dft_acnt_flg = 1);
commit;

insert into arc_Tradedtl
select * 
from Tradedtl
where acntId in (select acntid from CashAcnt where dft_acnt_flg = 1);
delete from Tradedtl where acntId in (select acntid from CashAcnt where dft_acnt_flg = 1);
commit;

insert into arc_sellbuyrecord
select * 
from sellbuyrecord
where 'Acnt' || stkid in (select acntid from CashAcnt where dft_acnt_flg = 1);
delete from sellbuyrecord where 'Acnt' || stkid in (select acntid from CashAcnt where dft_acnt_flg = 1);
commit;

insert into arc_CashAcnt select * from CashAcnt where dft_acnt_flg =1;
delete from CashAcnt where dft_acnt_flg = 1;
commit;
exception when others then 
dbms_output.put_line('arc trade records insert exception');
rollback;
end;
//////End script for backup trade records.

//////Start create stkdlyinfo
declare 
day_to_purge int := 6;
begin
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
end;
////End create stkdlyinfo.

/////Start backup stkdat2 data.
declare 
day_to_purge int := 6;
begin
delete from stkdat2 s1 where exists (select 'x' from stkdat2 s2 where s2.id = s1.id and s2.dl_dt = s1.dl_dt and s2.ft_id < s1.ft_id);
commit;
insert into arc_stkdat2 
select * 
from stkdat2 
where dl_dt < sysdate - day_to_purge
and not exists (
select 'x' 
from arc_stkdat2 s2 
where s2.ft_id = stkdat2.ft_id);
delete from stkdat2 where dl_dt < sysdate - day_to_purge;
commit;
exception when others then 
dbms_output.put_line('arc_stkdat2 insert exception');
rollback;
end;

////End backup stkdat data.
