CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lpthread

all: StompWCIClient

EchoClient: bin/ConnectionHandler.o bin/echoClient.o
	g++ -o bin/EchoClient bin/ConnectionHandler.o bin/echoClient.o $(LDFLAGS)

StompWCIClient: bin/StompConnectionHandler.o bin/StompClient.o bin/event.o bin/StompProtocol.o bin/GameObject.o
	g++ -o bin/StompWCIClient bin/StompConnectionHandler.o bin/StompClient.o bin/event.o bin/StompProtocol.o bin/GameObject.o $(LDFLAGS)

bin/ConnectionHandler.o: src/ConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/ConnectionHandler.o src/ConnectionHandler.cpp

bin/echoClient.o: src/echoClient.cpp
	g++ $(CFLAGS) -o bin/echoClient.o src/echoClient.cpp

bin/event.o: src/event.cpp
	g++ $(CFLAGS) -o bin/event.o src/event.cpp

bin/StompClient.o: src/StompClient.cpp
	g++ $(CFLAGS) -o bin/StompClient.o src/StompClient.cpp

bin/StompConnectionHandler.o: src/StompConnectionHandler.cpp
	g++ $(CFLAGS) -o bin/StompConnectionHandler.o src/StompConnectionHandler.cpp

bin/StompProtocol.o: src/StompProtocol.cpp
	g++ $(CFLAGS) -o bin/StompProtocol.o src/StompProtocol.cpp

bin/GameObject.o: src/GameObject.cpp
	g++ $(CFLAGS) -o bin/GameObject.o src/GameObject.cpp

.PHONY: clean
clean:
	rm -f bin/*
	