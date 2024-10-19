#include <SocketTask.h>
#include <StompConnectionHandler.h>

SocketTask::SocketTask(StompConnectionHandler* connectionHandler)
{
    CH = connectionHandler;
}

void SocketTask::run()
{
    CH->run();
}