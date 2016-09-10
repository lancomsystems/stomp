package de.lancom.systems.stomp.test;

import java.net.ServerSocket;

import lombok.Getter;
import org.apache.activemq.broker.BrokerService;

/**
 * Embedded active mq broker.
 */
public class Broker {

    @Getter
    private BrokerService brokerService;

    @Getter
    private int port = 0;

    /**
     * Create broker with random port.
     */
    public Broker() {
        this(0);
    }

    /**
     * Create broker with the given port.
     *
     * @param port port
     */
    public Broker(final int port) {
        this.port = port;
    }

    /**
     * Start broker.
     *
     * @throws Exception if message broker fails to start.
     */
    public void start() throws Exception {

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
    }

    /**
     * Stop broker.
     *
     * @throws Exception if message broker fails to stop.
     */
    public void stop() throws Exception {
        this.brokerService.stop();
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
