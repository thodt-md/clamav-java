package fi.solita.clamav;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * These tests assume clamd is running and responding in the virtual machine.
 */
public class PingTest {

  @ClassRule
  public static ClamdContainer container = new ClamdContainer();

  @Test
  public void testPingPong() throws IOException {
    ClamAVClient cl = new ClamAVClient(container.getClamdHost(), container.getClamdPort());
    assertTrue(cl.ping());
  }

}
