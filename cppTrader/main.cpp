
#include "tradex_sample.hpp"

#include <thread>
#include <chrono>
#include <iostream>
#include <string.h>
#include <sstream>

using namespace com::tradex::sample;

int main()
{
    TradeXSample sample;

    sample.initialize("/usr/share/tomcat/logs/");
    sample.set_normal_account(7001028);
    sample.set_credit_account(7001004);

    client_id_t cleint_id = 5001093;
    std::string password = "Admin@123";
    std::string login_url = "219.143.244.232:5570";
    std::string hard_drive = "ST1000DM010-2EP102";
    std::string mac = "8C:EC:4B:52:62:F4";
    std::string ip = "124.127.131.218";

    /*client_id_t cleint_id = 5001093;
    std::string password = "Admin@123";
    std::string login_url = "219.143.244.232:5570";
    std::string hard_drive = "NVMe SAMSUNG MZFLV512";
    std::string mac = "88:B1:11:C9:5B:21";
    std::string ip = "192.168.0.100";*/

    TRXLoginRequest l;

    memset(&l, 0, sizeof(TRXLoginRequest));

    l.client_id = cleint_id;
    strncpy(l.password,password.c_str(),password.size());
    strncpy(l.login_url,login_url.c_str(),login_url.size());
    strncpy(l.hard_drive,hard_drive.c_str(),hard_drive.size());
    strncpy(l.mac,mac.c_str(),mac.size());
    strncpy(l.ip,ip.c_str(),ip.size());

    sample.login(&l);

	while (!sample.is_login_ready()) {
		std::this_thread::sleep_for(std::chrono::seconds(1));
	}

	//sample.AlgoOrder();
	//sample.SHOrder();

	sample.QueryTrades();

	int x;
	std::cin >> x;

	sample.close();
    
	return 0;
}
