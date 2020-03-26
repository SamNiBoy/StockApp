#include "tradex_messages.h"
#include "tradex_trader_api.h"
#include "tradex_types.h"

#include <iostream>
#include <unordered_map>
#include <chrono>
#include <string>
#include<random>
#include<memory.h>
#include <ctime>

using namespace com::tradex::api;

namespace com
{
namespace tradex
{
namespace sample
{
	

inline uint64_t timestamp_now()
{
    return std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::high_resolution_clock::now().time_since_epoch()).count();
}

inline uint32_t next_basket_id() {
	static std::default_random_engine e(1);
	static std::uniform_int_distribution<int> u(1, 10000000);
	return u(e);
}

class TradeXSample;
class TradeXCallback : public TradeXSpi
{
private:
    TradeXSample *main;

public:
    TradeXCallback(TradeXSample *main);
    ~TradeXCallback();

    /// 登录响应回调，调用方应在登录成功之后进行后续操作
	void OnLogin(const TRXLoginResponse *loginResponse) override;
    /// 注销响应回调
	void OnLogout(bool isLogoutSuccess, const char *error_message) override;
    /// 连接建立成功
    /// <param name="severType">后端服务器类型</param>
    /// <param name="url">网络连接地址</param>
	void OnConnected(TRXServerType severType, const char *url) override;
    /// 连接断开通知
    /// <param name="severType">断开连接的服务器类型</param>
    /// <param name="url">断开连接的地址</param>
    /// <param name="errorMessage">断开原因</param>
	void OnDisconnected(TRXServerType severType, const char *url, const char *error_message) override;
    /// 委托回报响应
    /// <param name="orderReport">返回的委托信息</param>
	void OnOrderEvent(const TRXOrderReport *orderReport) override;
    /// 返回的成交回报信息
    /// <param name="tradeReport"></param>
	void OnTradeEvent(const TRXTradeReport *tradeReport) override;
    /// 交易单元连接状态通知
    /// <param name="notice"></param>
	void OnTradeUnitStatus(const TRXTradeUnitConnStatusNotice *notice) override;
    /// 撤单被拒绝回报
    /// <param name="cancelReject"></param>
	void OnCancelReject(const TRXOrderCancelReject *cancelReject) override;
    /// 持仓查询返回结果
    /// <param name="positionsList"></param>
	void OnQueryPosition(const TRXPosition *position, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success = true) override;
    /// 标准券持仓查询返回结果
    /// <param name="positionsList"></param>
	void OnQueryStandardCouponPosition(const TRXPosition *position, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success = true) override;
    /// 账户资金查询返回结果
    /// <param name="balanceList"></param>
	void OnQueryBalance(const TRXBalance *balance, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success = true) override;
    /// 委托查询返回结果
    /// <param name="orderReportList"></param>
	void OnQueryOrder(const TRXOrderReport *order, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success = true) override;
    /// 成交回报查询返回结果
    /// <param name="tradeReportList"></param>
	void OnQueryTrade(const TRXTradeReport *tradeReport, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success = true) override;
    /// 可融券查询返回结果
    /// <param name="borrowingSecurityList"></param>
	void OnQueryBorrowingSecurity(const TRXBorrowingSecurity *borrowingSecurity, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success = true) override;
    /// 融券负债查询返回结果
    /// <param name="securityLiabilityList"></param>
	void OnQuerySecurityLiability(const TRXSecurityLiability *securityLiability, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success = true) override;
    /// 融资负债查询返回结果
    /// <param name="financingLiabilityList"></param>
	void OnQueryFinancingLiability(const TRXFinancingLiability *financingLiability, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success = true) override;
    /// 信用资产查询返回结果
    /// <param name="creditAssetList"></param>
	void OnQueryCreditAsset(const TRXCreditAsset *creditAsset, const text_t error_message, const request_id_t request_id, const bool is_last, const bool is_success = true) override;
};

class TradeXSample
{
public:
    std::unordered_map<std::string, std::string> statusMapForQuery;
    int tran_status_code = 0;
    std::string tran_status_message = "";
private:
    TradeXApi *api;
    TradeXCallback callback;

