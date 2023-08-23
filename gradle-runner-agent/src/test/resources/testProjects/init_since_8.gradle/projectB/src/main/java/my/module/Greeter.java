package my.module;

public class Greeter {
    public static void main(String[] args) {
        System.out.println(greet());
        System.out.println(intro());
    }

    public static String greet() {

	// compilation error line
	must 'fail' [to] compile|line

        return "Hello, World!";
    }

    public static String intro() {
        return "I'm Gradle guinea pig";
    }
}
