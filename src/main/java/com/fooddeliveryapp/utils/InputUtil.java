package com.fooddeliveryapp.utils;

import java.util.Scanner;

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

    public static String readEmail(String message) {
        while (true) {
            System.out.print(message);
            String email = scanner.nextLine().trim();

            if (email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                return email;
            }
            System.out.println("Invalid email address. Please enter a valid email.");
        }
    }

}

