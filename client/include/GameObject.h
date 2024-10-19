#pragma once

#include <string>
#include <map>
#include <list>

using std::string;
using std::map;
using std::list;
using std::pair;

class GameObject
{
    private:
    map<string, string>* generalGameUpdates;
    map<string, string>* teamAUpdates;
    map<string, string>* teamBUpdates;
    list<pair<pair<string,string>, string>>* events;

    public:

    GameObject();

    void updateOrInsertStat(pair<string,string> stat, string title);

    //string generateTitle(string s);
    string generateTime(string s);
    string generateEventName(string s);
    string generateDescription(string s);
    list<list<pair<string,string>>*>* extract(string s);

    void updateGame(string s);
    void summarize(string file, string risha);
};