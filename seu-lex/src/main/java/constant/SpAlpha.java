package constant;

public class SpAlpha {
    // 空串边
    public static final char EPSILON = 'ε';
    // 表示匹配换行符外的任意字符
    public static final char ANY = 65535;
    // 表示连缀
    public static final char CONCAT = 65534;
    // 不含转义字符和引号
    public static final String lexDistinctOperand = "[]*?+()|.^{},-";
    // []中需要转义的所有字符
    public static final String specialChars = "[]*?+()|.^{},-\\\"";

    public static boolean isSpecialChars(char c){
        return specialChars.contains(Character.toString(c));
    }
    public static boolean isLexOperand(char c){
        return lexDistinctOperand.contains(Character.toString(c));
    }

}
