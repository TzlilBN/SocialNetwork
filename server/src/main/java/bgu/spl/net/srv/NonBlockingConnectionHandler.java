package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import bgu.spl.net.api.StompMessagingProtocol;

import bgu.spl.net.impl.stomp.StompConnections;
import bgu.spl.net.impl.stomp.StompMessageProtocol;

public class NonBlockingConnectionHandler implements ConnectionHandler<String> {

    private static final int BUFFER_ALLOCATION_SIZE = 1 << 13; //8k
    private static final ConcurrentLinkedQueue<ByteBuffer> BUFFER_POOL = new ConcurrentLinkedQueue<>();

    private final StompMessageProtocol protocol;
    private final MessageEncoderDecoder<String> encdec;
    private final Queue<ByteBuffer> writeQueue = new ConcurrentLinkedQueue<>();
    private final SocketChannel chan;
    private final Reactor reactor;
    private StompConnections connections;
    protected AtomicInteger userId;

    public NonBlockingConnectionHandler(
            MessageEncoderDecoder<String> reader,
            StompMessageProtocol protocol,
            SocketChannel chan,
            Reactor reactor,
            StompConnections connections,
            int userId) {
        this.chan = chan;
        this.encdec = reader;
        this.protocol = protocol;
        this.reactor = reactor;
        this.connections = connections;
        this.userId = new AtomicInteger(userId);
        this.protocol.start(userId, connections);
        connections.getIdToConnectionHandler().put(userId, this);
    }

    public Runnable continueRead() {
        ByteBuffer buf = leaseBuffer();

        boolean success = false;
        try {
            success = chan.read(buf) != -1;
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        if (success) {
            buf.flip();
            return () -> {
                try {
                    while (buf.hasRemaining()) {
                        String nextMessage = encdec.decodeNextByte(buf.get());
                        if (nextMessage != null) {
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
                } finally {
                    releaseBuffer(buf);
                }
            };
        } else {
            releaseBuffer(buf);
            close();
            return null;
        }

    }

    public void close() {
        try {
            chan.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public boolean isClosed() {
        return !chan.isOpen();
    }

    public void continueWrite() {
        while (!writeQueue.isEmpty()) {
            try {
                ByteBuffer top = writeQueue.peek();
                chan.write(top);
                if (top.hasRemaining()) {
                    return;
                } else {
                    writeQueue.remove();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                close();
            }
        }

        if (writeQueue.isEmpty()) {
            if (protocol.shouldTerminate()) close();
            else reactor.updateInterestedOps(chan, SelectionKey.OP_READ);
        }
    }

    private static ByteBuffer leaseBuffer() {
        ByteBuffer buff = BUFFER_POOL.poll();
        if (buff == null) {
            return ByteBuffer.allocateDirect(BUFFER_ALLOCATION_SIZE);
        }

        buff.clear();
        return buff;
    }

    private static void releaseBuffer(ByteBuffer buff) {
        BUFFER_POOL.add(buff);
    }

    @Override
    public void send(String msg) {
        writeQueue.add(ByteBuffer.wrap(encdec.encode(msg)));
        reactor.updateInterestedOps(chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    public void sendMessege(String seifa, int MId)
    {
        String output = "MESSAGE\n" + "message-id:" +String.valueOf(MId)+"\n"
        +"id:" + connections.getUserById(userId.get()).getTopicToSub().get(protocol.generateTopic(seifa))+"\n"
        +seifa;
        writeQueue.add(ByteBuffer.wrap(encdec.encode(output)));
        reactor.updateInterestedOps(chan, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }
}
