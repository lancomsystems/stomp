package de.lancom.systems.stomp.test;

import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.Getter;
import org.apache.activemq.broker.BrokerService;
import org.springframework.beans.factory.annotation.Value;

/**
 * Embedded stomp broke using Active MQ.
 */
public class StompBroker {

    @Getter
    private BrokerService brokerService;

    @Getter
    @Value("${embedded.broker.port}")
    private int port = 0;

    private AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Create a new broker and start.
     *
     * @return broker
     */
    public static StompBroker run() {
        final StompBroker broker = new StompBroker();
        try {
            broker.start();
        } catch (final Exception ex) {
            throw new RuntimeException("Could not run embedded broker", ex);
        }
        return broker;
    }

    /**
     * Create broker with random port.
     */
    public StompBroker() {
        this(0);
    }

    /**
     * Create broker with the given port.
     *
     * @param port port
     */
    public StompBroker(final int port) {
        this.port = port;
    }

    /**
     * Start broker.
     *
     * @throws Exception if message broker fails to start.
     */
    public void start() throws Exception {
        if (this.running.compareAndSet(false, true)) {
            if (port == 0) {
                final ServerSocket serverSocket = new ServerSocket(port);
                this.port = serverSocket.getLocalPort();
                serverSocket.close();
            }

            this.brokerService = new BrokerService();
            this.brokerService.addConnector("stomp+nio://localhost:" + port);
            this.brokerService.setPersistent(false);
            this.brokerService.setAllowTempAutoCreationOnSend(true);
            this.brokerService.start();
            this.brokerService.waitUntilStarted();
        }
    }

    /**
     * Stop broker.
     *
     * @throws Exception if message broker fails to stop.
     */
    public void stop() throws Exception {
        if (this.running.compareAndSet(true, false)) {
            this.brokerService.stop();
            this.brokerService.waitUntilStopped();
        }
    }

    /**
     * Add the given queue to the broker.
     *
     * @param name queue name
     */
    public void addQueue(final String name) {
        try {
            this.brokerService.getAdminView().addQueue(name);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Add the given topic to the broker.
     *
     * @param name topic name
     */
    public void addTopic(final String name) {
        try {
            this.brokerService.getAdminView().addTopic(name);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
