package sharpeye.sharpeye.utils;

/**
 * Adds functions to deal with strings
 */
public class Str {

    /**
     * Allows to check if the string is either null or empty at once
     * @param string your string to test
     * @return a boolean value telling if the string is null or empty
     */
    public static boolean IsNullOrEmpty(String string)
    {
        return (string == null || string.isEmpty());
    }
}
