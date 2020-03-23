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
	javah -d include -classpath ../src/ com.sn.trader.TradexCpp
	g++ -fPIC -shared -std=c++11 -o ./lib/libTradexCpp.so TradexCpp.cpp -I./include -I/usr/local/software/jdk1.8.0_241/include/ -I/usr/local/software/jdk1.8.0_241/include/linux/ -Xlinker nanolog.o tradex_sample.o -L ./lib -ltradex
	javac ../src/com/sn/trader/TradexCpp.java
	java -Djava.library.path=./lib -cp ../src/ com.sn.trader.TradexCpp
