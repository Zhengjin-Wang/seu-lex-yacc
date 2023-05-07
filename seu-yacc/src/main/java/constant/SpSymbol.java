package constant;

public class SpSymbol {

    // 空产生式
    public static final String EPSILON = "ε";
    // 文法开始符号的预测符
    public static final String DOLLAR = "$";
    // 自定的文法开始符号，保证文法开始符号不会出现在右部，S'->%start, $
    public static final String START = "S'";
    // 规约1号产生式说明accept了
    public static final Integer acceptActionId = -1;

}
