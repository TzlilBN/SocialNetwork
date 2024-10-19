package bgu.spl.net.impl.stomp;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.srv.Connections;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;;

public class StompConnections implements Connections<String>{

     protected ConcurrentLinkedQueue<StompUser> users;
     protected ConcurrentLinkedQueue<String> activeUsers;
     protected ConcurrentLinkedQueue<ConcurrentLinkedQueue<Object>> subsPerTopic;
     protected ConcurrentHashMap<Integer, ConnectionHandler<String>> idToConnectionHandler;
     protected AtomicInteger counterUserId;
     protected AtomicInteger counterMsgU;

     public StompConnections()
     {
        users = new ConcurrentLinkedQueue<StompUser>();
        activeUsers = new ConcurrentLinkedQueue<String>();
        subsPerTopic = new ConcurrentLinkedQueue<ConcurrentLinkedQueue<Object>>();
        HashMap<Integer, ConnectionHandler<String>> map = new HashMap<Integer, ConnectionHandler<String>>();
        idToConnectionHandler = new ConcurrentHashMap(map);
        counterUserId = new AtomicInteger(0);
        counterMsgU = new AtomicInteger(0);
     }

     public void send(int connectionId, String msg)
     {
        idToConnectionHandler.get(connectionId).send(msg);
     }

     public void send(String channel, String seifa)
     {
        ConcurrentLinkedQueue<Object> me = null;
        for(ConcurrentLinkedQueue<Object> a: subsPerTopic)
        {
            if (((String)(a.peek())).equals(channel))
            {
                me = a;
                break;
            }
        }
        Iterator<Object> iter2 = me.iterator();
        iter2.next();
        while(iter2.hasNext())
        {
            int connectionId = (Integer)(iter2.next());
            StompUser b = null;
            for (StompUser x: users)
                if(x.id == connectionId)
                    b = x;
            idToConnectionHandler.get(connectionId).sendMessege(seifa, generateNewMId());
        }
     }

     public void disconnect(String connectionUser)
     {
        Iterator<ConcurrentLinkedQueue<Object>> iter = subsPerTopic.iterator();
        while(iter.hasNext())
        {
            iter.next().remove(connectionUser);
        }
        activeUsers.remove(connectionUser);
    }


    public StompUser getUserById(int id)
    {
        for(StompUser a:users)
            if (a.id == id)
                return a;
        return null;
    }

    public Integer generateNewId()
    {
        int num = counterUserId.get();
        counterUserId.set(num+1);
        return num;
    }

    public Integer generateNewMId()
    {
        int num = counterMsgU.get();
        counterMsgU.set(num+1);
        return num;
    }

    public ConcurrentHashMap<Integer, ConnectionHandler<String>> getIdToConnectionHandler()
    {
        return idToConnectionHandler;
    }
}
