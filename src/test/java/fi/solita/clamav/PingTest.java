package fi.solita.clamav;

import org.junit.Test;

import java.io.IOException;
import java.net.UnknownHostException;

import static org.junit.Assert.assertTrue;

/**
 * These tests assume clamd is running and responding in the virtual machine.
 */
public class PingTest {

  @Test
  public void testPingPong() throws UnknownHostException, IOException {
    ClamAVClient cl = new ClamAVClient("localhost", 3310);
    assertTrue(cl.ping());
  }
}
