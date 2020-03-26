default: tradex build_java_so
tradex: main.o nanolog.o tradex_sample.o
	g++ -std=c++11 -o tradex -Xlinker main.o nanolog.o tradex_sample.o -L ./lib  -ltradex
tradex_sample.o: tradex_sample.cpp
	g++ -std=c++11 -fPIC -o tradex_sample.o -c tradex_sample.cpp
nanolog.o: nanolog.cpp
	g++ -std=c++11 -fPIC -o nanolog.o -c  nanolog.cpp
main.o: main.cpp
	g++ -std=c++11 -fPIC -o main.o -c main.cpp
clean:
	rm -f *.o
	rm -f tradex
	rm -f *.class
	rm -f ./include/*.h
	rm -f ./lib/libTradexCpp.so

build_java_so:
	javah -d include -classpath ../src/:../WEB-INF/classes/:../WEB-INF/lib/log4j-1.2.17.jar:../WEB-INF/lib/c3p0-0.9.1.2.jar:../WEB-INF/lib/javax.mail.jar:../WEB-INF/lib/json.jar:../WEB-INF/lib/mysql-connector-java-5.1.48.jar com.sn.trader.TradexCpp
	g++ -fPIC -shared -std=c++11 -o ./lib/libTradexCpp.so TradexCpp.cpp -I./include -I/usr/local/software/jdk1.8.0_241/include/ -I/usr/local/software/jdk1.8.0_241/include/linux/ -Xlinker nanolog.o tradex_sample.o -L ./lib -ltradex
	javac -classpath ../src/:../WEB-INF/classes/:../WEB-INF/lib/log4j-1.2.17.jar:../WEB-INF/lib/c3p0-0.9.1.2.jar:../WEB-INF/lib/javax.mail.jar:../WEB-INF/lib/json.jar:../WEB-INF/lib/mysql-connector-java-5.1.48.jar ../src/com/sn/trader/TradexCpp.java
	java -Djava.library.path=./lib -classpath ../src/:../WEB-INF/classes/:../WEB-INF/lib/log4j-1.2.17.jar:../WEB-INF/lib/c3p0-0.9.1.2.jar:../WEB-INF/lib/javax.mail.jar:../WEB-INF/lib/json.jar:../WEB-INF/lib/mysql-connector-java-5.1.48.jar com.sn.trader.TradexCpp
