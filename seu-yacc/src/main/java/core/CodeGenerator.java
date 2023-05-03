package core;

import constant.SpSymbol;
import dto.LR1;
import dto.ParseResult;

import java.util.List;
import java.util.Map;

public class CodeGenerator {
    /**
     *  1. 生成action表和goto表的时候，先遍历edges，找到移进的项（此时不会有冲突），终结符填在action表里，非终结符填在goto表里，填的是下一个状态号
     *  再遍历items，看看有没有可以规约的项，这时可能有移进-规约冲突或者规约-规约冲突，根据优先级判断保留哪个动作，这时要填负产生式号，代表规约
     *
     *  非终结符要取相反数再-1，让非终结符从0开始（goto表是独立于action表的另一张表，非终结符是列）
     *
     *  2. action表
     *  移进的时候要把当前token放入符号栈中，转移到下个状态，并将token流指针向后移动
     *  规约就不移动token流指针，先根据action表内容找对应产生式编号，通过产生式编号看非终结符(id)是哪个，看右部长度，决定从符号栈pop几个元素
     *  再把左部压入栈中，然后根据goto表看怎么转移状态
     *
     *  3. goto表就是直接看栈顶的非终结符，结合当前状态看应该转移到哪个状态，将新状态加入状态栈
     *
     *  4. 还需要一个产生式表，行号代表产生式号，只保存产生式长度就行，还要保存产生式字符串，用于可视化
     *  其中列的内容是symbol，非终结符仍然小于0，在C代码里用的时候要取相反数再-1转换一下
     *
     *  5. 每个symbol分配一个struct，包含symbolId和指针，如果symbolId<0（非终结符），指针指向孩子节点（规约的项目），在规约动作时会分配孩子
     *  如果symbolId>=0，指针指向与终结符类型对应的struct（比如标识符会有符号信息，字面常量会有数值信息）
      */

    // 可有可无
    public static String generateException() {
        return "void ArrayUpperBoundExceeded(void) {\n" +
                "    printf(\"Array upper bound exceeded!\");\n" +
                "  }\n" +
                "  void ArrayLowerBoundExceeded(void) {\n" +
                "    printf(\"Array lower bound exceeded!\");\n" +
                "  }\n" +
                "  void SomethingRedefined(void) {\n" +
                "    printf(\"Something redefined!\");\n" +
                "  }\n" +
                "  void SyntaxError(void) {\n" +
                "    printf(\"Syntax error!\");\n" +
                "  }\n" +
                "  void throw(void (*func)(void)) {\n" +
                "    atexit(func);\n" +
                "    exit(EXIT_FAILURE);\n" +
                "  }\n";
    }

    // 必要
    public static String generateExtern() {
        return "extern FILE *yyin;\n" +
                "extern char yytext[];\n" +
                "extern int yylex();\n" +
                "extern FILE *yyout;\n" +
                "extern YYSTYPE yylval;\n";
    }


    // 必要
    public static String generateNode(LR1 lr1) {
        int productionMaxLen = 0;
        for (List<Integer> production : lr1.getProductionIdToProduction().values()) {
            productionMaxLen = Math.max(production.size() + 1, productionMaxLen);
        }
        return "#define PRODUCTION_MAX_LEN " + productionMaxLen + "\n" +
                "struct Node {\n" +
                "\tint symbolId;\n" +
                "\tint pid;\n" +
                "\tYYSTYPE val;\n" +
                "\tstruct Node* children[PRODUCTION_MAX_LEN];\n" +
                "\tint children_num;\n" +
                "};\n" +
                "struct Node* root; // 语法树的根节点\n";

//                "  int nodeNum = 0;\n" +
//                "  void reduceNode(int num) {\n" +
//                "    struct Node *newNode = (struct Node *)malloc(sizeof(struct Node));\n" +
//                "    char *nonterminal = curToken;\n" +
//                "    if (nonterminal == NULL) nonterminal = curAttr;\n" +
//                "    newNode->childNum = num;\n" +
//                "    newNode->value = (char *)malloc(sizeof(char) * strlen(nonterminal));\n" +
//                "    newNode->yytext = (char *)malloc(sizeof(char) * strlen(curAttr));\n" +
//                "    strcpy(newNode->value, nonterminal);\n" +
//                "    strcpy(newNode->yytext, curAttr);\n" +
//                "    for (int i = 1; i <= num; i++) {\n" +
//                "      newNode->children[num-i] = nodes[nodeNum-i];\n" +
//                "      nodes[nodeNum-i] = NULL;\n" +
//                "    }\n" +
//                "    nodeNum = nodeNum - num;\n" +
//                "    nodes[nodeNum++] = newNode;\n" +
//                "  }\n";
    }

