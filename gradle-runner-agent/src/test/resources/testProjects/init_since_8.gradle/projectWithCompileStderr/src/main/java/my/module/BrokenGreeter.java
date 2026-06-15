package my.module;

public class BrokenGreeter {
  public String greet() {
    return missingMethod();
  }
}
