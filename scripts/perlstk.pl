#perl script.pl [-c -f] -d 获取默认＠defaultstock列表中股票信息，参数-c 清屏，参数-f 输出加倍完全的信息，参考图例
#perl script.pl -m [sh sz zx cy] 四个参数分别代表获取上海，深圳，中小，创业板股票的信息
#perl script.pl sh600001 sz000858 获取参数供给的股票信息
#use strict;
#use warnings;
use Carp;
use LWP::UserAgent;
use Getopt::Std;

use constant MAXNUMBER => 500;
use constant LINE => 30;
#use constant PROXY => "http://10.40.14.56:80";
$| = 1; 

my %opts;
getopts("cdfm:", %opts);

my %market = (
	sh => sub {map {"sh$_"} ("600001" .. "602100")},
	sz => sub {map {"sz$_"} ("000001" .. "001999")},
	zx => sub {map {"sz$_"} ("002001" .. "002999")},
	cy => sub {map {"sz$_"} ("300001" .. "300400")},
);

my @defaultstock = qw(sz000858);
#my @defaultstock = qw(sh601818 sz300229 sz002649 sz002368 sh600667 sz000858);

if($opts{c}){
	system "cls";
}

my @stock = @defaultstock;
if($opts{d}){
    print "d is provided.\n";
	@stock = @defaultstock;
}
elsif($opts{m} && exists $market{lc $opts{m}}){
    print "m is provided.\n";
	@stock = $market{lc $opts{m}}->();
}
else{
    print "nothing is provided.\n";
	@stock =  grep {/sd{6}/} map {lc} @ARGV;
}
@stock = @defaultstock;

print "\@stock is @stock\n";
#my $func = $opts{f} ? &DrawStock : &DrawMarket;
#$func = &DrawStock;
while(1)
{
    Stocks($func,@stock) if @stock;
    sleep(60*1);
}

sub Stocks{
    #my $drawfunc = &DrawStock;
	
	my @stocklist = @_;
	return unless @stocklist;
	my $i = int(@stocklist);
	for(0 .. $i){
		my $strs = GetStockValue( @stocklist[$_]);
		for(split /;/,$strs){
			my ($code,$value) = /hq_str_(.{2}\d{6})="([^"]*)"/;
			if($value){
				DrawStock($code,$value);
			}
		}
	}
}

sub Stocks2{
    print "in Stocks...";
	my $drawfunc = shift;
	
	my @stocklist = grep {/sd{6}/i} map {lc} @_;
    print "\@stocklist size @stocklist.\n";
	return unless @stocklist;
    print "how about here?";	
	my $i = int(@stocklist/MAXNUMBER);
	for(0 .. $i){
		my $strs = GetStockValue($_ < $i ?
			@stocklist[$_*MAXNUMBER .. $_*MAXNUMBER+MAXNUMBER-1] :
			@stocklist[$_*MAXNUMBER .. $#stocklist]);
		for(split /;/,$strs){
			my ($code,$value) = /hq_str_(sd{6})="([^"]*)"/;
			if($value){
				$drawfunc->($code,$value);
			}
		}
	}
}

sub GetStockValue{
	croak "Length > MAXNUMBER" if @_>MAXNUMBER;
	
	my $ua = LWP::UserAgent->new();
	#~ $ua->proxy("http", PROXY);
	
	my $res = $ua->get("http://hq.sinajs.cn/list=".join(",",@_));
	if($res->is_success){
		return $res->content;
	}
}

sub DrawMarket{
    print "Draw market...";
	my ($stockcode,$value) = @_;
    print "value is [$stockcode, $value]\n";
	my @list = split /,/, $value;
	
	$^ = "MARKET_TOP";
	$~ = "MARKET";
	$= = LINE+3;
	write;
	
	format MARKET_TOP = 

code     name          current (   +/-       %)    open   close          low(ch)        high(ch)  S(W)    $(W) [             buy <=>   sell          ]
======================================================================================================================================================
.

	format MARKET = 
@<<<<<<< @<<<<<<<<<<<< @###.## (@##.## @##.##%) @###.## @###.## @###.## (@##.##) @###.## (@#.##) @#### @###### [@########@###.## <=>@###.##@######## ]
$stockcode,$list[0],$list[3],$list[3]-$list[1],$list[1]>0?($list[3]-$list[1])*100/$list[1]:0,$list[1],$list[2],$list[5],$list[5]-$list[1],$list[4],$list[4]-$list[1],$list[8]/10000,$list[9]/10000,$list[10],$list[11],$list[21],$list[20],
.
}

sub DrawStock{
	my ($stockcode,$value) = @_;
	my @list = split /,/, $value;
	
	$~ = "STOCK";
	write;

	format STOCK = 
=====================================================================
@<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<@>>>>>>>>>>>>>>>>>>
"$stockcode: $list[0]","$list[30] $list[31]",
=====================================================================
current:@###.## (@##.##@##.##%) |@########@###.## <=>@###.##@########
$list[3],$list[3]-$list[1],$list[1]>0?($list[3]-$list[1])*100/$list[1]:0,$list[10],$list[11],$list[21],$list[20],
close:  @###.##                 |@########@###.## <=>@###.##@########
$list[2],$list[12],$list[13],$list[23],$list[22],
open:   @###.##                 |@########@###.## <=>@###.##@########
$list[1],$list[14],$list[15],$list[25],$list[24],
low:    @###.## (@##.##)        |@########@###.## <=>@###.##@########
$list[5],$list[5]-$list[1],$list[16],$list[17],$list[27],$list[26],
high:   @###.## (@##.##)        |@########@###.## <=>@###.##@########
$list[4],$list[4]-$list[1],$list[18],$list[19],$list[29],$list[28],
S(W):    @#####                 |------------------------------------
$list[8]/10000,
$(W):    @#####                 |@########                  @########
$list[9]/10000,$list[10]+$list[12]+$list[14]+$list[16]+$list[18],$list[20]+$list[22]+$list[24]+$list[26]+$list[28],
.
}
