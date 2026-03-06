package jetbrains.buildServer.gradle.runtime.output;

import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.messages.serviceMessages.ServiceMessage.*;

/**
 * Parser for output from tests.
 * Allows to split long outputs into smaller parts according to the desired max length.
 */
public class TestOutputParser {

  public static final char ESCAPE_CHARACTER = '|';

  @NotNull
  public static String getMessageWithoutParsing(@NotNull String message, int maxLength) {
    if (message.length() <= maxLength) return message;
    return message.substring(0, maxLength);
  }

  @NotNull
  public static String getMessageWithParsing(@NotNull String message, int maxLength) {
    if (message.length() <= maxLength) return message;

    // Find inner service messages and try not to split each message
    int start = message.indexOf(SERVICE_MESSAGE_START);
    if (start == -1 || start > maxLength) return message.substring(0, maxLength);

    int end = getEndMessage(message, start);
    if (end == -1) return message.substring(0, maxLength);
    if (end > maxLength) return message.substring(0, start > 0 ? start : maxLength);

    while (end < maxLength) {
      int newStart = message.indexOf(SERVICE_MESSAGE_START, end);
      if (newStart == -1 || newStart >= maxLength) return message.substring(0, maxLength);

      int newEnd = getEndMessage(message, newStart);
      if (newEnd == -1) return message.substring(0, maxLength);

      if (newEnd >= maxLength) return message.substring(0, newStart);

      end = newEnd;
    }

    return message.substring(0, end);
  }

  private static int getEndMessage(@NotNull String msg, int position) {
    char lastChar = 0;
    while (position < msg.length()) {
      char currentChar = msg.charAt(position);
      if (lastChar != ESCAPE_CHARACTER && currentChar == SERVICE_MESSAGE_END.charAt(SERVICE_MESSAGE_END.length() - 1)) {
        return position;
      }
      if (lastChar == ESCAPE_CHARACTER && currentChar == ESCAPE_CHARACTER) {
        lastChar = 0;
      }
      else {
        lastChar = currentChar;
      }

      position++;
    }

    return -1;
  }
}
