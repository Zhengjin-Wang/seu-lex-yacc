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
                "int yylineno = 1, yyleng = 0;\n" +
                "FILE *yyin = NULL, *yyout = NULL;\n" +
                "char yytext[1024] = {0};\n" +
                "char _cur_buf[1024] = {0};\n" +
                "int _cur_char = 0;\n" +
                "const int _init_state = " + dfa.getStartState() + ";\n" +
                "int _cur_state = _init_state, _cur_ptr = 0, _cur_buf_ptr = 0, _lat_acc_state = -1, _lat_acc_ptr = 0;\n" +
                "int yywrap();\n" +
                "int yylex();\n";
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

        stringBuilder.append("const int _trans_mat[" + stateNum + "][128] = {\n");

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
    public static String generateSwitchCase(DFA dfa){
        StringBuilder stringBuilder = new StringBuilder();
        // 由于最小化了，状态可能不连续，先获取最大的状态号
        Integer maxState = Collections.max(dfa.getStates());
        Integer stateNum = maxState + 1;

        stringBuilder.append("const int _swi_case[" + stateNum + "] = {");
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

    // 这个yylex只能匹配一次正则表达式，成功则执行动作，返回0，失败则返回-1
    public static String generateYYlex(DFA dfa){
        return "int yylex() {\n" +
                "      int rollbackLines = 0;\n" +
                "      if (yyin == NULL) yyin = stdin;\n" +
                "      if (yyout == NULL) yyout = stdout;\n" +
                "      if (_cur_char == EOF) {\n" +
                "        if (yywrap() == 1) return 0;\n" +
                "        else {\n" +
                "          yylineno = 1;\n" +
                "          yyleng = 0;\n" +
                "          memset(yytext, 0, sizeof(_cur_buf));\n" +
                "          memset(_cur_buf, 0, sizeof(_cur_buf));\n" +
                "          _cur_char = 0;\n" +
                "          _cur_state = _init_state, _cur_ptr = 0, _cur_buf_ptr = 0;\n" +
                "          _lat_acc_state = -1, _lat_acc_ptr = 0;\n" +
                "        }\n" +
                "      }\n" +
                "      while (_cur_state != -1) {\n" +
                "        _cur_char = fgetc(yyin); \n" +
                "        _cur_ptr++;\n" +
                "        if (_cur_char == '\\n') yylineno++, rollbackLines++;\n" +
                "        _cur_buf[_cur_buf_ptr++] = _cur_char;\n" +
                "        int _other = _trans_mat[_cur_state][0]; // 当前状态ANY边可达状态\n" +
                "        _cur_state = _trans_mat[_cur_state][_cur_char];\n" +
                "        if(_cur_state == -1) _cur_state =  _other; // 当前不能匹配，再试一下OTHER\n" +
                "        if (_swi_case[_cur_state] != -1) {\n" +
                "          _lat_acc_state = _cur_state;\n" +
                "          _lat_acc_ptr = _cur_ptr - 1;\n" +
                "          rollbackLines = 0;\n" +
                "        }\n" +
                "      }\n" +
                "      if (_lat_acc_state != -1) {\n" +
                "        fseek(yyin, _lat_acc_ptr - _cur_ptr + 1, SEEK_CUR);\n" +
                "        yylineno -= rollbackLines;\n" +
                "        _cur_ptr = _lat_acc_ptr;\n" +
                "        _cur_state = 0;\n" +
                "        _cur_buf[_cur_buf_ptr - 1] = '\\0';\n" +
                "        memset(yytext, 0, sizeof(yytext));\n" +
                "        yyleng = strlen(_cur_buf);\n" +
                "        strcpy(yytext, _cur_buf);\n" +
                "        memset(_cur_buf, 0, sizeof(_cur_buf));\n" +
                "        _cur_buf_ptr = 0;\n" +
                "        int _lat_acc_state_bak = _lat_acc_state;\n" +
                "        _lat_acc_state = -1;\n" +
                "        _lat_acc_ptr = 0;\n" +
                "        switch (_swi_case[_lat_acc_state_bak]) {\n" +
                generateSwitchAction(dfa) +
                "        }\n" +
                "      } else return -1; // error\n" +
                "      return 0; // FIXME!\n" +
                "    }\n";
    }


    public static String generateCode(ParseResult parseResult, DFA dfa){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(
                "// * ============== copyPart ================\n");
        stringBuilder.append(parseResult.getPreCopy());

        stringBuilder.append("// * ========== seulex generation ============\n");
        stringBuilder.append(generatePreContent(dfa));
        stringBuilder.append(generateTransMatrix(dfa));
        stringBuilder.append(generateSwitchCase(dfa));
        stringBuilder.append(generateYYlex(dfa));

        stringBuilder.append(generateYYless());
        stringBuilder.append(generateYYmore());

        stringBuilder.append("// * ============== cCodePart ===============\n");
        stringBuilder.append(parseResult.getUserCopy());

        return stringBuilder.toString();
    }

}
