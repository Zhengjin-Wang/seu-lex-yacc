package constant;

public class SpAlpha {
    // 空串边
    public static final char EPSILON = 'ε';
    // 表示匹配换行符外的任意字符
    public static final char ANY = '任';
    // 表示连缀
    public static final char CONCAT = '连';
    // 不含转义字符和引号
    public static final String lexDistinctOperand = "[]*?+()|.^{},-";
    // []中需要转义的所有字符
    public static final String specialChars = "[]*?+()|.^{},-\\\"";
    // 这些字符的前边不允许出现CONCAT
    public static final String noConcatBefore = "|)*+?]";
    // 这些字符的后边不允许出现CONCAT
    public static final String noConcatAfter = "(|";
    // 中缀转后缀中出现的操作符
    public static final String infixOp = "()连|*+?";
    // 后缀表达式中出现的运算符
    public static final String postfixOp = "连|*+?";

    public static boolean isSpecialChars(char c){
        return specialChars.contains(Character.toString(c));
    }
    public static boolean isLexOperand(char c){
        return lexDistinctOperand.contains(Character.toString(c));
    }
    public static boolean notValidConcatBeforeSomeChar(char c){return  noConcatBefore.contains(Character.toString(c));}
    public static boolean notValidConcatAfterSomeChar(char c){return  noConcatAfter.contains(Character.toString(c));}
    public static boolean isInfixOp(char c){
        return infixOp.contains(Character.toString(c));
    }
    public static boolean isPostfixOp(char c) {return postfixOp.contains(Character.toString(c));}

    public static final String[] opPriorities = {"(", "|","连","*+?"};
    public static int getPriority(char c){
        for(int i = 0; i < opPriorities.length; ++i){
            if (opPriorities[i].contains(Character.toString(c))){
                return i;
            }
        }
        return -1;
    }
    /**
     * 比较a,b的优先级
     * 如果 栈顶字符a优先级 < 读入字符b优先级， 则把b压入栈，否则就会把b弹出append到结果字符串上
     * @param a
     * @param b
     * @return a优先级 < b优先级 则返回true
     */
    public static boolean infixOpPriorityCompare(char a, char b){
        return getPriority(a) < getPriority(b);
    }

}
