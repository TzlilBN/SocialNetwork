package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.function.Supplier;
import bgu.spl.net.api.*;
import bgu.spl.net.impl.stomp.StompConnections;
import bgu.spl.net.impl.stomp.StompMessageProtocol;

public abstract class BaseServer implements Server<String> {

    private final int port;
    private final Supplier<StompMessageProtocol> protocolFactory;
    private final Supplier<MessageEncoderDecoder<String>> encdecFactory;
    private ServerSocket sock;
    private StompConnections connections;

    public BaseServer(
            int port,
            Supplier<StompMessageProtocol> protocolFactory,
            Supplier<MessageEncoderDecoder<String>> encdecFactory,
            StompConnections connections) {

        this.port = port;
        this.protocolFactory = protocolFactory;
        this.encdecFactory = encdecFactory;
		this.sock = null;
        this.connections = connections;
    }
    

    @Override
    public void serve() {

        try (ServerSocket serverSock = new ServerSocket(port)) {
			System.out.println("Server started");

            this.sock = serverSock; //just to be able to close

            while (!Thread.currentThread().isInterrupted()) {

                Socket clientSock = serverSock.accept();

                BlockingConnectionHandler handler = new BlockingConnectionHandler(
                        clientSock,
                        encdecFactory.get(),
                        protocolFactory.get(),
                        connections,
                        connections.generateNewId());

                execute(handler);
            }
        } catch (IOException ex) {
        }

        System.out.println("server closed!!!");
    }

    @Override
    public void close() throws IOException {
		if (sock != null)
			sock.close();
    }

    protected abstract void execute(BlockingConnectionHandler  handler);

}