    // 需要修改，主要是规约时构建node
    public static String generateFunctions() {
        return "void updateSymbolAttr(int popNum) {\n" +
                "    char *temp = (char *)malloc(sizeof(char) * strlen(curAttr));\n" +
                "    strcpy(temp, curAttr);\n" +
                "    while (popNum--) {\n" +
                "      if (symbolAttrSize == 0) throw(ArrayLowerBoundExceeded);\n" +
                "      free(symbolAttr[--symbolAttrSize]);\n" +
                "    }\n" +
                "    if (symbolAttrSize >= SYMBOL_ATTR_LIMIT) throw(ArrayUpperBoundExceeded);\n" +
                "    symbolAttr[symbolAttrSize] = (char *)malloc(strlen(temp) * sizeof(char));\n" +
                "    strcpy(symbolAttr[symbolAttrSize++], temp);\n" +
                "  }\n" +
                "  int stateStackPop(int popNum) {\n" +
                "    while (popNum--) {\n" +
                "      if (stateStackSize == 0) throw(ArrayLowerBoundExceeded);\n" +
                "      stateStackSize--;\n" +
                "    }\n" +
                "    if (stateStackSize == 0) return YACC_NOTHING;\n" +
                "    else return stateStack[stateStackSize - 1];\n" +
                "  }\n" +
                "  void stateStackPush(int state) {\n" +
                "    if (stateStackSize >= STATE_STACK_LIMIT) throw(ArrayUpperBoundExceeded);\n" +
                "    stateStack[stateStackSize++] = state;\n" +
                "  }\n" +
                "  void reduceTo(char *nonterminal) {\n" +
                "    if (curToken != NULL) {\n" +
                "      free(curToken);\n" +
                "      curToken = NULL;\n" +
                "    }\n" +
                "    curToken = (char *)malloc(strlen(nonterminal) * sizeof(char));\n" +
                "    strcpy(curToken, nonterminal);\n" +
                "  }\n";
    }

    // 看情况修改
    public static String generatePreSetContent(LR1 lr1){
        // 结束符dollarID定义在 y.yab.h里，是END_OF_TOKEN_STREAM宏
        return "#include <stdio.h>\n" +
                "#include <stdlib.h>\n" +
                "#include <string.h>\n" +
                "#define STACK_LIMIT 1000\n" +
                "#define SYMBOL_CHART_LIMIT 10000\n" +
                "#define SYMBOL_ATTR_LIMIT 10000\n" +
                "#define STATE_STACK_LIMIT 10000\n" +
                "#define YACC_ERROR -1\n" +
                "#define YACC_NOTHING -2\n" +
                "#define YACC_ACCEPT -42\n" +
                generateException()+ "\n" +
                generateExtern()+ "\n" +
                "int stateStack[STACK_LIMIT];\n" +
                "int stateStackSize = 0;\n" +
                "int debugMode = 0;\n" +
                "char *symbolAttr[SYMBOL_ATTR_LIMIT];\n" +
                "int symbolAttrSize = 0;\n" +
                "char *curAttr = NULL;\n" +
                "char *curToken = NULL;\n" +
                "FILE *treeout = NULL;\n" +
                "int memoryAddrCnt = 0;\n" +
                generateNode(lr1)+ "\n" +
                generateFunctions()+ "\n";
    }

    /**
     * 非终结符编号要取
     * 包括action表，goto表，产生式表（从1开始） int production_table[][] 二维数组
     * production_table[pid][0]代表左部symbolId（负数），production_table[pid][1]代表产生式右部长度
     * char* production_string[] 也得从1开始，主要用于可视化，非终结符看pid，终结符pid为0，看symbolId
     *
     * 非终结符就用负数，查表时变换下标
     * @param lr1
     * @return
     */
    public static String generateTable(LR1 lr1, Map<Integer, Map<Integer, Integer>> graph){

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("int nonTerminalToColumnIndex(int i) { return -i - 1; }\n"); // 变换函数

        // 开始生成production_table
        StringBuilder productionTableMainPart = new StringBuilder();
        // 默认是有序从1开始排的
        for (Integer productionId : lr1.getProductionIdToProduction().keySet()) {
            List<Integer> production = lr1.getProductionIdToProduction().get(productionId);
            productionTableMainPart.append(String.format("{%d, %d},\n", production.get(0), production.size() - 1));
        }
        String productionTableString = String.format("int production_table[%d][2] = {\n{0, 0},\n%s};\n",
                lr1.getProductionIdToProduction().size() + 1, productionTableMainPart.toString());

        stringBuilder.append(productionTableString);

        // 生成production_string
        StringBuilder productionStringMainPart = new StringBuilder();
        // 默认是有序从1开始排的
        for (Integer productionId : lr1.getProductionIdToProduction().keySet()) {
            List<Integer> production = lr1.getProductionIdToProduction().get(productionId);
            productionStringMainPart.append("\"");
            productionStringMainPart.append(lr1.getNumberToSymbol().get(production.get(0)) + " -> ");
            for (int i = 1; i < production.size(); i++) {
                productionStringMainPart.append( lr1.getNumberToSymbol().get(production.get(i)) + " ");
            }
            productionStringMainPart.append("\",\n");
        }
        String productionStringString = String.format("char* production_string[%d] = {\n\"null\",\n%s};\n",
                lr1.getProductionIdToProduction().size() + 1, productionStringMainPart.toString());

        stringBuilder.append(productionStringString);



        // 产生action表和goto表

        TableGenerator tableGenerator = new TableGenerator(lr1, graph);

        StringBuilder actionBuilder = new StringBuilder();

        int[][] actionTable = tableGenerator.getActionTable();

        for (int[] row : actionTable) {
            actionBuilder.append("{");
            for (int i : row) {
                actionBuilder.append(i + ", ");
            }
            actionBuilder.deleteCharAt(actionBuilder.length() - 1);
            actionBuilder.deleteCharAt(actionBuilder.length() - 1); // 去掉最后一个,
            actionBuilder.append("},\n");
        }
        actionBuilder.deleteCharAt(actionBuilder.length() - 1);
        actionBuilder.deleteCharAt(actionBuilder.length() - 1); // 去掉最后一个,\n

        String actionString = String.format("int action_table[%d][%d] = {\n%s\n};\n", actionTable.length, actionTable[0].length, actionBuilder);
        stringBuilder.append(actionString);

        // goto table
        StringBuilder gotoBuilder = new StringBuilder();

        int[][] gotoTable = tableGenerator.getGotoTable();

        for (int[] row : gotoTable) {
            gotoBuilder.append("{");
            for (int i : row) {
                gotoBuilder.append(i + ", ");
            }
            gotoBuilder.deleteCharAt(gotoBuilder.length() - 1);
            gotoBuilder.deleteCharAt(gotoBuilder.length() - 1); // 去掉最后一个,
            gotoBuilder.append("},\n");
        }
        gotoBuilder.deleteCharAt(gotoBuilder.length() - 1);
        gotoBuilder.deleteCharAt(gotoBuilder.length() - 1); // 去掉最后一个,\n

        String gotoString = String.format("int goto_table[%d][%d] = {\n%s\n};\n", gotoTable.length, gotoTable[0].length, gotoBuilder);
        stringBuilder.append(gotoString);



        return stringBuilder.toString();
    }

