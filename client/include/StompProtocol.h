#pragma once

#include "../include/ConnectionHandler.h"
#include <string>
#include <GameObject.h>

using std::vector;
using std::string;

class StompProtocol
{
private:

int counter;
std::string *username;
std::map<std::string, int> *topicToSubId;
std::map<int, int> *mapReceiptIdToSubId;
std::map<int ,std::string> *mapReceiptIdToTopic;
bool terminate;
int disReceipt;
std::map<string ,std::map<string, GameObject*>*> *archive;

public:
//Constructor
StompProtocol();

//Distructor
virtual ~StompProtocol();

std::string prepareSendToServer(std::string action);

void recieveAndAct(std::string frame);

std::string login(std::string s);

std::string join(std::string s);

std::string exit(std::string s);

std::string logout(std::string s);

vector<std::string>* report(std::string s);

void summary(std::string s);

void connected(std::string s);

void HorShahor(std::string s);

void receipt(std::string s);

void error(std::string s);

int generateId();

bool getTerminate();

string generateTopic(string s);

string generateUser(string s);
};
