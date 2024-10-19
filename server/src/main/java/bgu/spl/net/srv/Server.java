package bgu.spl.net.srv;

import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.MessagingProtocol;
import java.io.Closeable;
import java.util.function.Supplier;
import bgu.spl.net.api.*;
import bgu.spl.net.impl.stomp.StompConnections;
import bgu.spl.net.impl.stomp.StompMessageProtocol;

public interface Server<T> extends Closeable {

    /**
     * The main loop of the server, Starts listening and handling new clients.
     */
    void serve();

    /**
     *This function returns a new instance of a thread per client pattern server
     * @param port The port for the server socket
     * @param protocolFactory A factory that creats new MessagingProtocols
     * @param encoderDecoderFactory A factory that creats new MessageEncoderDecoder
     * @param <T> The Message Object for the protocol
     * @return A new Thread per client server
     */
    public static BaseServer  threadPerClient(
            int port,
            Supplier<StompMessageProtocol> protocolFactory,
            Supplier<MessageEncoderDecoder<String>> encoderDecoderFactory,
            StompConnections connections) {

        return new BaseServer(port, protocolFactory, encoderDecoderFactory, connections) {
            @Override
            protected void execute(BlockingConnectionHandler  handler) {
                new Thread(handler).start();
            }
        };

    }

    /**
     * This function returns a new instance of a reactor pattern server
     * @param nthreads Number of threads available for protocol processing
     * @param port The port for the server socket
     * @param protocolFactory A factory that creats new MessagingProtocols
     * @param encoderDecoderFactory A factory that creats new MessageEncoderDecoder
     * @param <T> The Message Object for the protocol
     * @return A new reactor server
     */
    public static Server<String> reactor(
            int nthreads,
            int port,
            Supplier<StompMessageProtocol> protocolFactory,
            Supplier<MessageEncoderDecoder<String>> encoderDecoderFactory,
            StompConnections connections) {
        return new Reactor(nthreads, port, protocolFactory, encoderDecoderFactory,connections);
    }

}
