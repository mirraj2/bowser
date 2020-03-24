package bowser.misc;

import static com.google.common.base.Preconditions.checkState;
import static ox.util.Utils.propagate;

import java.io.File;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.google.common.base.Splitter;

import ox.IO;
import ox.Log;

public class SSLUtils {

  public static SSLContext createContext(String domain) {
    String pass = "spamspam";

    File dir = new File("/etc/letsencrypt/live/" + domain);
    if (!dir.exists()) {
      Log.warn("Could not find letsencrypt dir: " + dir);
      return null;
    }

    File keystoreFile = new File(dir, "keystore.jks");
    File pemFile = new File(dir, "fullchain.pem");

    boolean generateKeystore = false;

    if (keystoreFile.exists()) {
      if (keystoreFile.lastModified() < pemFile.lastModified()) {
        Log.info("SSUtils: It looks like a new PEM file was created. Regenerating the keystore.");
        keystoreFile.delete();
        generateKeystore = true;
      }
    } else {
      generateKeystore = true;
    }

    if (generateKeystore) {
      Splitter splitter = Splitter.on(' ');
      try {
        String command = "openssl pkcs12 -export -out keystore.pkcs12 -in fullchain.pem -inkey privkey.pem -passout pass:"
            + pass;
        Log.debug(command);
        Process process = new ProcessBuilder(splitter.splitToList(command))
            .directory(dir).inheritIO().start();
        checkState(process.waitFor() == 0);

        command = "keytool -importkeystore -srckeystore keystore.pkcs12 -srcstoretype PKCS12 -destkeystore keystore.jks -srcstorepass "
            + pass + " -deststorepass " + pass;
        Log.debug(command);
        process = new ProcessBuilder(splitter.splitToList(command))
            .directory(dir).inheritIO().start();
        checkState(process.waitFor() == 0);

        new File(dir, "keystore.pkcs12").delete();// cleanup
      } catch (Exception e) {
        throw propagate(e);
      }
    }

    try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(IO.from(keystoreFile).asStream(), pass.toCharArray());

      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keystore, pass.toCharArray());

      SSLContext ret = SSLContext.getInstance("TLSv1.2");
      TrustManagerFactory factory = TrustManagerFactory.getInstance(
          TrustManagerFactory.getDefaultAlgorithm());
      factory.init(keystore);
      ret.init(keyManagerFactory.getKeyManagers(), factory.getTrustManagers(), null);

      return ret;
    } catch (Exception e) {
      throw propagate(e);
    }
  }

}
