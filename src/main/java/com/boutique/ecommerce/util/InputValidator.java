package com.boutique.ecommerce.util;

import java.util.Scanner;

/**
 * InputValidator - Safe Console Input Helper
 *
 * Ensures all user input from console is parsed cleanly without crashing
 * due to InputMismatchException or lingering newlines in the buffer.
 */
public class InputValidator {

    /**
     * Reads an integer within a specified range [min, max].
     */
    public static int readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("  [!] Please enter a number between %d and %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid input. Please enter a valid whole number.");
            }
        }
    }

    /**
     * Reads a double value greater than or equal to a minimum threshold.
     */
    public static double readDouble(Scanner scanner, String prompt, double min) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                double value = Double.parseDouble(input);
                if (value >= min) {
                    return value;
                }
                System.out.printf("  [!] Value must be at least $%.2f.%n", min);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid input. Please enter a valid decimal number (e.g. 29.99).");
            }
        }
    }

    /**
     * Reads a non-empty string.
     */
    public static String readNonEmptyString(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) {
                return input;
            }
            System.out.println("  [!] This field cannot be left blank. Please try again.");
        }
    }

    /**
     * Reads an optional string, defaulting to an existing value if left blank (useful for updates).
     */
    public static String readOptionalString(Scanner scanner, String prompt, String currentValue) {
        System.out.print(prompt + " [Current: " + currentValue + "]: ");
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? currentValue : input;
    }

    /**
     * Reads an optional double, defaulting to existing value if blank.
     */
    public static double readOptionalDouble(Scanner scanner, String prompt, double currentValue, double min) {
        while (true) {
            System.out.printf("%s [Current: $%.2f]: ", prompt, currentValue);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return currentValue;
            }
            try {
                double value = Double.parseDouble(input);
                if (value >= min) {
                    return value;
                }
                System.out.printf("  [!] Value must be at least $%.2f.%n", min);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number. Please enter a valid decimal or press ENTER to keep current.");
            }
        }
    }

    /**
     * Reads an optional integer, defaulting to existing value if blank.
     */
    public static int readOptionalInt(Scanner scanner, String prompt, int currentValue, int min) {
        while (true) {
            System.out.printf("%s [Current: %d]: ", prompt, currentValue);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                return currentValue;
            }
            try {
                int value = Integer.parseInt(input);
                if (value >= min) {
                    return value;
                }
                System.out.printf("  [!] Value must be at least %d.%n", min);
            } catch (NumberFormatException e) {
                System.out.println("  [!] Invalid number. Please enter a valid integer or press ENTER to keep current.");
            }
        }
    }

    /**
     * Reads a valid email format.
     */
    public static String readEmail(Scanner scanner, String prompt) {
        while (true) {
            String email = readNonEmptyString(scanner, prompt);
            if (email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return email;
            }
            System.out.println("  [!] Invalid email format (example: name@domain.com). Please try again.");
        }
    }

    /**
     * Prompts for a yes/no confirmation.
     */
    public static boolean readConfirmation(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n): ");
            String input = scanner.nextLine().trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            } else if (input.equals("n") || input.equals("no")) {
                return false;
            }
            System.out.println("  [!] Please enter 'y' for yes or 'n' for no.");
        }
    }
}
