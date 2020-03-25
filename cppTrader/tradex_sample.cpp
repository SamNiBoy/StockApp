#include "tradex_sample.hpp"

#include <memory>
#include <thread>
#include <chrono>
#include <string.h>

#include "nanolog.hpp"

#ifdef _WIN32

#else
#endif // 

namespace com
{
namespace tradex
{
namespace sample
{
	std::string get_time_stamp() {
		return "";
	}
////////////////////////////////////////////////////////////////////////////////
TradeXCallback::TradeXCallback(TradeXSample *main) : main(main)
{
}
TradeXCallback::~TradeXCallback()
{
}

void TradeXCallback::OnLogin(const TRXLoginResponse *loginResponse) {
	main->set_login(loginResponse->is_success);
	if (loginResponse->is_success) {
		std::cout << "Login:" << loginResponse->session_id << ";" << loginResponse->is_success << std::endl;
		if (!loginResponse->session_id) {
			std::cout << "";
		}
	}
	else {
		std::cout << "Login:" << loginResponse->session_id << ";" << loginResponse->is_success << ";" << loginResponse->error_message << std::endl;
	}
}
/// 注销响应回调
void TradeXCallback::OnLogout(bool isLogoutSuccess, const char *error_message) {
	std::cout << "Logout:" << isLogoutSuccess << ";" << error_message << std::endl;
}
/// 连接建立成功
/// <param name="severType">后端服务器类型</param>
/// <param name="url">网络连接地址</param>
void TradeXCallback::OnConnected(TRXServerType serverType, const char *url) {
	std::cout << "Connected:" << serverType << ":" << url<<std::endl;
}
/// 连接断开通知
/// <param name="severType">断开连接的服务器类型</param>
/// <param name="url">断开连接的地址</param>
/// <param name="errorMessage">断开原因</param>
void TradeXCallback::OnDisconnected(TRXServerType serverType, const char *url, const char *error_message) {
	std::cout << "DisConnected:" << serverType << ":" << url<< std::endl;
}
/// 委托回报响应
/// <param name="orderReport">返回的委托信息</param>
void TradeXCallback::OnOrderEvent(const TRXOrderReport *orderReport) {
	main->update_order_id(orderReport->client_order_id, orderReport->order_id);
	if (orderReport->client_basket_id != 0) {
		main->update_basket_id(orderReport->client_basket_id, orderReport->basket_id);
		std::cout << "link client_basket_id[" << orderReport->client_basket_id << "] to basket_id[" << orderReport->basket_id << "]." << std::endl;
	}

	//std::cout << "OrderEvent,client_order_id[" << orderReport->client_order_id << "],order_id[" << orderReport->order_id << "],status[" << orderReport->order_status << "]" << std::endl;
	if (orderReport->order_status == TRXOrderStatus::Rejected) {
		std::cout << "Reject:" << orderReport->error_message << std::endl;
        main->setTranStatus(orderReport->order_status, orderReport->error_message);
	}
}
/// 返回的成交回报信息
/// <param name="tradeReport"></param>
void TradeXCallback::OnTradeEvent(const TRXTradeReport *tradeReport) {
	std::cout << "TradeEvent,client_order_id[" << tradeReport->client_order_id << "],order_id[" << tradeReport->order_id << std::endl;
    std::cout << "Trade:" << tradeReport->trade_unit << ":" << tradeReport->order_id << ":" << tradeReport->trade_quantity << std::endl;
    std::string key = std::to_string(tradeReport->client_order_id) + "_trade_report";
    std::string value = "order:" + std::to_string(tradeReport->order_id) + "|symbol:" + tradeReport->symbol + "|price:" + std::to_string(tradeReport->trade_price) +
                        "|quantity:" + std::to_string(tradeReport->trade_quantity) + "|trade_amount:" + std::to_string(tradeReport->trade_amount);
    main->setTranStatus(0, value);
    /*auto b = main->statusMapForQuery.find(key);
    if (b != main->statusMapForQuery.end()) {
        std::cout<<"Updating queryTrade key:"<<key<<std::endl;
        std::cout<<"Updating queryTrade value:"<<value<<std::endl;
        main->statusMapForQuery[key] = value;
    }
    else {
        std::cout<<"OnTradeEvent: Before Inserting queryTrade key:"<<key<<", value:"<<value<<std::endl;
        std::pair<std::string, std::string> pr = std::make_pair(key, value);
        std::cout<<"middle of insert..."<<std::endl;
        main->statusMapForQuery.insert(pr);
        std::cout<<"OnTradeEvent: After Inserting queryTrade key:"<<key<<", value:"<<value<<std::endl;
    }*/
}
/// 交易单元连接状态通知
/// <param name="notice"></param>
void TradeXCallback::OnTradeUnitStatus(const TRXTradeUnitConnStatusNotice *notice) {}
/// 撤单被拒绝回报
/// <param name="cancelReject"></param>
void TradeXCallback::OnCancelReject(const TRXOrderCancelReject *cancelReject) {
	std::cout << "CancelRejected:" << cancelReject->order_id << ";" << cancelReject->error_message << std::endl;
}
/// 持仓查询返回结果
/// <param name="positionsList"></param>
void TradeXCallback::OnQueryPosition(const TRXPosition *position, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success) {
	if (position) {
		std::cout << "Position:" << position->symbol << ":" << position->latest_qty << ";is_last:"<< is_last << std::endl;
	}
	else {
		std::cout << "Position:" << is_last << ":" << error_message << std::endl;
	}
}
/// 标准券持仓查询返回结果
/// <param name="positionsList"></param>
void TradeXCallback::OnQueryStandardCouponPosition(const TRXPosition *position, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success) {}
/// 账户资金查询返回结果
/// <param name="balanceList"></param>
void TradeXCallback::OnQueryBalance(const TRXBalance *balance, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success) {
	if (balance) {
		std::cout << "Balance:" << balance->trade_unit << ":" << balance->available_balance << ";is_last=" << is_last << std::endl;
	}
	else {
		std::cout << "Balance:" << is_last << ":" << error_message << std::endl;
	}
}
/// 委托查询返回结果
/// <param name="orderReportList"></param>
void TradeXCallback::OnQueryOrder(const TRXOrderReport *order, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success) {
	if (order) {
		std::cout << "Order:" << order->trade_unit << ":" << order->order_id<<":"<<order->order_status << std::endl;
	}
	else {
		std::cout << "Order:" << is_last << ":" << error_message << std::endl;
	}
}
/// 成交回报查询返回结果
/// <param name="tradeReportList"></param>
/*
struct TRXTradeReport
{
    client_id_t client_id;        // 登录用户名
    trade_unit_t trade_unit;      // 交易单元
    symbol_t symbol;              // 交易标的代码，需填入交易所认可的交易标的代码
    order_id_t client_order_id;   // 调用方填写的委托ID
    order_id_t order_id;          // 委托的订单ID，该ID由系统返回
    basket_id_t basket_id;        // 篮子委托ID
    basket_id_t client_basket_id; // 用户定义的篮子委托ID
    trade_id_t trade_id;          // 成交编号
    TRXMarket market;             // 此订单的交易市场
    TRXSide side;                 // 买卖方向，及两融操作：担保品买入、担保品卖出、融资买入、融券卖出、买券还券、卖券还款
    TRXOpenClose open_close;      // 开平标志
    price_t trade_price;          // 本次成交的成交价
    quantity_t trade_quantity;    // 本次成交的成交数量
    timestamp_t trade_time;       // 本次成交的成交时间
    money_t trade_amount;         // 本次成交的成交金额
    price_t avg_price;            // 平均成交价格
    quantity_t cum_qty;           // 累计成交量
    quantity_t leaves_qty;        // 剩余数量
    money_t total_trading_amount; // 累计成交额
};*/
void TradeXCallback::OnQueryTrade(const TRXTradeReport *tradeReport, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success) {
	if (tradeReport) {
		std::cout << "Trade:" << tradeReport->trade_unit << ":" << tradeReport->order_id << ":" << tradeReport->trade_quantity << std::endl;
        std::string key = std::to_string(tradeReport->client_order_id) + "_trade_report";
        std::string value = "order:" + std::to_string(tradeReport->order_id) + "|symbol:" + tradeReport->symbol + "|price:" + std::to_string(tradeReport->trade_price) +
                            "|quantity:" + std::to_string(tradeReport->trade_quantity) + "|trade_amount:" + std::to_string(tradeReport->trade_amount);
        
        main->setTranStatus(0, value);
	    /*auto b = main->statusMapForQuery.find(key);
	    if (b != main->statusMapForQuery.end()) {
            std::cout<<"Updating queryTrade key:"<<key<<std::endl;
            std::cout<<"Updating queryTrade value:"<<value<<std::endl;
	        main->statusMapForQuery[key] = value;
	    }
	    else {
            std::cout<<"OnQuery Trade Inserting queryTrade key:"<<key<<std::endl;
            std::cout<<"OnQuery Trade Inserting queryTrade value:"<<value<<std::endl;
	        main->statusMapForQuery.insert(std::make_pair(key, value));
            std::cout<<"After Inserting queryTrade value:"<<value<<std::endl;
	    }*/
	    /*order_id_t find_order_id(order_id_t client_order_id)
	    {
	        auto b = clOrdId2OrderIdMap.find(client_order_id);
	        if (b != clOrdId2OrderIdMap.end())
	        {
	            return b->second;
	        }

	        return 0;
	    }*/
	}
	else {
		/*std::cout << "Trade query Trader error:" << is_last << ":" << error_message << ", request_id:"<<request_id<<std::endl;
		std::string key_request_id = std::to_string(request_id) + "_request_id";
        std::string ermsg = error_message;
		std::string key_value = "error:" + ermsg;
	    auto b = main->statusMapForQuery.find(key_request_id);
	    if (b != main->statusMapForQuery.end()) {
	        std::cout<<"Updating queryTrade request_id key:"<<key_request_id<<std::endl;
	        std::cout<<"Updating queryTrade key_value:"<<key_value<<std::endl;
	        main->statusMapForQuery[key_request_id] = key_value;
	    }
	    else {
	        std::cout<<"Inserting queryTrade key_request_id:"<<key_request_id<<std::endl;
	        std::cout<<"Inserting queryTrade key_value:"<<key_value<<std::endl;
	        main->statusMapForQuery.insert(std::make_pair(key_request_id, key_value));
	    }*/
	}
}
/// 可融券查询返回结果
/// <param name="borrowingSecurityList"></param>
void TradeXCallback::OnQueryBorrowingSecurity(const TRXBorrowingSecurity *borrowingSecurity, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success) {
	if (borrowingSecurity) {
		std::cout << "BorrowingSecurity:" << borrowingSecurity->trade_unit << ":" << borrowingSecurity->symbol << ":" << borrowingSecurity->available_to_borrow_qty << std::endl;
	}
	else {
		std::cout << "BorrowingSecurity:" << is_last << ":" << error_message << std::endl;
	}
}
/// 融券负债查询返回结果
/// <param name="securityLiabilityList"></param>
void TradeXCallback::OnQuerySecurityLiability(const TRXSecurityLiability *securityLiability, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success) {
	if (securityLiability) {
		std::cout << "SecurityLiability:" << securityLiability->trade_unit << ":" << securityLiability->symbol << ":" << securityLiability->total_debit << std::endl;
	}
	else {
		std::cout << "SecurityLiability:" << is_last << ":" << error_message << std::endl;
	}
}
/// 融资负债查询返回结果
/// <param name="financingLiabilityList"></param>
void TradeXCallback::OnQueryFinancingLiability(const TRXFinancingLiability *financingLiability, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success) {
	if (financingLiability) {
		std::cout << "FinancingLiability:" << financingLiability->trade_unit << ":" << financingLiability->symbol << ":" << financingLiability->total_debit << std::endl;
	}
	else {
		std::cout << "FinancingLiability:" << is_last << ":" << error_message << std::endl;
	}
}
/// 信用资产查询返回结果
/// <param name="creditAssetList"></param>
void TradeXCallback::OnQueryCreditAsset(const TRXCreditAsset *creditAsset, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success) {
	if (creditAsset) {
		std::cout << "CreditAsset:" << creditAsset->trade_unit << ":" << creditAsset->market_value << ":" << creditAsset->total_debit << std::endl;
	}
	else {
		std::cout << "CreditAsset:" << is_last << ":" << error_message << std::endl;
	}
}

////////////////////////////////////////////////////////////////////////////////
TradeXSample::TradeXSample() : api(nullptr), callback(this), normal_trade_unit(0), credit_trade_unit(0), is_login(false)
{
}
TradeXSample::~TradeXSample()
{
}

TradeXApi* TradeXSample::getAPI()
{
    if (api)
    {
        return api;
    }
    else
    {
        return NULL;
    }
}

void TradeXSample::initialize(const std::string &log_path)
{
    this->log_path = log_path;

    api = TradeXApi::CreateTraderApi(log_path.c_str(),TRXLogLevel::TRX_DEBUG);
    api->RegisterSpi(static_cast<TradeXSpi*>(&callback));
}

void TradeXSample::close()
{
    if (api)
    {
        api->Release();
		delete api;
		api = nullptr;
    }
}

void TradeXSample::login(const TRXLoginRequest *login)
{
    if (!api)
    {
        return;
    }

    int rtn = api->Login(login);
    if (rtn)
    {
        std::cout << "Login error:" << rtn << std::endl;
    }
    else
    {
        std::cout << "Login sent." << std::endl;
    }
}
void TradeXSample::logout()
{
    if (!api)
    {
        return;
    }

    int rtn = api->Logout();
    if (rtn)
    {
        std::cout << "Logout error:" << rtn << std::endl;
    }
    else
    {
        std::cout << "Logout sent." << std::endl;
    }
}

void TradeXSample::ETFCreation() {
	uint64_t client_order_id = timestamp_now();
	update_order_id(client_order_id, 0);

	TRXSingleOrder order;
	memset(&order, 0, sizeof(order));

	order.trade_unit = normal_trade_unit;
	order.client_order_id = client_order_id;
	strcpy(order.symbol, "510051");
	order.market = TRXMarket::SH_A;
	order.price = 1;
	order.quantity = 900000;
	order.side = TRXSide::Creation;

	int rtn = api->PlaceOrder(&order);
	if (rtn) {
		std::cout << rtn << std::endl;
		return;
	}
}

void TradeXSample::ETFRemption() {
	uint64_t client_order_id = timestamp_now();
	update_order_id(client_order_id, 0);

	TRXSingleOrder order;
	memset(&order, 0, sizeof(order));

	order.trade_unit = normal_trade_unit;
	order.client_order_id = client_order_id;
	strcpy(order.symbol, "510051");
	order.market = TRXMarket::SH_A;
	order.price = 1;
	order.quantity = 900000;
	order.side = TRXSide::Redemption;

	int rtn = api->PlaceOrder(&order);
	if (rtn) {
		std::cout << rtn << std::endl;
		return;
	}
}

void TradeXSample::SHOrder() {
	uint64_t client_order_id = timestamp_now();
	update_order_id(client_order_id, 0);

	TRXSingleOrder order;
	memset(&order, 0, sizeof(order));

	order.trade_unit = normal_trade_unit;
	order.client_order_id = client_order_id;
	strcpy(order.symbol, "510300");
	order.market = TRXMarket::SH_A;
	order.price = 3.877;
	order.quantity = 1000;
	order.side = TRXSide::Buy;
	order.price_type = TRXPriceType::LIMIT;

	int rtn = api->PlaceOrder(&order);
	if (rtn) {
		std::cout << rtn << std::endl;
		return;
	}

	std::this_thread::sleep_for(std::chrono::seconds(5));

	order_id_t order_id = find_order_id(client_order_id);
	if (order_id > 0) {
		TRXOrderCancelRequest cancel;
		cancel.order_id = order_id;
		cancel.trade_unit = normal_trade_unit;

		api->CancelOrder(&cancel);
	}
}

void TradeXSample::SZOrder() {
	uint64_t client_order_id = timestamp_now();
	update_order_id(client_order_id, 0);

	TRXSingleOrder order;
	memset(&order, 0, sizeof(order));

	order.trade_unit = normal_trade_unit;
	order.client_order_id = client_order_id;
	strcpy(order.symbol, "159919");
	order.market = TRXMarket::SZ_A;
	order.price = 4.146;
	order.quantity = 1000;
	order.side = TRXSide::Buy;
	order.price_type = TRXPriceType::LIMIT;

	int rtn = api->PlaceOrder(&order);
	if (rtn) {
		std::cout << rtn << std::endl;
		return;
	}

	std::this_thread::sleep_for(std::chrono::seconds(5));

	order_id_t order_id = find_order_id(client_order_id);
	if (order_id > 0) {
		TRXOrderCancelRequest cancel;
		cancel.order_id = order_id;
		cancel.trade_unit = normal_trade_unit;

		api->CancelOrder(&cancel);
	}
}
void TradeXSample::HKOrder() {
	uint64_t client_order_id = timestamp_now();
	update_order_id(client_order_id, 0);

	TRXSingleOrder order;
	memset(&order, 0, sizeof(order));

	order.trade_unit = normal_trade_unit;
	order.client_order_id = client_order_id;
	strcpy(order.symbol, "00002");
	order.market = TRXMarket::SH_HK;
	order.price = 3;
	order.quantity = 1000;
	order.side = TRXSide::Buy;
	order.price_type = TRXPriceType::LIMIT;
	order.hk_stock_lot_type = TRXHKOrderLotType::BoardLot;

	int rtn = api->PlaceOrder(&order);
	if (rtn) {
		std::cout << rtn << std::endl;
		return;
	}

	std::this_thread::sleep_for(std::chrono::seconds(5));

	order_id_t order_id = find_order_id(client_order_id);
	if (order_id > 0) {
		TRXOrderCancelRequest cancel;
		cancel.order_id = order_id;
		cancel.trade_unit = normal_trade_unit;

		api->CancelOrder(&cancel);
	}
}
void TradeXSample::CreditOrder() {
	uint64_t client_order_id = timestamp_now();
	update_order_id(client_order_id, 0);

	TRXSingleOrder order;
	memset(&order, 0, sizeof(order));

	order.trade_unit = normal_trade_unit;
	order.client_order_id = client_order_id;
	strcpy(order.symbol, "000001");
	order.market = TRXMarket::SZ_A;
	order.price = 16;
	order.quantity = 1000;
	order.side = TRXSide::Buy;
	order.price_type = TRXPriceType::LIMIT;
	order.credit_position_type = TRXCreditPositionType::NormalPosition;

	int rtn = api->PlaceOrder(&order);
	if (rtn) {
		std::cout << rtn << std::endl;
		return;
	}

	std::this_thread::sleep_for(std::chrono::seconds(5));

	order_id_t order_id = find_order_id(client_order_id);
	if (order_id > 0) {
		TRXOrderCancelRequest cancel;
		cancel.order_id = order_id;
		cancel.trade_unit = normal_trade_unit;

		api->CancelOrder(&cancel);
	}
}
void TradeXSample::AlgoOrder() {
	uint64_t client_order_id = timestamp_now();
	update_order_id(client_order_id, 0);

	TRXSingleOrder order;
	memset(&order, 0, sizeof(order));

	order.trade_unit = normal_trade_unit;
	order.client_order_id = client_order_id;
	strcpy(order.symbol, "600031");
	order.market = TRXMarket::SH_A;
	order.price = 16.82;
	order.quantity = 10000;
	order.side = TRXSide::Buy;
	order.price_type = TRXPriceType::LIMIT;

	TRXAlgoParameters algoParameters;
	memset(&algoParameters, 0, sizeof(algoParameters));
	strcpy(algoParameters.algo_name, "TWAP");

	algoParameters.start_time = 20200114095700; //YYYYMMDDHHMMSS
	algoParameters.end_time = 20200114100300; //YYYYMMDDHHMMSS
	algoParameters.participation_rate = 30;
	//algoParameters.cancel_rate = 10;
	algoParameters.style = 3;
	algoParameters.am_auction_flag = true;
	/*
	algoParameters.close_auction_flag = true;
	algoParameters.customized_minute = 1;
	algoParameters.customized_pct_of_px = 3;
	algoParameters.sell_on_down_limit = false;
	algoParameters.sell_on_up_limit = false;
	algoParameters.buy_on_up_limit = false;
	algoParameters.buy_on_down_limit = false;
	algoParameters.condition_control = false;
	algoParameters.count_eligible_volume_limit_price = false;
	algoParameters.trading_condition = 1;
	strcpy(algoParameters.reference_index, "600031SH");
	algoParameters.auto_pause_resume = false;
	algoParameters.eligible_to_trading = false;
	algoParameters.is_absolute_px = false;
	strcpy(algoParameters.px_type, "P");
	algoParameters.relative_price_limit_offset = 0.01;
	*/
	order.algo_parameters = &algoParameters;
	int rtn = api->PlaceOrder(&order);
	if (rtn) {
		std::cout << rtn << std::endl;
		return;
	}

	std::this_thread::sleep_for(std::chrono::seconds(5));

	order_id_t order_id = find_order_id(client_order_id);
	if (order_id > 0) {
		TRXOrderCancelRequest cancel;
		cancel.order_id = order_id;
		cancel.trade_unit = normal_trade_unit;

		api->CancelOrder(&cancel);
	}
}
void TradeXSample::BasketOrders() {
	basket_id_t client_basket_id = next_basket_id();
	update_basket_id(client_basket_id, 0);

	TRXBasketOrder basket;
	memset(&basket, 0, sizeof(basket));

	basket.trade_unit = this->normal_trade_unit;
	basket.client_basket_id = client_basket_id;

	order_id_t client_order_id_1 = timestamp_now();
	update_order_id(client_order_id_1, 0);
	{
		TRXBasketLeg leg1;
		memset(&leg1, 0, sizeof(leg1));

		leg1.client_order_id = client_order_id_1;
		strcpy(leg1.symbol, "600000");
		leg1.market = TRXMarket::SH_A;
		leg1.price = 12;
		leg1.quantity = 1000;
		leg1.side = TRXSide::Buy;
		leg1.price_type = TRXPriceType::LIMIT;

		basket.legs[0] = leg1;
	}

	order_id_t client_order_id_2 = timestamp_now();
	if (client_order_id_2 == client_order_id_1) {
		client_order_id_2 = client_order_id_2 + 1;
	}

	update_order_id(client_order_id_2, 0);
	{
		TRXBasketLeg leg2;
		memset(&leg2, 0, sizeof(leg2));

		leg2.client_order_id = client_order_id_2;
		strcpy(leg2.symbol, "159919");
		leg2.market = TRXMarket::SZ_A;
		leg2.price = 4.5;
		leg2.quantity = 1000;
		leg2.side = TRXSide::Buy;
		leg2.price_type = TRXPriceType::LIMIT;

		basket.legs[1] = leg2;
	}

	basket.leg_count = 2;

	int rtn = api->PlaceBasketOrder(&basket);
	if (rtn !=0) {
		std::cout << rtn << std::endl;
		return;
	}

	std::this_thread::sleep_for(std::chrono::seconds(5));

	order_id_t order_id_2 = find_order_id(client_order_id_2);
	if (order_id_2 > 0) {
		TRXOrderCancelRequest cancel;
		cancel.order_id = order_id_2;
		cancel.trade_unit = normal_trade_unit;

		rtn = api->CancelOrder(&cancel);
		if (!rtn) {
			std::cout << rtn << std::endl;
		}
	}

	std::this_thread::sleep_for(std::chrono::seconds(5));

	basket_id_t basket_id = find_basket_id(client_basket_id);
	if(basket_id>0)
	{
		TRXBasketCancelRequest cancelBasket;
		cancelBasket.trade_unit = normal_trade_unit;
		cancelBasket.basket_id = basket_id;

		rtn = api->CancelBasketOrder(&cancelBasket);
		if (!rtn) {
			std::cout << rtn << std::endl;
		}
	}
}
void TradeXSample::AlgoBasketOrders() {}

void TradeXSample::QueryTradeUnit() {
	TRXTradeUnitStatusQueryRequest request;
	memset(&request, 0, sizeof(request));

	request.trade_unit = normal_trade_unit;
	request.request_id = timestamp_now();

	int rtn = api->QueryTradeUnitStatus(&request);
	if (!rtn) {
		std::cout << rtn << std::endl;
	}
}

void TradeXSample::QueryCash() {
	TRXBalanceQueryRequest request;
	memset(&request, 0, sizeof(request));

	request.trade_unit = normal_trade_unit;
	request.request_id = timestamp_now();

	int rtn = api->QueryBalance(&request);
	if (!rtn) {
		std::cout << rtn << std::endl;
	}
}
void TradeXSample::QueryPosition() {
	TRXPositionQueryRequest request;
	memset(&request, 0, sizeof(request));

	request.trade_unit = normal_trade_unit;
	request.request_id = timestamp_now();

	int rtn = api->QueryPosition(&request);
	if (!rtn) {
		std::cout << rtn << std::endl;
	}
}
void TradeXSample::QueryCreditAsset() {
	TRXCreditAssetQueryRequest request;
	memset(&request, 0, sizeof(request));

	request.trade_unit = credit_trade_unit;
	request.request_id = timestamp_now();

	int rtn = api->QueryCreditAsset(&request);
	if (!rtn) {
		std::cout << rtn << std::endl;
	}
}
void TradeXSample::QueryOrders() {
	TRXOrderQueryRequest request;
	memset(&request, 0, sizeof(request));

	request.trade_unit = normal_trade_unit;
	request.request_id = timestamp_now();

	int rtn = api->QueryOrder(&request);
	if (!rtn) {
		std::cout << rtn << std::endl;
	}
}

std::string TradeXSample::getTranStatus()
{
    /*std::string key = std::to_string(client_order_id) + "_trade_report";
    std::cout <<"get "<<key<<" start from get_TradeResult."<< std::endl;
    while(true)
    {
        auto b = statusMapForQuery.find(key);
        if (b != statusMapForQuery.end())
        {
            statusMapForQuery.erase(key);
            return b->second;
        }
        else
        {
            if (request_id > 0)
            {
                std::string key_request_id = std::to_string(request_id) + "_request_id";
                //errored, return error message.
                b = statusMapForQuery.find(key_request_id);
                if (b != statusMapForQuery.end())
                {
                    statusMapForQuery.erase(key_request_id);
                    return b->second;
                }
            }
            std::cout <<"get "<<key<<" not ready, wait for 1 second."<< std::endl;
            std::this_thread::sleep_for(std::chrono::seconds(1));
        }
    }*/
    std::string final_msg = "";
    while(true)
    {
        if(tran_status_code == -1)
        {
            std::cout <<"trans not ready, wait for 1 second."<< std::endl;
            std::this_thread::sleep_for(std::chrono::seconds(1));
            continue;
        }
        if (tran_status_code)
        {
            final_msg = "error#" + std::to_string(tran_status_code) + "#" + tran_status_message;
        }
        else
        {
            final_msg = "success#" + tran_status_message;
        }
        break;
    }
    return final_msg;
}
void TradeXSample::QueryTrades() {
	TRXTradeQueryRequest request;
	memset(&request, 0, sizeof(request));

	request.trade_unit = normal_trade_unit;
	request.request_id = timestamp_now();
	request.client_order_id = 497975296;

	int rtn = api->QueryTrade(&request);
	if (!rtn) {
		std::cout << rtn << std::endl;
	}
}
void TradeXSample::QuerySecurityLiability() {
	TRXSecurityLiabilityQueryRequest request;
	memset(&request, 0, sizeof(request));

	request.trade_unit = credit_trade_unit;
	request.request_id = timestamp_now();

	int rtn = api->QuerySecurityLiability(&request);
	if (!rtn) {
		std::cout << rtn << std::endl;
	}
}
void TradeXSample::QueryFinancingLiability() {
	TRXFinancingLiabilityQueryRequest request;
	memset(&request, 0, sizeof(request));

	request.trade_unit = credit_trade_unit;
	request.request_id = timestamp_now();

	int rtn = api->QueryFinancingLiability(&request);
	if (!rtn) {
		std::cout << rtn << std::endl;
	}
}
void TradeXSample::QueryBorrowingSecurity() {
	TRXBorrowingSecurityQueryRequest request;
	memset(&request, 0, sizeof(request));

	request.trade_unit = credit_trade_unit;
	request.request_id = timestamp_now();

	int rtn = api->QueryBorrowingSecurity(&request);
	if (!rtn) {
		std::cout << rtn << std::endl;
	}
}

} // namespace sample

} // namespace tradex
} // namespace com
