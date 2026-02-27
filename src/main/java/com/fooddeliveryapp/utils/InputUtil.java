package com.fooddeliveryapp.utils;

import java.util.Scanner;
import java.util.regex.Pattern;

public class InputUtil {

    private static final Scanner scanner = new Scanner(System.in);

    public static int readInt(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value < 0) {
                    System.out.println("Invalid input. Please enter a positive integer.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter an integer.");
            }
        }
    }

    public static double readDouble(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value < 0) {
                    System.out.println("Invalid input. Please enter a positive number.");
                    continue;
                }
                return value;
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
            }
        }
    }


    public static String readString(String message) {
        System.out.print(message);
        return scanner.nextLine().trim();
    }

    public static char readChar(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input.charAt(0);
            }
            System.out.println("Invalid input. Please enter a character.");
        }
    }

    public static boolean confirmAction(String actionPrompt) {
        while (true) {
            System.out.print(actionPrompt + " (Yes/No): ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y")) {
                return true;
            } else if (input.equalsIgnoreCase("no") || input.equalsIgnoreCase("n")) {
                return false;
            } else {
                System.out.println("Invalid input. Please enter either Yes or No.");
            }
        }
    }

    public static String readValidName(String message) {
        while (true) {
            String name = readString(message);
            if (name.matches("[a-zA-Z ]+")) {
                return name;
            }
            System.out.println("Invalid name! Name should contain only alphabets.");
        }
    }

    public static boolean readBoolean(String message) {
        while (true) {
            System.out.print(message);
            String input = scanner.nextLine().trim().replace(" ","").toLowerCase();

            if (input.equals("true") || input.equals("yes") || input.equals("y")) {
                return true;
            } else if (input.equals("false") || input.equals("no") || input.equals("n")) {
                return false;
            } else {
                System.out.println("Invalid input. Please enter yes/no or true/false.");
            }
        }
    }

    // Stronger email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^(?=.{1,254}$)(?=.{1,64}@)" +               // overall length + local part length
                    "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+" +
                    "(\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@" +   // local part
                    "[A-Za-z0-9]" +
                    "([A-Za-z0-9-]{0,61}[A-Za-z0-9])?" +
                    "(\\.[A-Za-z]{2,})+$",                       // domain + TLD
            Pattern.CASE_INSENSITIVE
    );

    public static String readEmail(String message) {
        while (true) {
            System.out.print(message);
            String email = scanner.nextLine().trim();

            if (EMAIL_PATTERN.matcher(email).matches()) {
                return email;
            }

            System.out.println("Invalid email address. Please enter a valid email (e.g., user@example.com).");
        }
    }

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])" +          // at least 1 lowercase
                    "(?=.*[A-Z])" +           // at least 1 uppercase
                    "(?=.*\\d)" +             // at least 1 digit
                    "(?=.*[@$!%*?&^#()_+=-])" + // at least 1 special char
                    "[A-Za-z\\d@$!%*?&^#()_+=-]" +
                    "{8,64}$"                 // length between 8 and 64
    );

    public static String readPassword(String message) {
        while (true) {
            System.out.print(message);
            String password = scanner.nextLine();

            if (PASSWORD_PATTERN.matcher(password).matches()) {
                return password;
            }

            System.out.println("""
            Invalid password. Password must:
            - Be 8–64 characters long
            - Contain at least one uppercase letter
            - Contain at least one lowercase letter
            - Contain at least one digit
            - Contain at least one special character
            - Contain no spaces
            """);
        }
    }
}

