package core;

import constant.SpAlpha;
import dto.DFA;
import dto.ParseResult;

import java.util.*;

public class CodeGenerator {

    // 解析字符的数量
    private static final Integer charNum = 128;

    // 预置变量代码
    public static String generatePreContent(DFA dfa){
        return  "#include <stdio.h>\n" +
                "#include <stdlib.h>\n" +
                "#include <string.h>\n" +
                "#define ECHO fprintf(yyout,\"%s\\n\",yytext);\n" +
                "#ifndef YYSTYPE\n" +
                "#define YYSTYPE int\n" +
                "#endif \n" +
                "YYSTYPE yylval;\n" +
                "int yylineno = 1, yyleng = 0;\n" +
                "FILE *yyin = NULL, *yyout = NULL;\n" +
                "char yytext[1024] = {0};\n" +
                "char _buffer[1024] = {0};\n" +
                "int cur_char = 0;\n" +
                "const int init_state = "+ dfa.getStartState() +";\n" +
                "int cur_state = init_state, cur_ptr = 0, _buffer_ptr = 0, last_acc_state = -1, last_acc_ptr = 0;\n" +
                "int yywrap();\n" +
                "int yylex();";
    }

    public static String generateYYless(){
        return "void yyless(int n) {\n" +
                "      int delta = strlen(yytext) - n;\n" +
                "      fseek(yyin, -delta, SEEK_CUR);\n" +
                "      FILE *yyinCopy = yyin;\n" +
                "      while (delta--) fgetc(yyinCopy) == '\\n' && yylineno--;\n" +
                "    }\n";
    }

    public static String generateYYmore(){
        return "void yymore() {\n" +
                "      char old[1024];\n" +
                "      strcpy(old, yytext);\n" +
                "      yylex();\n" +
                "      strcpy(yytext, strcat(old, yytext));\n" +
                "    }\n";
    }

    public static String generateTransMatrix(DFA dfa){
        StringBuilder stringBuilder = new StringBuilder();
        // 由于最小化了，状态可能不连续，先获取最大的状态号
        Integer maxState = Collections.max(dfa.getStates());
        Integer stateNum = maxState + 1;

        stringBuilder.append("const int transfer_matrix[" + stateNum + "][128] = {\n");

        // SpAlpha.ANY 表示其他字符，如果当前状态下读入的字符会转移到-1状态，先试试能不能通过字符0转移，字符0就代表OTHER
        for(int i = 0; i < stateNum; ++i){
            stringBuilder.append("{");
            Map<Character, List<Integer>> outEdges = dfa.getTransGraph().get(i);
            if(outEdges == null) {// 不存在这个状态或者这个状态没有出边
                stringBuilder.append("-1,".repeat(charNum));
            }
            else{
                // 先判断有没有ANY，ANY作为字符0转移下一个状态
                if(outEdges.containsKey(SpAlpha.ANY)){
                    Integer nextState = outEdges.get(SpAlpha.ANY).get(0);
                    stringBuilder.append(nextState + ",");
                }
                else{
                    stringBuilder.append("-1,");
                }
                for(int c = 1; c < charNum; ++c){
                    char ch = (char) c;
                    if(outEdges.containsKey(ch)){
                        Integer nextState = outEdges.get(ch).get(0);
                        stringBuilder.append(nextState + ",");
                    }
                    else{
                        stringBuilder.append("-1,");
                    }
                }
            }
            stringBuilder.deleteCharAt(stringBuilder.length()-1); // 去掉最后一个,
            stringBuilder.append("},\n");
        }

        stringBuilder.append("};\n");

        return stringBuilder.toString();
    }

    // 构造状态和switch case number的关系，如果值为-1，表示非终态，终态下标和值相同
    public static String generateAcceptStateTable(DFA dfa){
        StringBuilder stringBuilder = new StringBuilder();
        // 由于最小化了，状态可能不连续，先获取最大的状态号
        Integer maxState = Collections.max(dfa.getStates());
        Integer stateNum = maxState + 1;

        stringBuilder.append("const int acc_table[" + stateNum + "] = {");
        for(int i = 0; i < stateNum; ++i){
            if(dfa.getEndStates().contains(i)){
                stringBuilder.append(i + ",");
            }
            else {
                stringBuilder.append("-1,");
            }
        }
        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        stringBuilder.append("};\n");

        return stringBuilder.toString();
    }

    public static String generateSwitchAction(DFA dfa){
        StringBuilder stringBuilder = new StringBuilder();

        for (Integer endState : dfa.getActionMap().keySet()) {
            stringBuilder.append("\t\t\tcase " + endState + ":\n");
            stringBuilder.append("\t\t\t\t"+dfa.getActionMap().get(endState).getAction());
            stringBuilder.append("\n\t\t\t\tbreak;\n");
        }

        stringBuilder.append("\t\t\tdefault:\n" +
                             "\t\t\t\tbreak;\n");

        return stringBuilder.toString();
    }

