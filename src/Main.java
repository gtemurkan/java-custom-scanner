import java.io.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

public class Main {
    private static void dump(java.util.Scanner sc) {
        System.out.println("System:");
        sc.findAll(Pattern.compile(".*", Pattern.DOTALL)).map(MatchResult::group).forEach(System.out::print);
        System.out.println();
    }

    private static void dump(Scanner sc) {
        System.out.println("Custom:");
        sc.dump();
    }

    public static void main(String[] args) throws Exception {
        var system = new java.util.Scanner(new File("test/lorem-ipsum.txt"));
        var custom = new Scanner(new File("test/lorem-ipsum.txt"));
        system.useDelimiter(" [A-Z]");
        custom.useDelimiter(" [A-Z]");
//        System.out.println("System:");
        while (system.hasNext()) {
            System.out.printf("[%s]%n", system.next());
        }
//        System.out.println("Custom:");
//        for (int i = 0; i < 100 && custom.hasNext(); i++) {
//            System.out.printf("[%s]%n", custom.next());
//        }
    }
}