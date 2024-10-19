#include <iostream>
#include <mutex>

class StompConnectionHandler;

class SocketTask
{
    private:
    StompConnectionHandler* CH;

    public:
        SocketTask(StompConnectionHandler* connectionHandler);
        void run();
};