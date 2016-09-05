package de.lancom.systems.stomp.util;

import java.net.ServerSocket;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import lombok.Getter;
import org.apache.activemq.broker.BrokerService;

/**
 * Created by fkneier on 19.08.16.
 */
public class Broker {

    @Getter
    private int brokerPort;

    @Getter
    private BrokerService brokerService;

    @PostConstruct
    public void startBroker() throws Exception {
        final ServerSocket serverSocket = new ServerSocket(0);
        this.brokerPort = serverSocket.getLocalPort();
        serverSocket.close();

        this.brokerService = new BrokerService();
        this.brokerService.addConnector("stomp+nio://localhost:" + brokerPort);
        this.brokerService.setPersistent(false);
        this.brokerService.setAllowTempAutoCreationOnSend(true);
        this.brokerService.start();
    }

    @PreDestroy
    public void stopBroker() throws Exception {
        this.brokerService.stop();
    }

    public void addQueue(final String name) {
        try {
            this.brokerService.getAdminView().addQueue(name);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public void addTopic(final String name) {
        try {
            this.brokerService.getAdminView().addTopic(name);
        } catch (final Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