    // 命令行交互，回车后stdin可能已经刷新，因此不能再识别第一次匹配到内容以后的内容，因为是直接匹配stdin流中的内容
    // 这个yylex只能匹配一次正则表达式，成功则执行动作，返回自定义宏（其实就是yacc定义的终结符，用在yacc），读到EOF返回0，在yyparse()读到0的话就用END_OF_TOKEN_STREAM代替，失败则返回-1
    public static String generateYYlex(DFA dfa){
        return "int yylex() {\n" +
                "      int rollbackLines = 0;\n" +
                "      if (yyin == NULL) yyin = stdin;\n" +
                "      if (yyout == NULL) yyout = stdout;\n" +
                "      if (cur_char == EOF) {\n" +
                "        if (yywrap() == 1) return 0;\n" +
                "        else {\n" +
                "          yylineno = 1;\n" +
                "          yyleng = 0;\n" +
                "          memset(yytext, 0, sizeof(yytext));\n" +
                "          memset(_buffer, 0, sizeof(_buffer));\n" +
                "          cur_char = 0;\n" +
                "          cur_state = init_state, cur_ptr = 0, _buffer_ptr = 0, last_acc_state = -1, last_acc_ptr = 0;\n" +
                "        }\n" +
                "      }\n" +
                "\n" +
                "      while (cur_state != -1) {\n" +
                "        cur_char = fgetc(yyin); \n" +
                "        cur_ptr++;\n" +
                "        if (cur_char == '\\n') yylineno++, rollbackLines++;\n" +
                "        _buffer[_buffer_ptr++] = cur_char;\n" +
                "        int _other = transfer_matrix[cur_state][0]; // 当前状态OTHER边可达状态\n" +
                "        cur_state = transfer_matrix[cur_state][cur_char]; // 从当前字符转移到的状态\n" +
                "        if(cur_state == -1 && cur_char != '\\n') cur_state =  _other; // 当前不能匹配，再试一下OTHER\n" +
                "        if (cur_state == -1) break; // 还不能匹配，说明已经离开状态机接收范围了\n" +
                "        if (acc_table[cur_state] != -1) { // 到达了一个终态\n" +
                "          last_acc_state = cur_state;\n" +
                "          last_acc_ptr = cur_ptr - 1; // cur_ptr已经往后移动了一格，当前字符的位置是上一格\n" +
                "          rollbackLines = 0;\n" +
                "        }\n" +
                "      }\n" +
                "\n" +
                "      if (last_acc_state != -1) {\n" +
                "        fseek(yyin, last_acc_ptr - (cur_ptr - 1) , SEEK_CUR); // 当前字符被读入了，指针回退一格，要把它返回到输入流，作为下一次匹配的开始\n" +
                "        yylineno -= rollbackLines;\n" +
                "        cur_ptr = last_acc_ptr;\n" +
                "        cur_state = 0;\n" +
                "        _buffer[_buffer_ptr - 1] = '\\0'; // 把上一个无效字符覆盖为\\0\n" +
                "        memset(yytext, 0, sizeof(yytext));\n" +
                "        yyleng = strlen(_buffer);\n" +
                "        strcpy(yytext, _buffer);\n" +
                "        memset(_buffer, 0, sizeof(_buffer));\n" +
                "        _buffer_ptr = 0;\n" +
                "        int acc_state = last_acc_state;\n" +
                "        last_acc_state = -1;\n" +
                "        last_acc_ptr = 0;\n" +
                "        switch (acc_state) {" +
                generateSwitchAction(dfa) +
                "        }\n" +
                "      } else return -1; // error 从一个非终态跳出状态机\n" +
                "      return -2; // yyparse() continue, meet an ignorable token\n" +
                "    }\n";
    }


    public static String generateCode(ParseResult parseResult, DFA dfa){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(
                "// * ============== copyPart ================\n");
        stringBuilder.append(parseResult.getPreCopy());

        stringBuilder.append("// * ========== seu-lex generation ============\n");
        stringBuilder.append(generatePreContent(dfa));
        stringBuilder.append(generateTransMatrix(dfa));
        stringBuilder.append(generateAcceptStateTable(dfa));
        stringBuilder.append(generateYYlex(dfa));

        stringBuilder.append(generateYYless());
        stringBuilder.append(generateYYmore());

        stringBuilder.append("// * ============== cCodePart ===============\n");
        stringBuilder.append(parseResult.getUserCopy());

        return stringBuilder.toString();
    }

}
