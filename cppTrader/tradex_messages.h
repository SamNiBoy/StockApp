/////////////////////////////////////////////////////////////////////////
///@system  TradeX新一代交易系统
///@company 华创证券有限责任公司
///@brief   接口业务消息类型
///@author  cuiyc
/////////////////////////////////////////////////////////////////////////

#ifndef _TRADEX_API_MESSAGES_
#define _TRADEX_API_MESSAGES_

#include "tradex_types.h"

namespace com
{
namespace tradex
{
namespace api
{
/////////////////////////////////////////////////////////////////////
///登录请求数据结构
/////////////////////////////////////////////////////////////////////
struct TRXLoginRequest
{
    client_id_t client_id;   //登录用户名
    password_t password;     //登录密码
    url_t login_url;         //认证服务器的地址，格式为[ip:port]
    ip_t ip;                 //本机IP地址
    mac_t mac;               //本机MAC地址
    hard_drive_t hard_drive; //硬盘序列号
    text_t text;             //其他信息，可不填
};
/////////////////////////////////////////////////////////////////////
///登录返回结果
/////////////////////////////////////////////////////////////////////
struct TRXLoginResponse
{
    bool is_success;      //是否登录成功，TRUE：成功；FALSE：失败
    text_t error_message; //登录失败原因
    session_t session_id; //登录成功后服务器返回的SessionID，用于后续报单
};
/////////////////////////////////////////////////////////////////////
///订单的算法参数
/////////////////////////////////////////////////////////////////////
struct TRXAlgoParameters
{
    algo_name_t algo_name;                  // 算法名称: TWAP, VWAP, STRICTTWAP,POV,VWAPPLUS,PEGGING
    timestamp_t start_time;                 // 算法订单开始时间，格式为YYYYMMDDHHMMSS
    timestamp_t end_time;                   // 算法订单结束时间，格式为YYYYMMDDHHMMSS
    double participation_rate;              // 参与率，值范围:0到100之间，30表示市场占比30%，0为不限制市场占比
    int style;                              // 交易松紧度,取值分别为：1，2，3，其中由小到大表示越来越“紧”，即交易过程中容忍的偏移量越来越小
    bool am_auction_flag;                   // 参与开盘竞价,TRUE为参与开盘竞价
    bool close_auction_flag;                // 参与收盘竞价,TRUE为参与收盘竞价;
    bool count_eligible_volume_limit_price; // 限价内占比,限价内占比为“true”，则算法计算的订单占比为此时间段限价范围内的成交量占比；限价内占比为“false”，则算法计算的订单占比为此时间段市场成交总量的占比。当价格类型选择为市价时，该参数不起作用
    int display_order_size;                 // 单次委托量,PEGGING算法独有的参数，每次挂单的数量，默认为0时算法会根据股票微观结构进行设置
    double ratio_of_random;                 // 委托随机偏移值,PEGGING算法独有的参数，对每次委托量增加随机偏移比例的设置
    int min_trade_inetrval;                 // 最小委托间隔，PEGGING算法独有的参数，当股价活跃时为防止频繁委托需要设置的参数，该参数默认为A股行情更新间隔3秒
    bool buy_on_up_limit;                   // 涨停全部买入,风控参数，默认值为Y，即股票涨停时将剩余量全部委托出去
    bool sell_on_up_limit;                  // 涨停全部卖出
    bool buy_on_down_limit;                 // 跌停全部买入
    bool sell_on_down_limit;                // 跌停全部卖出
    int customized_minute;                  // 自定义时间,风控参数，默认值为0，与customized_pct_of_px同时使用，customized_minute分钟，价格波动超过customized_pct_of_px%暂停交易
    double customized_pct_of_px;            // 自定义波动比例,风控参数，默认值为0，与customized_minute同时使用，customized_minute分钟，价格波动超过customized_pct_of_px%暂停交易
    double cancel_rate;                     // 撤单率控制,风控参数，默认值为49，该参数会对母单的撤单率进行控制，默认是控制在小于等于49%。
    bool condition_control;                 // 是否启用条件控制:是否启用条件单，默认值为N
    symbol_t reference_index;               // 条件单跟踪合约名称,进行条件委托时该参数必填，默认值为空，交易市场与合约标的拼接字符串,如000001SZ(平安银行)
    int trading_condition;                  // 触发条件:条件单时必填，0表示大于；1表示大于等于；2表示小于；3表示小于等于；
    bool is_absolute_px;                    // 是否使用绝对价格跟踪,条件单时必填，Y表示跟踪绝对价格，此时必须填ref_price；N表示按照类型跟踪，此时必须填px_type
    double ref_price;                       // 跟踪合约的价格，条件单时根据is_absolute_px情况进行填写
    double relative_price_limit_offset;     // 跟踪合约的相对价格偏移,条件单时必填，"-1"表示偏移"-1%"的价格
    algo_price_type_t px_type;              // 跟踪价格的类型(条件单时根据is_absolute_px情况进行填写):PC:前收盘价、OP:开盘价、P:最新价、HP:最高价、LP:最低价、ULP:涨停价、DLP:跌停价、BP1:买1价、BV1:买1量
    bool auto_pause_resume;                 // 自动暂停继续,条件单时必填,当条件变为不再满足时，母单自动暂停直到再次满足条件再自动继续
    bool eligible_to_trading;               // 满足条件后执行/暂停,条件单时必填,触发条件时true为开始执行交易
};
/////////////////////////////////////////////////////////////////////
///新建单个委托的数据结构
/////////////////////////////////////////////////////////////////////
struct TRXSingleOrder
{
    trade_unit_t trade_unit;                    // 交易单元
    order_id_t client_order_id;                 // 委托号，调用方负责设置，并保证唯一性
    symbol_t symbol;                            // 交易标的代码，需填入交易所认可的交易标的代码
    TRXMarket market;                           // 此订单的交易市场
    price_t price;                              // 订单价格
    quantity_t quantity;                        // 订单数量
    TRXSide side;                               // 买卖方向，及两融操作：担保品买入、担保品卖出、融资买入、融券卖出、买券还券、卖券还款
    TRXOpenClose open_close;                    // 开平标志
    TRXPriceType price_type;                    // 订单价格类型
    TRXCreditPositionType credit_position_type; // 信用头寸类型
    TRXHedgeFlag hedge_flag;                    // 股指期货投机套保标志
    TRXHKOrderLotType hk_stock_lot_type;        // 港股整股、零股设置
    TRXAlgoParameters *algo_parameters;         // 算法单相关的参数
};
/////////////////////////////////////////////////////////////////////
///篮子订单成员信息
/////////////////////////////////////////////////////////////////////
struct TRXBasketLeg
{
    order_id_t client_order_id;                 // 委托号，调用方负责设置，并保证唯一性
    symbol_t symbol;                            // 交易标的代码，需填入交易所认可的交易标的代码
    TRXMarket market;                           // 此订单的交易市场
    price_t price;                              // 订单价格
    quantity_t quantity;                        // 订单数量
    TRXSide side;                               // 买卖方向，及两融操作：担保品买入、担保品卖出、融资买入、融券卖出、买券还券、卖券还款
    TRXOpenClose open_close;                    // 开平标志
    TRXPriceType price_type;                    // 订单价格类型
    TRXCreditPositionType credit_position_type; // 信用头寸类型
    TRXHedgeFlag hedge_flag;                    // 股指期货投机套保标志
    TRXHKOrderLotType hk_stock_lot_type;        // 港股整股、零股设置
};
/////////////////////////////////////////////////////////////////////
///新建篮子委托的数据结构
/////////////////////////////////////////////////////////////////////
struct TRXBasketOrder
{
public:
    trade_unit_t trade_unit;            // 交易单元
    basket_id_t client_basket_id;       // 篮子委托的委托号，调用方负责设置，并保证唯一性
    TRXAlgoParameters *algo_parameters; // 算法单相关的参数
    uint16_t leg_count;                 // 篮子委托的成员数量
    TRXBasketLeg legs[1000];            // 篮子委托的成员信息
};
/////////////////////////////////////////////////////////////////////
///单个订单撤单请求
/////////////////////////////////////////////////////////////////////
struct TRXOrderCancelRequest
{
    trade_unit_t trade_unit; // 交易单元
    order_id_t order_id;     // 被撤委托的订单ID，该ID由系统返回
};
/////////////////////////////////////////////////////////////////////
///篮子委托撤单请求，撤整个篮子
/////////////////////////////////////////////////////////////////////
struct TRXBasketCancelRequest
{
    trade_unit_t trade_unit; // 交易单元
    basket_id_t basket_id;   // 被撤的篮子委托的ID
};
/////////////////////////////////////////////////////////////////////
///撤单请求被拒绝消息
/////////////////////////////////////////////////////////////////////
struct TRXOrderCancelReject
{
    order_id_t client_order_id;   // 被撤委托的ID， 此ID是调用方填写的委托ID
    order_id_t order_id;          // 被撤委托的订单ID，该ID由系统返回
    basket_id_t client_basket_id; // 用户定义的篮子委托ID
    basket_id_t basket_id;        // 篮子委托ID
    order_id_t parent_order_id;   // 母单(算法)ID
    TRXOrderStatus order_status;  // 订单状态
    text_t error_message;         // 被拒绝的原因
};

struct TRXOrderReport
{
    client_id_t client_id;                      // 登录用户名
    trade_unit_t trade_unit;                    // 交易单元
    order_id_t client_order_id;                 // 调用方填写的委托ID
    symbol_t symbol;                            // 交易标的代码，需填入交易所认可的交易标的代码
    order_id_t order_id;                        // 委托的ID，该ID由系统返回
    basket_id_t basket_id;                      // 篮子委托ID
    basket_id_t client_basket_id;               // 用户定义的篮子委托ID
    order_id_t parent_order_id;                 // 母单(算法)ID
    bool is_parent_order;                       // 当前订单是否为(算法)母单
    bool is_child_order;                        // 当前订单是否为(算法)子单
    TRXMarket market;                           // 此订单的交易市场
    price_t price;                              // 订单价格
    quantity_t quantity;                        // 订单数量
    TRXSide side;                               // 买卖方向，及两融操作：担保品买入、担保品卖出、融资买入、融券卖出、买券还券、卖券还款
    TRXOpenClose open_close;                    // 开平标志
    TRXPriceType price_type;                    // 订单价格类型
    TRXCreditPositionType credit_position_type; // 信用头寸类型
    TRXHedgeFlag hedge_flag;                    // 股指期货投机套保标志
    TRXHKOrderLotType hk_stock_lot_type;        // 港股整股、零股设置
    TRXOrderStatus order_status;                // 订单状态
    price_t avg_price;                          // 平均成交价
    quantity_t cum_qty;                         // 累计成交数量
    quantity_t leaves_qty;                      // 剩余数量
    timestamp_t order_time;                     // 订单产生时间
    timestamp_t transact_time;                  // 消息产生时间
    money_t total_trading_amount;               // 累计成交额
    text_t error_message;                       // 拒绝原因
    TRXAlgoParameters *algo_parameters;         // 算法单相关的参数
    client_id_t approver;                       // 审批人
    timestamp_t approval_time;                  // 审批时间
    TRXApprovalStatus approval_status;          // 审批状态
};

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
};

struct TRXTradeUnitConnStatusNotice
{
    trade_unit_t trade_unit;               // 交易单元
    account_t account;                     // 资金账号
    TRXTradeUnitConnectStatus conn_status; // 交易单元连接状态
    TRXTradeUnitStatus status;             // 交易单元状态
    text_t message;                        // 连接状态额外信息
};
/////////////////////////////////////////////////////////////////////
///委托查询请求
/////////////////////////////////////////////////////////////////////
struct TRXOrderQueryRequest
{
    request_id_t request_id;    // 请求编号，由调用方设置，保证唯一性
    trade_unit_t trade_unit;    // 交易单元
    order_id_t client_order_id; // 调用方设置的委托号
    order_id_t order_id;        // 系统返回的委托号
};
/////////////////////////////////////////////////////////////////////
///查询账户资金余额请求
/////////////////////////////////////////////////////////////////////
struct TRXBalanceQueryRequest
{
    request_id_t request_id; // 请求编号，由调用方设置，保证唯一性
    trade_unit_t trade_unit; // 交易单元
};
/////////////////////////////////////////////////////////////////////
///查询账户持仓请求
/////////////////////////////////////////////////////////////////////
struct TRXPositionQueryRequest
{
    request_id_t request_id; // 请求编号，由调用方设置，保证唯一性
    trade_unit_t trade_unit; // 交易单元
    symbol_t symbol;         // 交易标的代码，需填入交易所认可的交易标的代码
};
/////////////////////////////////////////////////////////////////////
///查询成交回报请求
/////////////////////////////////////////////////////////////////////
struct TRXTradeQueryRequest
{
    request_id_t request_id;    // 请求编号，由调用方设置，保证唯一性
    trade_unit_t trade_unit;    // 交易单元
    order_id_t client_order_id; // 调用方设置的委托号
    order_id_t order_id;        // 系统返回的委托号
};
/////////////////////////////////////////////////////////////////////
///信用资产查询请求
/////////////////////////////////////////////////////////////////////
struct TRXCreditAssetQueryRequest
{
    request_id_t request_id; // 请求编号，由调用方设置，保证唯一性
    trade_unit_t trade_unit; // 交易单元
};
/////////////////////////////////////////////////////////////////////
///可融券查询请求
/////////////////////////////////////////////////////////////////////
struct TRXBorrowingSecurityQueryRequest
{
    request_id_t request_id;             // 请求编号，由调用方设置，保证唯一性
    trade_unit_t trade_unit;             // 交易单元
    TRXCreditPositionType position_type; // 头寸性质
};
/////////////////////////////////////////////////////////////////////
///融券负债信息查询请求
/////////////////////////////////////////////////////////////////////
struct TRXSecurityLiabilityQueryRequest
{
    request_id_t request_id; // 请求编号，由调用方设置，保证唯一性
    trade_unit_t trade_unit; // 交易单元
    symbol_t symbol;         // 交易标的代码，需填入交易所认可的交易标的代码
};
/////////////////////////////////////////////////////////////////////
///融资负债查询信息请求
/////////////////////////////////////////////////////////////////////
struct TRXFinancingLiabilityQueryRequest
{
    request_id_t request_id; // 请求编号，由调用方设置，保证唯一性
    trade_unit_t trade_unit; // 交易单元
    symbol_t symbol;         // 交易标的代码，需填入交易所认可的交易标的代码
};
/////////////////////////////////////////////////////////////////////
///交易单元连接状态查询
/////////////////////////////////////////////////////////////////////
struct TRXTradeUnitStatusQueryRequest
{
    request_id_t request_id; // 请求编号，由调用方设置，保证唯一性
    trade_unit_t trade_unit; // 交易单元
};
/////////////////////////////////////////////////////////////////////
///查询出的持仓信息
/////////////////////////////////////////////////////////////////////
struct TRXPosition
{
    trade_unit_t trade_unit;  // 交易单元
    symbol_t symbol;          // 交易标的代码，需填入交易所认可的交易标的代码
    TRXSide side;             // 买卖方向，期货及期权
    TRXHedgeFlag hedge_flag;  // 股指期货投机套保标志
    TRXMarket market;         // 此持仓的交易市场
    quantity_t yesterday_qty; // 昨日持仓
    quantity_t latest_qty;    // 最新持仓
    quantity_t available_qty; // 可用数量
    quantity_t frozen_qty;    // 冻结数量
    money_t margin;           // 保证金(期货、期权)
};
/////////////////////////////////////////////////////////////////////
///交易单元资金查询结果
/////////////////////////////////////////////////////////////////////
struct TRXBalance
{
    trade_unit_t trade_unit;        // 交易单元
    money_t initial_balance;        // 日初资金，资金划转、出入金会影响该值，否则该值为日初或昨日结算后资金余额
    money_t available_balance;      // 可用资金
    money_t frozen_balance;         // 冻结资金
    money_t margin;                 // 保证金(期货、期权)
    money_t hk_available_balance;   // 港股可用资金
    money_t market_value;           // 证券市值
    money_t total_asset;            // 总资产
    money_t withdrawable_balance;   // 可取资金
    money_t available_margin;       // 可用保证金(两融)
    money_t normal_borrowing_cash;  // 普通头寸可融资金
    money_t special_borrowing_cash; // 专项头寸可融资金
};
/////////////////////////////////////////////////////////////////////
///两融可融券信息查询结果
/////////////////////////////////////////////////////////////////////
struct TRXBorrowingSecurity
{
    trade_unit_t trade_unit;             // 交易单元
    symbol_t symbol;                     // 交易标的代码，需填入交易所认可的交易标的代码
    TRXMarket market;                    // 交易市场
    ratio_t security_borrowing_ratio;    // 融券保证金比例
    quantity_t available_to_borrow_qty;  // 可用数量
    TRXCreditPositionType position_type; // 头寸性质
    TRXSecurityStatus security_status;   // 融券状态
};
/////////////////////////////////////////////////////////////////////
///信用资产查询结果
/////////////////////////////////////////////////////////////////////
struct TRXCreditAsset
{
    trade_unit_t trade_unit;             // 交易单元
    money_t guaranty_asset;              // 担保资产
    money_t total_debit;                 // 负债总额
    money_t net_asset;                   // 净资产
    money_t market_value;                // 证券市值
    money_t cash_asset;                  // 现金资产
    ratio_t maintenance_ratio;           // 维持担保比例
    money_t available_margin;            // 可用保证金
    money_t withdraw_quota;              // 可转出资金
    money_t available_buy_collateral;    // 买担保品可用资金
    money_t available_buy_repay;         // 买券还券可用资金
    money_t margin_buy_limit;            // 融资额度上限
    money_t available_margin_buy;        // 融资可用额度
    money_t used_margin_buy_quota;       // 融资已用额度
    money_t margin_buy_interest;         // 融资合约利息
    money_t margin_buy_market_value;     // 融资市值
    money_t short_sell_limit;            // 融券额度上限
    money_t available_to_short_sell;     // 融券可用额度
    money_t used_short_sell_quota;       // 融券已用额度
    money_t short_sell_interest;         // 融券合约利息
    money_t short_sell_market_value;     // 融券市值
    money_t collateral_conversion_asset; // 证券担保折算资产
    money_t short_sell_balance;          // 融券卖出所得总额
    money_t margin_buy_ratio;            // 融资保证金率
};
/////////////////////////////////////////////////////////////////////
///融券负债
/////////////////////////////////////////////////////////////////////
struct TRXSecurityLiability
{
    trade_unit_t trade_unit;                    // 交易单元
    symbol_t symbol;                            // 交易标的代码，需填入交易所认可的交易标的代码
    trade_id_t contract_code;                   // 合约编号
    timestamp_t open_date;                      // 合约日期
    money_t total_debit;                        // 负债总额
    TRXContractStatus contract_status;          // 合约状态
    order_id_t order_id;                        // 委托编号
    price_t trade_price;                        // 成交价格
    quantity_t trade_qty;                       // 成交数量
    money_t trade_turnover;                     // 成交金额
    money_t trade_charges;                      // 成交手续费
    money_t interest_paid;                      // 已还利息
    quantity_t qty_paid;                        // 已还数量
    money_t contract_interest;                  // 合约利息金额
    ratio_t annual_rate;                        // 年利率
    timestamp_t return_deadline;                // 归还截止日
    timestamp_t renewal_due_date;               // 续签到期日期
    TRXCreditPositionType credit_position_type; // 负债头寸类型，普通，专项
};
/////////////////////////////////////////////////////////////////////
///融资负债
/////////////////////////////////////////////////////////////////////
struct TRXFinancingLiability
{
    trade_unit_t trade_unit;                    // 交易单元
    symbol_t symbol;                            // 交易标的代码，需填入交易所认可的交易标的代码
    trade_id_t contract_code;                   // 合约编号
    timestamp_t open_date;                      // 合约日期
    money_t total_debit;                        // 负债总额
    TRXContractStatus contract_status;          // 合约状态
    order_id_t order_id;                        // 委托编号
    price_t trade_price;                        // 成交价格
    quantity_t trade_qty;                       // 成交数量
    money_t trade_turnover;                     // 成交金额
    money_t trade_charges;                      // 成交手续费
    money_t interest_paid;                      // 已还利息
    money_t fund_paid;                          // 已还金额
    money_t contract_interest;                  // 合约利息金额
    ratio_t annual_rate;                        // 年利率
    timestamp_t return_deadline;                // 归还截止日
    timestamp_t renewal_due_date;               // 续签到期日期
    TRXCreditPositionType credit_position_type; // 负债头寸类型，普通，专项
};

} // namespace api

} // namespace tradex
} // namespace com

#endif