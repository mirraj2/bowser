package bowser.websocket;

public enum Opcode {

  CONTINUOUS(0), TEXT(1), BINARY(2), PING(8), PONG(9), CLOSING(10);

  private static final Opcode[] codes = new Opcode[11];
  static {
    for (Opcode code : Opcode.values()) {
      codes[code.code] = code;
    }
  }

  public final byte code;

  private Opcode(int code) {
    this.code = (byte) code;
  }

  public static Opcode get(int code) {
    return codes[code];
  }

}
