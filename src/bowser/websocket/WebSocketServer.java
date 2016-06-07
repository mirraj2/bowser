package bowser.websocket;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class WebSocketServer {

  public final int port;
  private ServerSocket server;
  private Consumer<ClientSocket> onOpen = socket -> {
  };

  public WebSocketServer(int port) {
    this.port = port;
  }

  public WebSocketServer onOpen(Consumer<ClientSocket> onOpen) {
    this.onOpen = onOpen;
    return this;
  }

  public WebSocketServer start() {
    try {
      server = new ServerSocket(port);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    Executors.newSingleThreadExecutor().execute(() -> {
      try {
        while (true) {
          Socket client = server.accept();
          new ClientSocket(client, onOpen);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    });

    return this;
  }

}
