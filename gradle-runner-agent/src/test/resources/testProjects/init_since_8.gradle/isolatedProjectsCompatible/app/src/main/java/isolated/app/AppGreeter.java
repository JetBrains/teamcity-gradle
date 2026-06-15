package isolated.app;

import isolated.lib.LibGreeter;

public class AppGreeter {
  public String greet() {
    return "app uses " + new LibGreeter().greet();
  }
}