    /**
     * 根据规约的产生式编号选择动作，在这里要把动作$$ = $1 + $3解析替换一下
     * @param lr1
     * @return
     */
    public static String generateActionSwitch(LR1 lr1){
        return "";
    }

    public static String generatePrintGrammarTree(){
        return "";
    }

    public static String generateYYParse(LR1 lr1){
        return "";
    }


    // 生成action表，goto表（有二义性要考虑优先级），生成解析token流的主函数
    // yacc基本功能是语法解析，只要从输入token流得到规约序列/语法树即可，符号表生成留给定义.y文件的用户来做，但要能解析 $$ = $1 + $3 的形式
    // 异常处理也是用户定义，但yacc要能提供行号

    /**
     * 状态转移表（两个二维数组，action和goto），以及产生式对应的长度（产生式id-产生式长度）
     * 符号栈里存放的是node，node包含symbolId，symbol的yylval，symbol的子节点，symbol的子节点数量（当时用的产生式的长度）
     * 状态栈就是单纯的int 状态号了
     * 读的时候调用yylex()得到下一个token，这时有当前的行号，yytext，yylval （边做词法分析边做语法分析，同时进行）
     * 当读到EOF token代表程序结束
     * @param parseResult
     * @param lr1
     * @return
     */
    public static String generateYTabC(ParseResult parseResult, LR1 lr1, Map<Integer, Map<Integer, Integer>> graph){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(
                "// * ============== copyPart ================\n");
        stringBuilder.append(parseResult.getPreCopy());

        stringBuilder.append("// * ========== seu-yacc generation ============\n");
        /* main part */
        stringBuilder.append(generatePreSetContent(lr1));
        stringBuilder.append(generateTable(lr1, graph));
        stringBuilder.append(generateActionSwitch(lr1));
        stringBuilder.append(generatePrintGrammarTree());
        stringBuilder.append(generateYYParse(lr1));

        stringBuilder.append("// * ============== cCodePart ===============\n");
        stringBuilder.append(parseResult.getUserCopy());



        return stringBuilder.toString();
    }

    // 生成 y.tab.h 主要是终结符的宏
    public static String generateYTabH(LR1 lr1){
        StringBuilder stringBuilder = new StringBuilder();

        for (Integer symbol : lr1.getNumberToSymbol().keySet()) {
            if(symbol < 128){ // 只考虑自定义的终结符，不考虑非终结符和ascii字符
                continue;
            }
            String symbolName = lr1.getNumberToSymbol().get(symbol);
            if (symbolName.equals(SpSymbol.DOLLAR)){ // 两个特殊字符的宏要特殊设置，在生成token流的时候最后要加一个END_OF_TOKEN_STREAM
                symbolName = "END_OF_TOKEN_STREAM";
            }
            else if(symbolName.equals(SpSymbol.EPSILON)){
                symbolName = "EPSILON";
            }
            stringBuilder.append(String.format("#define %s %d\n", symbolName, symbol));
        }

        String rsl = "#ifndef Y_TAB_H_\n" +
                "#define Y_TAB_H_\n" +
                "#define WHITESPACE -10\n" +
                stringBuilder.toString() +
                "#define YYSTYPE " + lr1.getUnionString() +
                "#endif\n";

        return rsl;
    }

}
