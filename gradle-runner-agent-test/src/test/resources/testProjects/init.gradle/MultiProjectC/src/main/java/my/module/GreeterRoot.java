package my.module;

public class GreeterRoot {
    public static void main(String[] args) {
        System.out.println(greet());
        System.out.println(intro());
    }

    public static String greet() {
        return "Hello, World!";
    }

    public static String intro() {
        return "I'm Root guinea pig";
    }
}
