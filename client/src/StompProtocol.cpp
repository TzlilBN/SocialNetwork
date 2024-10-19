#include "../include/StompProtocol.h"
#include <event.h>

using std::string;
using std::cout;
using std::map;
using std::vector;
using std::pair;

StompProtocol::StompProtocol(): counter(0), username(nullptr), terminate(false), disReceipt(0)
{
    topicToSubId = new map<string,int>;
    mapReceiptIdToSubId = new map<int,int>;
    mapReceiptIdToTopic = new map<int,string>;
    archive = new map<string ,map<string, GameObject*>*>;
}

StompProtocol::~StompProtocol()
{
    topicToSubId->~map();
    mapReceiptIdToSubId->~map();
    mapReceiptIdToTopic->~map();
}

string StompProtocol::prepareSendToServer(std::string action)
{
    std::size_t space = action.find_first_of(' ');
    string command = action.substr(0,space);
    if(command.compare("login") == 0)
    {
        return login(action);
    }
    if(command.compare("join") == 0)
    {
        return join(action);
    }
    if(command.compare("exit") == 0)
    {
        return exit(action);
    }
    if(command.compare("logout") == 0)
    {
        return logout(action);
    }
    if(command.compare("report") == 0)
    {
        return "report";
    }
    if(command.compare("summary") == 0)
    {
        return "summary";
    }
    return action;
}

void StompProtocol::recieveAndAct(std::string frame)
{
    std::size_t space = frame.find("\n");
    string command = frame.substr(0,space);
    if(command.compare("CONNECTED") == 0)
    {
        return connected(frame);
    }
    if(command.compare("MESSAGE") == 0)
    {
        return HorShahor(frame);
    }
    if(command.compare("RECEIPT") == 0)
    {
        return receipt(frame);
    }
    if(command.compare("ERROR") == 0)
    {
        return error(frame);
    }
}

std::string StompProtocol::login(std::string s)
{
    if (username!=nullptr)
    {
        cout<<"The cliet is already logged in, log out before trying again"<<std::endl;
        return "already logged";
    }
    std::size_t space = s.find_first_of(' ');
    space = s.find_first_of(' ',space+1);
    std::size_t space2 = s.find_first_of(' ',space+1);
    string un = s.substr(space+1,space2-space-1);
    username = new string(un);
    string password = s.substr(space2+1);
    string output ="CONNECT\n";
    output.append("accept-version:1.2\n");
    output.append("host:stomp.cs.bgu.ac.il\n");
    output.append("login:"+un+"\n");
    output.append("passcode:" + password+"\n\n\0");
    cout<< "this is the frame sent : " +output<<std::endl;
    return output;
}

std::string StompProtocol::join(string s)
{
    std::size_t space = s.find_first_of(' ');
    string topic = s.substr(space+1);
    int subIdInt = generateId();
    string subId = std::to_string(subIdInt);
    int receiptIdInt = generateId();
    string receiptId = std::to_string(receiptIdInt);
    mapReceiptIdToSubId->insert({receiptIdInt, subIdInt});
    mapReceiptIdToTopic->insert({receiptIdInt, string(topic)});
    string output ="SUBSCRIBE\n";
    output.append("destination:/"+ topic +"\n");
    output.append("id:"+ subId +"\n");
    output.append("receipt:"+receiptId+"\n\n\0");
    return output;
}

std::string StompProtocol::exit(string s)
{
    std::size_t space = s.find_first_of(' ');
    string topic = s.substr(space+1);
    //may not work
    string subId = std::to_string(topicToSubId->at(topic));
    int receiptIdInt = generateId();
    string receiptId = std::to_string(receiptIdInt);
    mapReceiptIdToTopic->insert({receiptIdInt, string(topic)});
    string output ="UNSUBSCRIBE\n";
    output.append("id:"+ subId +"\n");
    output.append("receipt:"+receiptId+"\n\n\0");
    return output;    
}

std::string StompProtocol::logout(string s)
{
    int receiptIdInt = generateId();
    string receiptId = std::to_string(receiptIdInt);
    disReceipt = receiptIdInt;
    string output ="DISCONNECT\n";
    output.append("receipt:"+receiptId+"\n\n\0");
    return output;    
}

