package jetbrains.buildServer.gradle.filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class GradleTestFilter {
  private final List<GradleTestPattern> includes;
  private final List<GradleTestPattern> excludes;
  private final List<GradleTestPattern> cmdIncludes;

  public GradleTestFilter(final Collection<String> includes, final Collection<String> excludes, final Collection<String> cmdIncludes) {
    this.includes = compile(includes);
    this.excludes = compile(excludes);
    this.cmdIncludes = compile(cmdIncludes);
  }

  public boolean contains(final String className) {
    return (includes.isEmpty() || test(includes, className)) &&
           (cmdIncludes.isEmpty() || test(cmdIncludes, className)) &&
           (excludes.isEmpty() || !test(excludes, className));
  }

  private boolean test(final List<GradleTestPattern> patterns, final String className) {
    for (final GradleTestPattern pattern : patterns) {
      if (pattern.test(className)) return true;
    }
    return false;
  }

  private static List<GradleTestPattern> compile(final Collection<String> expressions) {
    final List<GradleTestPattern> result = new ArrayList<GradleTestPattern>();
    for (final String expression : expressions) {
      result.add(new GradleTestPattern(expression));
    }
    return Collections.unmodifiableList(result);
  }

  private static class GradleTestPattern {
    private final Pattern pattern;

    public GradleTestPattern(final String expression) {
      pattern = compile(expression);
    }

    public boolean test(final String str) {
      return pattern.matcher(str).matches();
    }

    private static Pattern compile(final String expression) {
      if (expression == null || expression.length() == 0) return Pattern.compile("");
      final StringBuilder pattern = new StringBuilder();
      final StringBuilder substring = new StringBuilder();
      int cursor = 0;
      while (cursor < expression.length()) {
        final char ch = expression.charAt(cursor);
        if (ch == '*') {
          pattern.append(".*");
          pattern.append(Pattern.quote(substring.toString()));
          substring.setLength(0);
        } else {
          substring.append(ch);
        }
        cursor++;
      }
      pattern.append(Pattern.quote(substring.toString()));
      return Pattern.compile(pattern.toString());
    }
  }
}
