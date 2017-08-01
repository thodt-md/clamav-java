package fi.solita.clamav;

import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Test container with clamd. Uses <code>lokori/clamav-java</code> image.
 */
public class ClamdContainer extends GenericContainer<ClamdContainer> {

  private static final String IMAGE_NAME = "lokori/clamav-java";

  // hardcoded clamd TCP port in lokori/clamav-java.
  private static final int CLAMD_PORT = 3310;

  private static final int CONNECT_TIMEOUT = 500;
  private static final int READ_TIMEOUT = 500;

  private static final byte[] PING = "zPING\0".getBytes(StandardCharsets.US_ASCII);
  private static final byte[] PONG = "PONG".getBytes(StandardCharsets.US_ASCII);

  private static final RateLimiter CLAMD_CONNECT_RATE_LIMIT = RateLimiterBuilder.newBuilder()
      .withRate(10, TimeUnit.SECONDS)
      .withConstantThroughput()
      .build();

  /**
   * Constructs the test container using a specific tag.
   *
   * @param tag lokori/clamav-java tag.
   */
  public ClamdContainer(String tag) {
    super(IMAGE_NAME + ":" + tag);
  }

  /**
   * Constructs the test container using the latest tag.
   */
  public ClamdContainer() {
    this("latest");
  }

  @Override
  protected void configure() {
    addExposedPort(CLAMD_PORT);
  }

  @Override
  protected void waitUntilContainerStarted() {
    // we must check, that clamd is properly responding before making the container available
    logger().info("Waiting for clamd to initialize on {}:{}", getClamdHost(), getClamdPort());
    Unreliables.retryUntilTrue(120, TimeUnit.SECONDS, () -> {
      if (!isRunning()) {
        throw new ContainerLaunchException("Container failed to start");
      }

      boolean ready = CLAMD_CONNECT_RATE_LIMIT.getWhenReady(this::ping);
      if (ready) {
        logger().info("Initialization complete");
      }

      return ready;
    });
  }

  // some communications logic copied from ClamAVClient

  private boolean ping() throws IOException {
    try (Socket socket = openSocket();
         OutputStream out = socket.getOutputStream();
    ) {
      out.write(PING);
      out.flush();
      byte[] b = new byte[PONG.length];
      InputStream inputStream = socket.getInputStream();
      int copyIndex = 0;
      int readResult;
      do {
        readResult = inputStream.read(b, copyIndex, Math.max(b.length - copyIndex, 0));
        copyIndex += readResult;
      } while (readResult > 0);

      return Arrays.equals(b, PONG);
    }
  }

  private Socket openSocket() throws IOException {
    Socket socket = new Socket();
    socket.connect(new InetSocketAddress(getClamdHost(), getClamdPort()), CONNECT_TIMEOUT);
    socket.setSoTimeout(READ_TIMEOUT);
    return socket;
  }

  @Override
  protected Integer getLivenessCheckPort() {
    return getClamdPort();
  }

  public int getClamdPort() {
    return getMappedPort(CLAMD_PORT);
  }

  public String getClamdHost() {
    return getContainerIpAddress();
  }

}
