#include <GameObject.h>
#include <iostream>
#include <fstream>

using namespace std;
using std::string;
using std::map;
using std::list;
using std::pair;

GameObject::GameObject()
{
    generalGameUpdates = new map<string, string>;
    teamAUpdates = new map<string, string> ;
    teamBUpdates = new map<string, string> ;
    events = new list<pair<pair<string,string>, string>>;
}

void GameObject::updateOrInsertStat(pair<string,string> stat ,string title)
{
    if (title.compare("general") == 0)
    {
        if(generalGameUpdates->count(stat.first)>0)
            generalGameUpdates->erase(stat.first);
        generalGameUpdates->insert(stat);
    }
    if (title.compare("a") == 0)
    {
        if(teamAUpdates->count(stat.first)>0)
            teamAUpdates->erase(stat.first);
        teamAUpdates->insert(stat);
    }
    if (title.compare("b") == 0)
    {
        if(teamBUpdates->count(stat.first)>0)
            teamBUpdates->erase(stat.first);
        teamBUpdates->insert(stat);   
    }
}

/*string GameObject::generateTitle(string s)
{
    std::size_t titleInd = s.find_first_of("general game updates:");
    if (titleInd > 0)
        return "general";
    titleInd = s.find_first_of("team a updates:");
     if (titleInd > 0)
        return "a";
    titleInd = s.find_first_of("team b updates:");
     if (titleInd > 0)
        return "b";
    return "end";
}*/

string GameObject::generateTime(string s)
{
    std::size_t timeInd = s.find("time: ");
    std::size_t newLine = s.find("\n", timeInd);
    string output = s.substr(timeInd+6,newLine-timeInd-6);
    return output;
}

string GameObject::generateEventName(string s)
{
    std::size_t eventNameInd = s.find("event name: ");
    std::size_t newLine = s.find("\n", eventNameInd);
    string output = s.substr(eventNameInd+12,newLine-eventNameInd-12);
    return output;
}

string GameObject::generateDescription(string s)
{
    std::size_t desInd = s.find("description:");
    std::size_t newLine = s.find("\n", desInd);
    std::size_t newLine2 = s.find("\n", newLine+1);
    string output = s.substr(newLine+1,newLine2-newLine-1);
    return output;
}

list<list<pair<string,string>>*>* GameObject::extract(string s)
{
    list<list<pair<string,string>>*>* pairsList = new list<list<pair<string,string>>*>();
    list<pair<string,string>>* generalPairsList = new list<pair<string,string>>();
    std::size_t generalInd = s.find("general game updates:");
    std::size_t AInd = s.find("team a updates:");
    string toPairs = s.substr(generalInd+22,AInd-generalInd-22);
    std::size_t endLine = toPairs.find("\n");
    std::size_t startLine = 0;
    std::size_t nekudotaim = 0;
    while (endLine< toPairs.length())
    {
        string pairStr = toPairs.substr(startLine, endLine - startLine);
        nekudotaim = pairStr.find_first_of(':');
        pair<string, string> currPair(pairStr.substr(0, nekudotaim), pairStr.substr(nekudotaim +2, pairStr.length() -nekudotaim-2));
        generalPairsList->push_back(currPair);
        startLine = endLine +1;
        endLine = toPairs.find("\n", startLine);
    }
    pairsList->push_back(generalPairsList);
    list<pair<string,string>>* aPairsList = new list<pair<string,string>>();
    std::size_t BInd = s.find("team b updates:");
    toPairs = s.substr(AInd+16,BInd-AInd-16);
    endLine = toPairs.find("\n");
    startLine = 0;
    nekudotaim = 0;
    while (endLine < toPairs.length())
    {
        string pairStr = toPairs.substr(startLine, endLine - startLine);
        nekudotaim = pairStr.find_first_of(':');
        pair<string, string> currPair(pairStr.substr(0, nekudotaim), pairStr.substr(nekudotaim +2, pairStr.length() -nekudotaim-2 ));
        aPairsList->push_back(currPair);
        startLine = endLine +1;
        endLine = toPairs.find("\n", startLine);
    }
    pairsList->push_back(aPairsList);
    list<pair<string,string>>* bPairsList = new list<pair<string,string>>();
    std::size_t desInd = s.find("description:");
    toPairs = s.substr(BInd+16,desInd-BInd-16);
    endLine = toPairs.find("\n");
    startLine = 0;
    nekudotaim = 0;
    while (endLine < toPairs.length())
    {
        string pairStr = toPairs.substr(startLine, endLine - startLine);
        nekudotaim = pairStr.find_first_of(':');
        pair<string, string> currPair(pairStr.substr(0, nekudotaim), pairStr.substr(nekudotaim +2, pairStr.length() -nekudotaim-2));
        bPairsList->push_back(currPair);
        startLine = endLine +1;
        endLine = toPairs.find("\n", startLine);
    }
    pairsList->push_back(bPairsList);

    return pairsList;
}

void GameObject::updateGame(string s)
{
    list<list<pair<string,string>>*>* updates = extract(s);
    int i =0;
    string title = "general";
    for (list<pair<string,string>>* currList: *updates)
    {
        if (i==1)
            title = "a";
        if (i==2)
            title ="b";
        for (pair<string,string> stat: *currList)
        {
            updateOrInsertStat(stat, title);
        }
        i = i +1;
    }
    events->push_back({{generateTime(s),generateEventName(s)},generateDescription(s)});
}

void GameObject::summarize(string file, string risha)
{
    std::size_t vsInd = risha.find(" vs ");
    std::size_t newLineInd = risha.find("\n");
    string aName = risha.substr(0,vsInd);
    string bName = risha.substr(vsInd +4,newLineInd-vsInd-5);
    risha.append("Game stats:\n");
    risha.append("General stats:\n");
    for (pair<string,string> stat: *generalGameUpdates)
        risha.append(stat.first + ": " + stat.second +"\n");
    risha.append("\n");
    risha.append( aName + " stats:\n");
    for (pair<string,string> stat: *teamAUpdates)
        risha.append(stat.first + ": " + stat.second +"\n");
    risha.append("\n");
    risha.append(bName + " stats:\n");
    for (pair<string,string> stat: *teamBUpdates)
        risha.append(stat.first + ": " + stat.second +"\n");
    risha.append("\n");
    risha.append("Game event reports: \n");
    for (pair<pair<string,string>, string> event: *events)
    {
        risha.append(event.first.first + " - " + event.first.second +"\n\n");
        risha.append(event.second + "\n");
        risha.append("\n");
    }

    ofstream summaryFile;
    summaryFile.open(file);
    summaryFile << risha;
    summaryFile.close();
    //risha is ready as a summary, I have no idea how to write it into file
    
}
