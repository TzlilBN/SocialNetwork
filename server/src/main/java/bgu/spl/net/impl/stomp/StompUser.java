package bgu.spl.net.impl.stomp;

import java.util.HashMap;

public class StompUser {

    protected Integer id;
    protected String login;
    protected String passcode;

    protected HashMap<Integer, String> dictSubToTopic;
    protected HashMap<String, Integer> dictTopicToSub;

    public StompUser(int id, String username, String password)
    {
        this.id = id;
        this.login = username;
        this.passcode = password;
        dictSubToTopic = new HashMap<>();
        dictTopicToSub = new HashMap<>();
    }

    public void addToDict(int subId, String Topic)
    {
        dictSubToTopic.put(subId, Topic);
    }

    public HashMap<String, Integer> getTopicToSub()
    {
        return dictTopicToSub;
    }

    public String getUsername()
    {
        return login;
    }
    
}
