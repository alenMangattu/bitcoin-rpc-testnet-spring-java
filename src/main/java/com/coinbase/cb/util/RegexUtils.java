package com.coinbase.cb.util;

public class RegexUtils {
    public static boolean isValidPassword(String password) {
        // Regex ensuring password meets specified policy
        String regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&\\-_,;])[A-Za-z\\d@$!%*?&\\-_,;]{12,240}$";
        return password != null && password.matches(regex);
    }



    public static boolean onlycharecters(String input) {
        String regex = "^[a-zA-Z]+$";
        System.out.println("CHecking input" + input);

        System.out.println("CHecking input" + input.matches(regex));
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(input);
//        return matcher.matches();
        return input != null && input.matches(regex);
    }

    public static boolean isValidEmail(String input) {
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        return input != null && input.matches(regex);
    }
}
