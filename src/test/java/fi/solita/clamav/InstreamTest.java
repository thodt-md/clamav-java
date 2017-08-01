package fi.solita.clamav;

import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * These tests assume clamd is running and responding in the test container.
 */
public class InstreamTest {

  @ClassRule
  public static ClamdContainer container = new ClamdContainer();

  private byte[] scan(byte[] input) throws IOException {
    ClamAVClient cl = new ClamAVClient(container.getClamdHost(), container.getClamdPort());
    return cl.scan(input);
  }

  private byte[] scan(InputStream input) throws IOException {
    ClamAVClient cl = new ClamAVClient(container.getClamdHost(), container.getClamdPort());
    return cl.scan(input);
  }

  @Test
  public void testRandomBytes() throws IOException {
    byte[] r = scan("alsdklaksdla".getBytes("ASCII"));
    assertTrue(ClamAVClient.isCleanReply(r));
  }

  @Test
  public void testPositive() throws IOException {
    // http://www.eicar.org/86-0-Intended-use.html
    byte[] EICAR = "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*".getBytes("ASCII");
    byte[] r = scan(EICAR);
    assertFalse(ClamAVClient.isCleanReply(r));
  }

  @Test
  public void testStreamChunkingWorks() throws IOException {
    byte[] multipleChunks = new byte[50000];
    byte[] r = scan(multipleChunks);
    assertTrue(ClamAVClient.isCleanReply(r));
  }

  @Test
  public void testChunkLimit() throws IOException {
    byte[] maximumChunk = new byte[2048];
    byte[] r = scan(maximumChunk);
    assertTrue(ClamAVClient.isCleanReply(r));
  }

  @Test
  public void testZeroBytes() throws IOException {
    byte[] r = scan(new byte[]{});
    assertTrue(ClamAVClient.isCleanReply(r));
  }

  @Test(expected = ClamAVSizeLimitException.class)
  public void testSizeLimit() throws IOException {
    scan(new SlowInputStream());
  }
}