vector<string>* StompProtocol::report(string s)
{
    vector<string>* vectorEventsFrames = new vector<string>();
    std::size_t space = s.find_first_of(' ');
    string fileName = s.substr(space+1);
    names_and_events EventsForGame = parseEventsFile(fileName);
    //save the report in the client
    string output ="SEND\n";
    output.append("destination:/"+ EventsForGame.team_a_name+"_"+ EventsForGame.team_b_name +"\n\n");
    output.append("user: " + *username +"\n");
    output.append("team a: " + EventsForGame.team_a_name +"\n");
    output.append("team b: " + EventsForGame.team_b_name +"\n");
    for (Event event:EventsForGame.events)
    {
        string eventFrame = string(output);
        eventFrame.append("event name: " + event.get_name() +"\n");
        eventFrame.append("time: " + std::to_string(event.get_time()) +"\n");
        eventFrame.append("general game updates:\n");
        for (std::pair<string,string> pair:event.get_game_updates())
        {
            eventFrame.append(pair.first + ": " + pair.second + "\n");
        }
        eventFrame.append("team a updates:\n");
        for (std::pair<string,string> pair:event.get_team_a_updates())
        {
            eventFrame.append(pair.first + ": " + pair.second + "\n");
        }
        eventFrame.append("team b updates:\n");
        for (std::pair<string,string> pair:event.get_team_b_updates())
        {
            eventFrame.append(pair.first + ": " + pair.second + "\n");
        }
        eventFrame.append("description:\n");
        eventFrame.append(event.get_discription() +"\n\0");
        vectorEventsFrames->push_back(eventFrame);
    }
    return vectorEventsFrames;
}

void StompProtocol::summary(string s)
{
    std::size_t space = s.find_first_of(' ');
    std::size_t space2 = s.find_first_of(' ', space+1);
    string topic = s.substr(space+1, space2-space-1);
    space = space2;
    space2 = s.find_first_of(' ', space+1);
    string user = s.substr(space+1, space2-space-1);
    string file = s.substr(space2+1);
    GameObject* game = archive->at(topic)->at(user);
    std::size_t makafInd = topic.find_first_of('_');
    string risha("");
    risha.append(topic.substr(0,makafInd) + " vs " +    topic.substr(makafInd+1) + " \n");
    game->summarize(file, risha);
}

void StompProtocol::connected(string s)
{
    std::cout<<"Login Succesful"<<std::endl;
}

void StompProtocol::HorShahor(string s)
{
    string topic = generateTopic(s);
    string user = generateUser(s);
    if (archive->at(topic)->count(user) == 0)
        archive->at(topic)->insert({user,new GameObject()});
    GameObject* game = archive->at(topic)->at(user);
    game->updateGame(s);
}

void StompProtocol::receipt(string s)
{
    std::size_t nekudotaim = s.find_first_of(':');
    std::size_t backslash = s.find("\n");
    backslash = s.find("\n", backslash+1);
    string receiptId = s.substr(nekudotaim+1, backslash-nekudotaim-1);
    int receiptIdInt = stoi(receiptId);
    if (receiptIdInt == disReceipt)
    {
        terminate = true;
        return;
    }
    string topic = mapReceiptIdToTopic->at(receiptIdInt);
    if(topicToSubId->count(topic)>0)
    {
        topicToSubId->erase(topic);
        archive->erase(topic);
        cout<<"Exited channel " + topic<<std::endl;
    }
    else
    {
        topicToSubId->insert({topic, mapReceiptIdToSubId->at(receiptIdInt)});
        archive->insert({topic, new map<string,GameObject*>});
        cout<<"Joined channel " + topic<<std::endl;
    }
    mapReceiptIdToSubId->erase(receiptIdInt);
    mapReceiptIdToTopic->erase(receiptIdInt);
}

void StompProtocol::error(string s)
{
    std::size_t message = s.find("message: ");
    std::size_t newLine = s.find("\n", message+1);
    string output = s.substr(message+9,newLine-message-9);
    cout<<output<<std::endl;
}

int StompProtocol::generateId()
{
    int output = counter;
    counter = counter + 1;
    return counter;
}

bool StompProtocol::getTerminate()
{
    return terminate;
}

string StompProtocol::generateTopic(string s)
{
    std::size_t topicInd = s.find("destination:/");
    std::size_t newLine = s.find('\n', topicInd+1);
    string output = s.substr(topicInd+13,newLine-topicInd-13);
    return output;

}

string StompProtocol::generateUser(string s)
{
    std::size_t UserInd = s.find("user:");
    std::size_t newLine = s.find('\n', UserInd+1);
    string output = s.substr(UserInd+6,newLine-UserInd-6);
    return output;
}