#include <stdlib.h>
#include <thread>
#include <iostream> 
#include "../include/StompConnectionHandler.h"
#include <SocketTask.h>

using namespace std;

int main(int argc, char *argv[])
{
	if (argc < 3)
	{
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    
    StompConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect())
	{
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }
	std::thread outputThread(&StompConnectionHandler::run, &connectionHandler);
	while (1)
    {
        const short bufsize = 1024;
        char buf[bufsize];
        std::cin.getline(buf, bufsize);
		std::string line(buf);
		int len=line.length();
        if (!connectionHandler.sendFrame(line)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }
	}








	return 0;
}