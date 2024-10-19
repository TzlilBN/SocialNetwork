package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.Connections;
import bgu.spl.net.srv.Server;

public class StompServer {

    public static void main(String[] args)
    {
        StompConnections connections = new StompConnections();

        if (args[1].equals("tpc"))
        {
            Server.threadPerClient(
                7777, //port
                () -> new StompMessageProtocol(), //protocol factory
                StompMessageEncoderDecoder::new,//message encoder decoder factory
                connections 
            ).serve();
        }
    
        else if (args[1].equals("reactor"))
        {
            Server.reactor(
             Runtime.getRuntime().availableProcessors(),
             7777, //port
             () -> new StompMessageProtocol(), //protocol factory
             StompMessageEncoderDecoder::new, //message encoder decoder factory
             connections
             ).serve();
        }
    
    }
}
