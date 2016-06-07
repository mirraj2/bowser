package bowser.websocket;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocketFactory;

public class WebSocketServer {

  public final int port;
  private SSLContext context;
  private ServerSocket server;
  private Consumer<ClientSocket> onOpen = socket -> {
  };

  public WebSocketServer(int port) {
    this.port = port;
  }

  public WebSocketServer ssl(SSLContext context) {
    this.context = context;
    return this;
  }

  public WebSocketServer onOpen(Consumer<ClientSocket> onOpen) {
    this.onOpen = onOpen;
    return this;
  }

  public WebSocketServer start() {
    try {
      if (context != null) {
        SSLServerSocketFactory socketFactory = context.getServerSocketFactory();
        server = socketFactory.createServerSocket(port);
      } else {
        server = new ServerSocket(port);
      }
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
