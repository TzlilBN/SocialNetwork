package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.*;
import bgu.spl.net.api.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.text.html.HTMLDocument.HTMLReader.IsindexAction;

public class StompMessageProtocol implements StompMessagingProtocol<String>
{

    private StompConnections connections;
    private int connectionId;
    private boolean shouldTerminate = false;

    public void start(int connectionId, StompConnections connections)
    {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    public boolean shouldTerminate() {
        return shouldTerminate;
    }
    
    public String[] process(String msg)
    {
        String[] response = new String[4];
        for (int i = 0; i < 4; i++)
            response[i] = null;
        String[] msgByLines = msg.split("\n");
        //deal if there isnt any \n
        //List<String> that holds all the right actions for the error 
        String action = msgByLines[0];
        String receiptNumber = null;
        if (msg.contains("receipt"))
        {
            int receiptIndex = msg.indexOf("receipt");
            int backSlashAfterReceipt = msg.indexOf("\n",receiptIndex);
            receiptNumber = msg.substring(receiptIndex+8, backSlashAfterReceipt);
        }
        int err = 0;
        //Do the Action

        if (action.contentEquals("CONNECT"))
        {
            connect(msg, response, receiptNumber);
            err++;
        }
        if (action.contentEquals("DISCONNECT"))
        {
            disconnect(msg, response, receiptNumber);
            err++;
        }
        if (action.contentEquals("SUBSCRIBE"))
        {
            err++;
            subscribe(msg, response, receiptNumber);
        }
        if (action.contentEquals("UNSUBSCRIBE"))
        {
            err++;
            unsubscribe(msg, response, receiptNumber);
        }
        if (action.contentEquals("SEND"))
        {
            err++;
            send(msg, response, receiptNumber);
        }
        if( response[3]!= null)
            return response;
        if(err==0)
        {
            response[3] = genrateError("StompCommand not recognized", receiptNumber);
            return response;
        }
        if (msg.contains("receipt"))
        {
            int receiptIndex = msg.indexOf("receipt");
            int backSlashAfterReceipt = msg.indexOf("\n",receiptIndex);
            String receiptNUmber = msg.substring(receiptIndex+8, backSlashAfterReceipt);
            response[2] = generateReceipt(receiptNUmber);
        }
        return response;
    }

    public String generateReceipt(String num)
    {
        String receipt;
        receipt = "RECEIPT\n"
                        +"receipt-id:" + num +"\n"
                        +"\n"
                        +"\u0000";
        return receipt;
    }

    public void connect(String msg, String[] response, String receiptNumber)
    {
        String log = generateLogin(msg);
        if (log == null)
        {
            response[3] = genrateError("No login username given", receiptNumber);
            return;
        }
        if(this.connections.activeUsers.contains(log))
        {
            response[3] = genrateError("User already logged in", receiptNumber);
            return;
        }
        String pass = generatePasscode(msg); 
        if (pass == null)
        {
            response[3] = genrateError("No subscription Id given", receiptNumber);
            return;
        }
        boolean amIAlive = false;
        for(StompUser a:connections.users)
        {
            if (a.login.equals(log))
            {
                if(!pass.equals(a.passcode))
                {
                    response[3] = genrateError("Wrong password", receiptNumber);
                    return;
                }
                a.id = connectionId;
                String connectedFrame = "CONNECTED\n" + "version: 1.2\n" + "\n"+ "\u0000";
                response[0] = connectedFrame;
                connections.activeUsers.add(log);
                return;
            }
        }
        StompUser me = new StompUser(connectionId, log, pass);
        connections.users.add(me);
        connections.activeUsers.add(log);
        String connectedFrame = "CONNECTED\n" + "version: 1.2\n" + "\n"+ "\u0000";
        response[0] = connectedFrame;
        return;
        
    }

    public void disconnect(String msg, String[] response, String receiptNumber)
    {
        String username = connections.getUserById(connectionId).login;
        if (receiptNumber == null)
        {
            response[3] = genrateError("There was no receipt on a DISCONNECT frame", receiptNumber);
            return;
        }
        connections.disconnect(username);
    }


    public void subscribe(String msg, String[] response, String receiptNumber)
    {
        //check if user is active
        String sub = generateSubId(msg);
        if (sub == null)
        {
            response[3] = genrateError("No subscription Id given", receiptNumber);
            return;
        }
        Integer subId = Integer.valueOf(sub);
        String topic = generateTopic(msg);
        if (topic == null)
        {
            response[3] = genrateError("No topic given", receiptNumber);
            return;
        }
        boolean gameAlreadyIn = false;
        for(ConcurrentLinkedQueue<Object> game: connections.subsPerTopic)
        {
            if(((String)game.peek()).equals(topic))
            {
                gameAlreadyIn = true;
                game.add(connectionId);
            }
        }
        if(!gameAlreadyIn)
        {
            ConcurrentLinkedQueue<Object> topicN = new ConcurrentLinkedQueue<Object>();
            topicN.add(topic);
            topicN.add(connectionId);
            connections.subsPerTopic.add(topicN);
        }
        StompUser me = connections.getUserById(connectionId);
        me.dictSubToTopic.put(subId, topic);
        me.dictTopicToSub.put(topic, subId);
    }


    public void unsubscribe(String msg, String[] response, String receiptNumber)
    {
        String sub = generateSubId(msg);
        if (sub == null)
        {
            response[3] = genrateError("No subscription Id given", receiptNumber);
            return;
        }
        Integer subId = Integer.valueOf(sub);
        StompUser me = connections.getUserById(connectionId);
        String topic = me.dictSubToTopic.get(subId);
        me.dictSubToTopic.remove(subId);
        me.dictTopicToSub.remove(topic);
        for(ConcurrentLinkedQueue<Object> game: connections.subsPerTopic)
        {
            if(((String)game.peek()).equals(topic))
            {
                game.remove(connectionId);
                return;
            }
        }
    }

    public void send(String msg, String[] response, String receiptNumber)
    {
        
        String topic = generateTopic(msg);
        if (topic == null)
        {
            response[3] = genrateError("No topic given", receiptNumber);
            return;
        }
        //check if the topic exists
        for(ConcurrentLinkedQueue<Object> game: connections.subsPerTopic)
        {
            if(((String)game.peek()).equals(topic))
            {
                if(!game.contains(connectionId))
                {
                    response[3] = genrateError("Cant write to a topic you're not subscribed to", receiptNumber);
                    return;
                }
                response[1] = "1";
                return;
            }
        }
        response[3] = genrateError("There is no such topic", receiptNumber);
    }

    public String generatePasscode(String msg)
    {
        int passcodeIndex = msg.indexOf("passcode:");
        if (passcodeIndex <0)
            return null;
        int backSlashAfterPasscode = msg.indexOf("\n",passcodeIndex);
        String passcode = msg.substring(passcodeIndex+9, backSlashAfterPasscode); 
        return passcode;  
    }

    public String generateSubId(String msg)
    {
        int subIDIndex = msg.indexOf("id:");
        if (subIDIndex <0)
            return null;
        int backSlashAfterSubId = msg.indexOf("\n",subIDIndex);
        String subId = msg.substring(subIDIndex+3, backSlashAfterSubId);
        return subId;   
    }

    public String generateTopic(String msg)
    {
        int topicIndex = msg.indexOf("destination:");
        if (topicIndex <0)
            return null;
        int backSlashAfterTopic = msg.indexOf("\n",topicIndex);
        String topic = msg.substring(topicIndex+13, backSlashAfterTopic);
        return topic;   
    }

    public String generateLogin(String msg)
    {
        int loginIndex = msg.indexOf("login:");
        if (loginIndex <0)
            return null;
        int backSlashAfterLogin = msg.indexOf("\n",loginIndex);
        String login = msg.substring(loginIndex+6, backSlashAfterLogin);
        return login;   
    }

    public String genrateError(String explenation, String receiptId)
    {
        String errorFrame = "ERROR\n" + "message: " + explenation + "\n";
        if (receiptId != null)
            errorFrame = errorFrame +"receipt-id: " + receiptId+"\n\n\u0000";  
        return errorFrame;
    }

}

