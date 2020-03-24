package bowser.misc;

/**
 * This error message will be propagated to the user.
 */
public class UserReadableError extends RuntimeException {

  public UserReadableError(String msg) {
    super(msg);
  }

  public UserReadableError(String msg, Throwable cause) {
    super(msg, cause);
  }

}
