package pp.winf2slay.controller.network;

import org.junit.jupiter.api.Test;
import pp.winf2slay.controller.message.client.CMJoinLobby;
import pp.winf2slay.controller.message.server.SMWelcome;
import pp.winf2slay.controller.message.server.ServerMessage;
import pp.winf2slay.model.Game;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Echte Netzwerkverbindungen über localhost: Mehrere Server nacheinander im selben
 * Programm (wie bei „Nochmal spielen“), belegte Ports und abgebrochene Verbindungen.
 */
class ServerRestartTest {

    private static int freePort() throws IOException {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        }
    }

    /**
     * Verbindet sich, meldet sich an und wartet auf die Begrüßung des Servers.
     */
    private static String joinOnce(int port, String name) throws Exception {
        CountDownLatch welcome = new CountDownLatch(1);
        AtomicReference<String> confirmed = new AtomicReference<>();
        AtomicReference<ClientNetworkSupport> ref = new AtomicReference<>();
        ClientNetworkSupport client = new ClientNetworkSupport(new ClientNetworkListener() {
            @Override
            public void onConnected() {
                ref.get().send(new CMJoinLobby(name));
            }

            @Override
            public void onMessage(ServerMessage message) {
                if (message instanceof SMWelcome w) {
                    confirmed.set(w.getPlayerName());
                    welcome.countDown();
                }
            }

            @Override
            public void onDisconnected(String reason) {
                welcome.countDown();
            }
        });
        ref.set(client);
        client.connect("localhost", port);
        assertTrue(welcome.await(10, TimeUnit.SECONDS), "keine Antwort vom Server");
        client.disconnect();
        return confirmed.get();
    }

    @Test
    void severalServersOneAfterAnother() throws Exception {
        for (int round = 1; round <= 3; round++) {
            int port = freePort();
            WinfToSlayServer server = WinfToSlayServer.start(port);
            try {
                assertEquals("Runde" + round, joinOnce(port, "Runde" + round));
            }
            finally {
                server.stop();
            }
        }
    }

    @Test
    void busyPortIsReportedAsIOException() throws Exception {
        try (ServerSocket blocker = new ServerSocket(0)) {
            int port = blocker.getLocalPort();
            assertThrows(IOException.class, () -> WinfToSlayServer.start(port));
        }
        // danach funktioniert ein neuer Server weiterhin
        int port = freePort();
        WinfToSlayServer server = WinfToSlayServer.start(port);
        try {
            assertEquals("Danach", joinOnce(port, "Danach"));
        }
        finally {
            server.stop();
        }
    }

    @Test
    void clientsLearnWhenTheServerStops() throws Exception {
        int port = freePort();
        WinfToSlayServer server = WinfToSlayServer.start(port);
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch disconnected = new CountDownLatch(1);
        AtomicReference<String> reason = new AtomicReference<>();
        ClientNetworkSupport client = new ClientNetworkSupport(new ClientNetworkListener() {
            @Override
            public void onConnected() {
                connected.countDown();
            }

            @Override
            public void onMessage(ServerMessage message) {
                // nicht relevant
            }

            @Override
            public void onDisconnected(String r) {
                reason.set(r);
                disconnected.countDown();
            }
        });
        try {
            client.connect("localhost", port);
            assertTrue(connected.await(10, TimeUnit.SECONDS), "keine Verbindung");
            server.stop();
            assertTrue(disconnected.await(10, TimeUnit.SECONDS), "Client hat das Ende des Servers nicht bemerkt");
            assertEquals("Der Gastgeber hat das Spiel beendet.", reason.get());
        }
        finally {
            client.disconnect();
            server.stop();
        }
    }

    @Test
    void stopTwiceIsHarmless() throws Exception {
        WinfToSlayServer server = WinfToSlayServer.start(freePort());
        server.stop();
        server.stop();
    }

    @Test
    void disconnectWhileConnectingLeavesNoGhost() throws Exception {
        int port = freePort();
        WinfToSlayServer server = WinfToSlayServer.start(port);
        try {
            ClientNetworkSupport client = new ClientNetworkSupport(new ClientNetworkListener() {
                @Override
                public void onConnected() {
                    // nicht relevant
                }

                @Override
                public void onMessage(ServerMessage message) {
                    // nicht relevant
                }

                @Override
                public void onDisconnected(String reason) {
                    // nicht relevant
                }
            });
            client.disconnect(); // Abbruch, bevor der Verbindungsaufbau beginnt
            assertThrows(IOException.class, () -> client.connect("localhost", port));
            // ein regulärer Spieler kann sich weiterhin anmelden und bekommt den Wunschnamen
            assertEquals("Echt", joinOnce(port, "Echt"));
        }
        finally {
            server.stop();
        }
    }

    @Test
    void fullLobbyRejectsWithReasonAndWithoutWelcome() throws Exception {
        int port = freePort();
        WinfToSlayServer server = WinfToSlayServer.start(port);
        List<ClientNetworkSupport> seated = new ArrayList<>();
        try {
            for (int i = 1; i <= Game.MAX_PLAYERS; i++) seated.add(joinAndStay(port, "Platz" + i));
            CountDownLatch disconnected = new CountDownLatch(1);
            AtomicBoolean welcomed = new AtomicBoolean();
            AtomicReference<String> reason = new AtomicReference<>();
            AtomicReference<ClientNetworkSupport> ref = new AtomicReference<>();
            ClientNetworkSupport late = new ClientNetworkSupport(new ClientNetworkListener() {
                @Override
                public void onConnected() {
                    ref.get().send(new CMJoinLobby("Zuspaet"));
                }

                @Override
                public void onMessage(ServerMessage message) {
                    if (message instanceof SMWelcome) welcomed.set(true);
                }

                @Override
                public void onDisconnected(String r) {
                    reason.set(r);
                    disconnected.countDown();
                }
            });
            ref.set(late);
            late.connect("localhost", port);
            assertTrue(disconnected.await(10, TimeUnit.SECONDS), "Abweisung kam nicht an");
            assertEquals(WinfToSlayServer.REJECT_REASON, reason.get());
            assertFalse(welcomed.get(), "abgewiesener Client wurde begrüßt");
        }
        finally {
            seated.forEach(ClientNetworkSupport::disconnect);
            server.stop();
        }
    }

    /**
     * Tritt der Lobby bei und bleibt verbunden.
     */
    private static ClientNetworkSupport joinAndStay(int port, String name) throws Exception {
        CountDownLatch welcome = new CountDownLatch(1);
        AtomicReference<ClientNetworkSupport> ref = new AtomicReference<>();
        ClientNetworkSupport client = new ClientNetworkSupport(new ClientNetworkListener() {
            @Override
            public void onConnected() {
                ref.get().send(new CMJoinLobby(name));
            }

            @Override
            public void onMessage(ServerMessage message) {
                if (message instanceof SMWelcome) welcome.countDown();
            }

            @Override
            public void onDisconnected(String reason) {
                // nicht relevant
            }
        });
        ref.set(client);
        client.connect("localhost", port);
        assertTrue(welcome.await(10, TimeUnit.SECONDS), name + " wurde nicht begrüßt");
        return client;
    }
}
