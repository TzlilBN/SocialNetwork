package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api. StompMessagingProtocol;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import bgu.spl.net.impl.stomp.StompConnections;
import bgu.spl.net.impl.stomp.StompMessageProtocol;

public class BlockingConnectionHandler implements Runnable, ConnectionHandler<String> {

    private final StompMessageProtocol protocol;
    private final MessageEncoderDecoder<String> encdec;
    private final Socket sock;
    private BufferedInputStream in;
    private BufferedOutputStream out;
    private volatile boolean connected = true;
    private StompConnections connections;
    protected AtomicInteger userId;

    public BlockingConnectionHandler(Socket sock, MessageEncoderDecoder<String> reader, StompMessageProtocol protocol,StompConnections connections, int userId) {
        this.sock = sock;
        this.encdec = reader;
        this.protocol = protocol;
        this.connections = connections;
        this.userId = new AtomicInteger(userId);
        connections.getIdToConnectionHandler().put(userId, this);
    }

    @Override
    public void run() {
        try (Socket sock = this.sock) { //just for automatic closing
            int read;
            protocol.start(userId.get(), connections);
            in = new BufferedInputStream(sock.getInputStream());
            out = new BufferedOutputStream(sock.getOutputStream());

            while (!protocol.shouldTerminate() && connected && (read = in.read()) >= 0) {
                String nextMessage = encdec.decodeNextByte((byte) read);
                if (nextMessage != null)
                {
                    String[] response = protocol.process(nextMessage);
                    for (int i = 0; i < 4; i++)
                    {
                        if (response[i] == null)
                            continue;
                        if (i == 1)
                        {
                            String mySUb = Integer.toString(connections.getUserById(userId.get()).getTopicToSub().get(protocol.generateTopic(nextMessage)));
                            mySUb = "subscription:" + mySUb +"\n" + nextMessage.substring(5);
                            connections.send(protocol.generateTopic(nextMessage), mySUb);
                        }
                        else
                        {
                            connections.send(userId.get(), response[i]);
                            if (i==3)
                            {
                                String username = connections.getUserById(userId.get()).getUsername();
                                connections.disconnect(username);
                                close();
                            }
                        }
                    }
                }
            }
            }catch (IOException ex) {
                ex.printStackTrace();
        }

    }

    @Override
    public void close() throws IOException {
        connected = false;
        sock.close();
    }

    @Override
    public void send(String msg)
    {
        try {
            out.write(encdec.encode(msg));
            out.flush();
        }catch (IOException ex) {
            ex.printStackTrace();
        }
        
    }

    public void sendMessege(String seifa, int MId) {
        String output = "MESSAGE\n" + "message-id:" +String.valueOf(MId)+"\n"
        +"id:" + connections.getUserById(userId.get()).getTopicToSub().get(protocol.generateTopic(seifa))+"\n"
        +seifa;
        try {
            out.write(encdec.encode(output));
            out.flush();
        }catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
