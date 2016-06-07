package bowser.websocket;

import static com.google.common.base.Preconditions.checkState;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import ox.Json;
import ox.Log;

public class ClientSocket {

  private static final Executor executor = Executors.newCachedThreadPool();

  private final Socket socket;

  private Consumer<String> onMessage = s -> {
  };

  private final Consumer<ClientSocket> onOpen;

  private final Random rand = new Random();

  private OutputStream os;

  public ClientSocket(Socket socket, Consumer<ClientSocket> onOpen) {
    this.socket = socket;
    this.onOpen = onOpen;

    listen();
  }

  public ClientSocket send(Json json) {
    try {
      byte[] payload = json.asByteArray();
      os.write(createFrame(Opcode.TEXT, payload));
      os.flush();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
    return this;
  }

  public ClientSocket onMessage(Consumer<String> onMessage) {
    this.onMessage = onMessage;
    return this;
  }

  private void listen() {
    executor.execute(() -> {
      try {
        BufferedInputStream in = new BufferedInputStream(socket.getInputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        baos.write(in.read());
        byte[] buf = new byte[in.available()];
        in.read(buf);
        baos.write(buf);

        String s = baos.toString("UTF-8");
        handleUpgradeRequest(s);
        listenForMessages(in);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private void listenForMessages(InputStream in) throws Exception {
    while (true) {
      byte b1 = (byte) in.read();

      // boolean finished = b1 >> 8 != 0;
      byte rsv = (byte) ((b1 & ~(byte) 128) >> 4);
      checkState(rsv == 0, "Bad rsv: " + rsv);
      Opcode code = Opcode.get((byte) (b1 & 15));

      if (code == Opcode.TEXT) {
        byte[] data = getPayload(in);
        String text = new String(data, Charsets.UTF_8);
        onMessage.accept(text);
      } else if (code == Opcode.PING) {
        getPayload(in);
        sendPong();
      } else {
        Log.debug("Received opcode: " + code);
      }
    }
  }

  private byte[] getPayload(InputStream in) throws Exception {
    byte b2 = (byte) in.read();
    boolean mask = (b2 & -128) != 0;
    int payloadlength = (byte) (b2 & ~(byte) 128);

    if (payloadlength < 0 || payloadlength > 125) {
      if (payloadlength == 126) {
        byte[] sizebytes = new byte[3];
        sizebytes[1] = (byte) in.read();
        sizebytes[2] = (byte) in.read();
        payloadlength = new BigInteger(sizebytes).intValue();
      } else {
        byte[] bytes = new byte[8];
        in.read(bytes);
        long length = new BigInteger(bytes).longValue();
        if (length > Integer.MAX_VALUE) {
          throw new RuntimeException("Payloadsize is to big...");
        } else {
          payloadlength = (int) length;
        }
      }
    }

    return readPayload(in, mask, payloadlength);
  }

  private byte[] readPayload(InputStream in, boolean mask, int payloadLength) throws Exception {
    byte[] maskKey = null;

    if (mask) {
      maskKey = new byte[4];
      in.read(maskKey);
    }

    byte[] data = new byte[payloadLength];
    checkState(in.read(data) == data.length, "Didn't read all the bytes.");

    if (mask) {
      for (int i = 0; i < payloadLength; i++) {
        data[i] = ((byte) (data[i] ^ maskKey[i % 4]));
      }
    }

    return data;
  }

  private void sendPong() {
    byte[] data = createFrame(Opcode.PONG, new byte[0]);
    try {
      os.write(data);
      os.flush();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private byte[] createFrame(Opcode code, byte[] payload) {
    boolean mask = false;
    int sizebytes = payload.length <= 125 ? 1 : payload.length <= 65535 ? 2 : 8;
    ByteBuffer buf = ByteBuffer
        .allocate(1 + (sizebytes > 1 ? sizebytes + 1 : sizebytes) + (mask ? 4 : 0) + payload.length);
    byte one = (byte) -128;
    one |= Opcode.TEXT.code;
    buf.put(one);
    byte[] payloadlengthbytes = toByteArray(payload.length, sizebytes);
    assert (payloadlengthbytes.length == sizebytes);

    if (sizebytes == 1) {
      buf.put((byte) (payloadlengthbytes[0] | (mask ? (byte) -128 : 0)));
    } else if (sizebytes == 2) {
      buf.put((byte) ((byte) 126 | (mask ? (byte) -128 : 0)));
      buf.put(payloadlengthbytes);
    } else if (sizebytes == 8) {
      buf.put((byte) ((byte) 127 | (mask ? (byte) -128 : 0)));
      buf.put(payloadlengthbytes);
    } else {
      throw new RuntimeException("Size representation not supported/specified");
    }

    if (mask) {
      ByteBuffer maskkey = ByteBuffer.allocate(4);
      maskkey.putInt(rand.nextInt());
      buf.put(maskkey.array());
      for (int i = 0; i < payload.length; i++) {
        buf.put((byte) (payload[i] ^ maskkey.get(i % 4)));
      }
    } else {
      buf.put(payload);
    }

    buf.flip();

    return buf.array();
  }

  private byte[] toByteArray(long val, int bytecount) {
    byte[] buffer = new byte[bytecount];
    int highest = 8 * bytecount - 8;
    for (int i = 0; i < bytecount; i++) {
      buffer[i] = (byte) (val >>> (highest - 8 * i));
    }
    return buffer;
  }

  private void handleUpgradeRequest(String s) {
    List<String> lines = Splitter.on("\r\n").omitEmptyStrings().splitToList(s);
    List<String> firstLine = Splitter.on(" ").splitToList(lines.get(0));
    checkState(firstLine.get(0).equals("GET"), "Upgrade request must be a GET. Instead it was: " + firstLine);

    Map<String, String> headers = Maps.newHashMap();
    for (int i = 1; i < lines.size(); i++) {
      String line = lines.get(i);
      int j = line.indexOf(':');
      String key = line.substring(0, j);
      String value = line.substring(j + 2);
      headers.put(key, value);
    }

    String websocketKey = headers.get("Sec-WebSocket-Key");

    HashCode hash = Hashing.sha1().hashString(websocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11", Charsets.UTF_8);

    String acceptCode = BaseEncoding.base64().encode(hash.asBytes());

    String response = "HTTP/1.1 101 Switching Protocols\r\n"
        + "Connection: Upgrade\r\n"
        + "Upgrade: websocket\r\n"
        + "Sec-WebSocket-Accept: " + acceptCode
        + "\r\n\r\n";

    try {
      os = new BufferedOutputStream(socket.getOutputStream());
      os.write(response.getBytes(Charsets.UTF_8));
      os.flush();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    onOpen.accept(this);
  }

}
