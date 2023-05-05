package core;

import constant.SpSymbol;
import dto.LR1;
import dto.ParseResult;

import java.util.List;
import java.util.Map;

import static java.lang.Math.max;

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

    // 必要
    public static String generateNode(LR1 lr1) {
        int productionMaxLen = 0;
        for (List<Integer> production : lr1.getProductionIdToProduction().values()) {
            productionMaxLen = max(production.size() + 1, productionMaxLen);
        }
        return "#define PRODUCTION_MAX_LEN " + productionMaxLen + "\n" +
                "struct Node {\n" +
                "\tint symbolId;\n" +
                "\tint pid;\n" +
                "\tYYSTYPE val;\n" +
                "\tstruct Node* children[PRODUCTION_MAX_LEN];\n" +
                "\tint children_num;\n" +
                "\tint nodeId; // 用于标记节点\n" +
                "};\n" +
                "int node_cnt = 0;\n" +
                "int getNodeId() { return node_cnt++; }\n" +
                "struct Node* createNode() {\n" +
                "\tstruct Node* node = (struct Node*)malloc(sizeof(struct Node));\n" +
                "\tnode->nodeId = getNodeId();\n" +
                "\treturn node;\n" +
                "}\n" +
                "struct Node* root; // 语法树的根节点\n";


    }


    // 看情况修改
    public static String generatePreSetContent(LR1 lr1){
        // 结束符dollarID定义在 y.yab.h里，是END_OF_TOKEN_STREAM宏
        return "#include <stdio.h>\n" +
                "#include <stdlib.h>\n" +
                "#include <string.h>\n" +
                "#include \"y.tab.h\"\n" +
                "#define SYMBOL_STACK_LIMIT 1000\n" +
                "#define STATE_STACK_LIMIT 10000\n" +
                "\n" +
                "extern FILE* yyin;\n" +
                "extern FILE* yyout;\n" +
                "extern char yytext[];\n" +
                "extern int yylex();\n" +
                "extern YYSTYPE yylval;\n" +
                "extern int yylineno;\n" +
                "\n" +
                "void errorGrammar(void) {\n" +
                "\tprintf(\"Grammar error occured!\");\n" +
                "}\n" +
                "void errorLexical(void) {\n" +
                "\tprintf(\"Lexical error occured!\");\n" +
                "}\n" +
                "\n" +
                "void stackOverflow(void) {\n" +
                "\tprintf(\"Stack overflow!\");\n" +
                "}\n" +
                "void popEmptyStack(void) {\n" +
                "\tprintf(\"Pop empty stack!\");\n" +
                "}\n" +
                "void throw(void (*func)(void)){\n" +
                "\tprintf(\"Error occurred in line %d, before word %s.\\n\", yylineno, yytext);\n" +
                "  atexit(func);\n" +
                "  exit(EXIT_FAILURE);\n" +
                "}\n" +
                "FILE* graphviz_file;\n" +
                generateNode(lr1)+ "\n" +
                "struct Node* symbol_stack[SYMBOL_STACK_LIMIT];\n" +
                "int state_stack[STATE_STACK_LIMIT];\n" +
                "int symbol_stack_ptr = 0;\n" +
                "int state_stack_ptr = 0;\n" +
                "\n" +
                "struct Node* symbolStackTop() {\n" +
                "\tif (symbol_stack_ptr == 0) {\n" +
                "\t\tthrow(popEmptyStack);\n" +
                "\t}\n" +
                "\treturn symbol_stack[symbol_stack_ptr - 1];\n" +
                "}\n" +
                "struct Node* popSymbolStack() {\n" +
                "\tif (symbol_stack_ptr == 0) {\n" +
                "\t\tthrow(popEmptyStack);\n" +
                "\t}\n" +
                "\treturn symbol_stack[--symbol_stack_ptr];\n" +
                "}\n" +
                "void pushSymbolStack(struct Node* symbol) {\n" +
                "\tif (symbol_stack_ptr == SYMBOL_STACK_LIMIT) {\n" +
                "\t\tthrow(stackOverflow);\n" +
                "\t}\n" +
                "\tsymbol_stack[symbol_stack_ptr++] = symbol;\n" +
                "}\n" +
                "int stateStackTop() {\n" +
                "\tif (state_stack_ptr == 0) {\n" +
                "\t\tthrow(popEmptyStack);\n" +
                "\t}\n" +
                "\treturn state_stack[state_stack_ptr - 1];\n" +
                "}\n" +
                "int popStateStack(){\n" +
                "\tif (state_stack_ptr == 0) {\n" +
                "\t\tthrow(popEmptyStack);\n" +
                "\t}\n" +
                "\treturn state_stack[--state_stack_ptr];\n" +
                "}\n" +
                "void pushStateStack(int state) {\n" +
                "\tif (state_stack_ptr == STATE_STACK_LIMIT) {\n" +
                "\t\tthrow(stackOverflow);\n" +
                "\t}\n" +
                "\tstate_stack[state_stack_ptr++] = state;\n" +
                "}\n";
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
        stringBuilder.append("int reduceActionToProductionId(int i) { return -i; }\n");

        // ascii
        StringBuilder asciiTableBuilder = new StringBuilder();
        for (int i = 0; i < 128; i++) {
            asciiTableBuilder.append(i + ",0,");
        }
        String symbolTableString = String.format("char ascii_string[256] = {%s};\n", asciiTableBuilder.toString());
        stringBuilder.append(symbolTableString);

        // 生成终结符
        int symbolNum = 0;
        StringBuilder symbolTableBuilder = new StringBuilder();

        for (Integer symbolId : lr1.getNumberToSymbol().keySet()) {
            if(symbolId >= 128){
                symbolTableBuilder.append("\"" + lr1.getNumberToSymbol().get(symbolId) + "\",");
                ++symbolNum;
            }
        }
        symbolTableString = String.format("char* symbol_string[%d] = {%s};\n",symbolNum, symbolTableBuilder.toString());
        stringBuilder.append(symbolTableString);
        stringBuilder.append("char* symbolIdToString(int i) {\n" +
                "\tif (i >= 128) return symbol_string[i - 128];\n" +
                "\treturn ascii_string + (2 * i);\n" +
                "}\n");

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
                int symbolId = production.get(i);
                String symbol = lr1.getNumberToSymbol().get(symbolId);
                // if(symbolId >= 0 && symbolId < 128) symbol = symbol.substring(1,2);
                productionStringMainPart.append( symbol + " ");
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
     * 如过要支持自定义YYSTYPE，需要修改这个函数
     * @param lr1
     * @return
     */
    public static String generateActionSwitch(LR1 lr1){
        StringBuilder stringBuilder = new StringBuilder();
        for (Integer pid : lr1.getProductionAction().keySet()) {
            stringBuilder.append("case " + pid + ":\n");
            String action = lr1.getProductionAction().get(pid);
            StringBuilder actionBuilder = new StringBuilder();
            List<Integer> production = lr1.getProductionIdToProduction().get(pid); // 用于找到symbolId

            for (int i = 0; i < action.length(); i++) {
                char c = action.charAt(i);
                if(c != '$'){
                    actionBuilder.append(c);
                }
                else{
                    ++i;
                    c = action.charAt(i);
                    if(c == '$'){ // 是左部
                        actionBuilder.append("node->val");
                        int symbolId = production.get(0);
                        String unionAttr = lr1.getSymbolToUnionAttr().get(symbolId);
                        if(unionAttr != null) {
                            actionBuilder.append("." + unionAttr);
                        }
                    }
                    else{ // 右部
                        String number = "";
                        while(action.charAt(i) >= '0' && action.charAt(i) <= '9'){
                            number = number + action.charAt(i);
                            ++i;
                        }
                        --i;
                        actionBuilder.append(String.format("(node->children[%s])->val", number));

                        int index = Integer.parseInt(number);
                        int symbolId = production.get(index);
                        String unionAttr = lr1.getSymbolToUnionAttr().get(symbolId);
                        if(unionAttr != null) {
                            actionBuilder.append("." + unionAttr);
                        }
                    }

                }
            }
            stringBuilder.append(actionBuilder);
            stringBuilder.append("\nbreak;\n");
        }
        return "void doSemanticAction(int pid, struct Node* node) {\n" +
                "\tswitch (pid)\n" +
                "\t{\n" +
                stringBuilder.toString() +
                "\tdefault:\n" +
                "\t\tbreak;\n" +
                "\t}\n" +
                "}\n";
    }

    public static String generatePrintGrammarTree(){
        return "void printGrammarTree(struct Node* node, int depth) {\n" +
                "\tfor (int i = 0; i < depth; ++i) {\n" +
                "\t\tprintf(\"\\t\"); // 根据深度确定缩进\n" +
                "\t}\n" +
                "\tprintf(\"|\");\n" +
                "\tif (node->symbolId >= 0) { // 终结符\n" +
                "\t\tprintf(\"%s\\n\", symbolIdToString(node->symbolId));\n" +
                "\t\tfprintf(graphviz_file, \"%d [label = \\\"%s\\\"]\\n\", node->nodeId, symbolIdToString(node->symbolId));\n" +
                "\t\treturn;\n" +
                "\t}\n" +
                "\tprintf(\"%s\\n\", production_string[node->pid]);\n" +
                "\tfprintf(graphviz_file, \"%d [label = \\\"%s\\\"]\\n\", node->nodeId, production_string[node->pid]);\n" +
                "\tfor (int i = 1; i <= node->children_num; ++i) {\n" +
                "\t\tfprintf(graphviz_file, \"%d -> %d\\n\", node->nodeId, node->children[i]->nodeId);\n" +
                "\t\tprintGrammarTree(node->children[i], depth + 1);\n" +
                "\t}\n" +
                "}\n" +
                "\n" +
                "void outputGraphvizFile() {\n" +
                "\tgraphviz_file = fopen(\"grammar_tree.dot\", \"w\");\n" +
                "\tfprintf(graphviz_file, \"digraph G{ \\n rankdir = TB \\n node[shape = box] \\n node[fontname = \\\"SimHei\\\"] \\n edge[fontname = \\\"SimHei\\\"] \\n\");\n" +
                "\tprintGrammarTree(root, 0);\n" +
                "\tfprintf(graphviz_file, \"}\");\n" +
                "\tfflush(graphviz_file);\n" +
                "\tsystem(\"dot -Tpng grammar_tree.dot -o grammar_tree.png -Gdpi=150\");\n" +
                "}\n";
    }

    public static String generateYYParse(LR1 lr1){
        return "int yyparse() {\n" +
                "\tint start_state = " + lr1.getStartState().getStateId() + "; // 需要在java里设置\n" +
                "\tint token = 0;\n" +
                "\tpushStateStack(start_state);\n" +
                "\twhile (token != END_OF_TOKEN_STREAM && (token = yylex()) >= -2) { // 遇到EOF会跳出循环\n" +
                "\t\t\n" +
                "\t\tif (token == -1) { // error\n" +
                "\t\t\tthrow(errorLexical);\n" +
                "\t\t\treturn -1;\n" +
                "\t\t}\n" +
                "\n" +
                "\t\tif (token == -2) continue; // white space\n" +
                "\n" +
                "\t\tif (token == 0) token = END_OF_TOKEN_STREAM;\n" +
                "\n" +
                "\t\tint action = action_table[stateStackTop()][token];\n" +
                "\n" +
                "\t\twhile ((action = action_table[stateStackTop()][token]) < 0) { // 能规约就一直规约，直到把当前token移进，或者一开始就是移进\n" +
                "\t\t\tif (action == -1) {\n" +
                "\t\t\t\troot = symbolStackTop(); // 这地方就没S'了\n" +
                "\t\t\t\treturn 0; // 接受态，成功解析，必须在这返回，不然S'的goto表没有能转移的状态，一定会报错\n" +
                "\t\t\t}\n" +
                "\t\t\tint pid = reduceActionToProductionId(action);\n" +
                "\t\t\tint nonTerminalId = production_table[pid][0];\n" +
                "\t\t\tint productionLen = production_table[pid][1];\n" +
                "\n" +
                "\t\t\tstruct Node* node = createNode();\n" +
                "\t\t\tnode->symbolId = nonTerminalId;\n" +
                "\t\t\tnode->pid = pid;\n" +
                "\t\t\tnode->children_num = productionLen;\n" +
                "\n" +
                "\t\t\tfor (int i = productionLen; i >= 1; --i) {\n" +
                "\t\t\t\tnode->children[i] = popSymbolStack();\n" +
                "\t\t\t\tpopStateStack();\n" +
                "\t\t\t}\n" +
                "\t\t\t// 计算node->val\n" +
                "\t\t\tdoSemanticAction(pid, node);\n" +
                "\t\t\tpushSymbolStack(node);\n" +
                "\t\t\tint col = nonTerminalToColumnIndex(nonTerminalId);\n" +
                "\t\t\tint curState = stateStackTop();\n" +
                "\t\t\tint nextState = goto_table[curState][col];\n" +
                "\t\t\tpushStateStack(nextState);\n" +
                "\t\t}\n" +
                "\n" +
                "\t\tif (action == 0) { // 出错\n" +
                "\t\t\tthrow(errorGrammar);\n" +
                "\t\t\treturn -1;\n" +
                "\t\t}\n" +
                "\t\telse if (action > 0) { // 移进\n" +
                "\t\t\tpushStateStack(action);\n" +
                "\t\t\tstruct Node* node = createNode();\n" +
                "\t\t\tnode->symbolId = token;\n" +
                "\t\t\t// node->val = yylval;\n" +
                "\t\t\tmemcpy((void*) &(node->val), (void*) &yylval, sizeof(YYSTYPE));\n" +
                "\t\t\tpushSymbolStack(node);\n" +
                "\t\t}\n" +
                "\t\t\n" +
                "\t}\n" +
                "\n" +
                "\treturn -1; // 出现异常\n" +
                "\n" +
                "}\n";
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

        String unionDefine = "";
        if(lr1.getUnionString() != null){
            unionDefine = "#define YYSTYPE " + lr1.getUnionString();
        }
        else{
            unionDefine = "#define YYSTYPE int\n";
        }

        String rsl = "#ifndef Y_TAB_H_\n" +
                "#define Y_TAB_H_\n" +
                stringBuilder.toString() +
                unionDefine +
                "#endif\n";

        return rsl;
    }

}
