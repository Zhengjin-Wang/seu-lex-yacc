package constant;

public class SpAlpha {
    public static final char EPSILON = 'ε';
    public static final String lexDistinctOperand = "[]*?+()|.^{},-"; // 不含转义字符和引号

    public static boolean isLexOperand(char c){
        return lexDistinctOperand.contains(Character.toString(c));
    }
}
