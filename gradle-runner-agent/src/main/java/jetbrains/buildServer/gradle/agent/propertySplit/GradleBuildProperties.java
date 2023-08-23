package jetbrains.buildServer.gradle.agent.propertySplit;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Enumeration;
import jetbrains.buildServer.util.SortedProperties;

/**
 * Properties which are used in the Gradle Tooling API's build.
 * The code was taken from {@link java.util.Properties} except for the timestamp and comments part.
 * This was made in order to support Gradle configuration-cache.
 * So if properties content (keys and values) didn't change, then Gradle will resuse the configuration-cache.
 * It wouldn't be possible with the timestamp header of {@link java.util.Properties}.
 */
public class GradleBuildProperties extends SortedProperties {

  @Override
  public void store(OutputStream out, String comments) throws IOException {
    storeInternal(out);
  }

  private void storeInternal(OutputStream out) throws IOException {
    BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "8859_1"));
    boolean escUnicode = true;

    synchronized (this) {
      for (Enumeration<?> e = keys(); e.hasMoreElements();) {
        String key = (String)e.nextElement();
        String val = (String)get(key);
        key = saveConvert(key, true, escUnicode);
        /* No need to escape embedded and trailing spaces for value, hence
         * pass false to flag.
         */
        val = saveConvert(val, false, escUnicode);
        bw.write(key + "=" + val);
        bw.newLine();
      }
    }
    bw.flush();
  }

  /*
   * Converts unicodes to encoded &#92;uxxxx and escapes
   * special characters with a preceding slash
   */
  private String saveConvert(String theString,
                             boolean escapeSpace,
                             boolean escapeUnicode) {
    int len = theString.length();
    int bufLen = len * 2;
    if (bufLen < 0) {
      bufLen = Integer.MAX_VALUE;
    }
    StringBuffer outBuffer = new StringBuffer(bufLen);

    for(int x=0; x<len; x++) {
      char aChar = theString.charAt(x);
      // Handle common case first, selecting largest block that
      // avoids the specials below
      if ((aChar > 61) && (aChar < 127)) {
        if (aChar == '\\') {
          outBuffer.append('\\'); outBuffer.append('\\');
          continue;
        }
        outBuffer.append(aChar);
        continue;
      }
      switch(aChar) {
        case ' ':
          if (x == 0 || escapeSpace)
            outBuffer.append('\\');
          outBuffer.append(' ');
          break;
        case '\t':outBuffer.append('\\'); outBuffer.append('t');
          break;
        case '\n':outBuffer.append('\\'); outBuffer.append('n');
          break;
        case '\r':outBuffer.append('\\'); outBuffer.append('r');
          break;
        case '\f':outBuffer.append('\\'); outBuffer.append('f');
          break;
        case '=': // Fall through
        case ':': // Fall through
        case '#': // Fall through
        case '!':
          outBuffer.append('\\'); outBuffer.append(aChar);
          break;
        default:
          if (((aChar < 0x0020) || (aChar > 0x007e)) & escapeUnicode ) {
            outBuffer.append('\\');
            outBuffer.append('u');
            outBuffer.append(toHex((aChar >> 12) & 0xF));
            outBuffer.append(toHex((aChar >>  8) & 0xF));
            outBuffer.append(toHex((aChar >>  4) & 0xF));
            outBuffer.append(toHex( aChar        & 0xF));
          } else {
            outBuffer.append(aChar);
          }
      }
    }
    return outBuffer.toString();
  }

  /**
   * Convert a nibble to a hex character
   * @param   nibble  the nibble to convert.
   */
  private static char toHex(int nibble) {
    return hexDigit[(nibble & 0xF)];
  }

  /** A table of hex digits */
  private static final char[] hexDigit = {
    '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'
  };
}