    //普通账户
    trade_unit_t normal_trade_unit;
    //两融账户
    trade_unit_t credit_trade_unit;

    volatile bool is_login;

    std::unordered_map<order_id_t, order_id_t> clOrdId2OrderIdMap;
    std::unordered_map<basket_id_t, basket_id_t> clientBasketId2BasketIdMap;


    std::string log_path;

public:
    TradeXSample();
    ~TradeXSample();

    TradeXApi * getAPI();
    void initialize(const std::string &log_path);
    void close();
    void login(const TRXLoginRequest *login);
    void logout();

    void ETFCreation();
    void ETFRemption();

	void SHOrder();
	void SZOrder();
	void HKOrder();
	void CreditOrder();
	void AlgoOrder();
	void BasketOrders();
	void AlgoBasketOrders();

	void QueryTradeUnit();
	
	void QueryCash();
	void QueryPosition();
	void QueryCreditAsset();
	void QueryOrders();
	void QueryTrades();
	void QuerySecurityLiability();
	void QueryFinancingLiability();
	void QueryBorrowingSecurity();
    
    void initTranStatus()
    {
        tran_status_code = -1;
        tran_status_message = "";
    };
    
    void setTranStatus(int tran_sts, std::string tran_msg)
    {
        std::cout<<"set TranStats:"<<std::endl;
        std::cout<<"tran_status_code:"<<tran_sts<<std::endl<<"tran_status_message:"<<tran_msg<<std::endl;
        tran_status_code = tran_sts;
        tran_status_message = tran_msg;
    }
    
    int PlaceOrder(TRXSingleOrder* pOrder)
    {
        initTranStatus();
        return api->PlaceOrder(pOrder);
    };
    
    int QueryBalance(TRXBalanceQueryRequest * p)
    {
        initTranStatus();
        
        int rtn = api->QueryBalance(p);
        return rtn;
    }
    
    int CancelOrder(TRXOrderCancelRequest * p)
    {
        initTranStatus();
        int rtn = api->CancelOrder(p);
        return rtn;
    }
    
    int QueryStockInHand(TRXPositionQueryRequest *p)
    {
        initTranStatus();
        int rtn = api->QueryPosition(p);
    }
	std::string getTranStatus();

    void set_normal_account(trade_unit_t id) { normal_trade_unit = id; }
    void set_credit_account(trade_unit_t id) { credit_trade_unit = id; }

	void set_login(bool l) {
		this->is_login = l;
	}

	bool is_login_ready() {
		return this->is_login;
	}

    void update_order_id(order_id_t client_order_id, order_id_t order_id)
    {
		auto b = clOrdId2OrderIdMap.find(client_order_id);
		if (b != clOrdId2OrderIdMap.end()) {
			clOrdId2OrderIdMap[client_order_id] = order_id;
		}
		else {
			clOrdId2OrderIdMap.insert(std::make_pair(client_order_id, order_id));
		}
    }
    order_id_t find_order_id(order_id_t client_order_id)
    {
        auto b = clOrdId2OrderIdMap.find(client_order_id);
        if (b != clOrdId2OrderIdMap.end())
        {
            return b->second;
        }

        return 0;
    }
    void update_basket_id(basket_id_t client_basket_id, basket_id_t basket_id)
    {
		auto b = clientBasketId2BasketIdMap.find(client_basket_id);
		if (b != clientBasketId2BasketIdMap.end()) {
			clientBasketId2BasketIdMap[client_basket_id] = basket_id;
		}
		else {
			clientBasketId2BasketIdMap.insert(std::make_pair(client_basket_id, basket_id));
		}
    }
    basket_id_t find_basket_id(basket_id_t client_basket_id)
    {
        auto b = clientBasketId2BasketIdMap.find(client_basket_id);
        if (b != clientBasketId2BasketIdMap.end())
        {
            return b->second;
        }

        return 0;
    }
};

} // namespace sample
} // namespace tradex
} // namespace com