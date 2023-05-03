import core.CodeGenerator;
import core.DFABuilder;
import core.LexParser;
import core.NFABuilder;
import dto.DFA;
import dto.NFA;
import dto.ParseResult;
import utils.VisualizeUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LexMain {

    public static void main(String[] args) {
        System.out.println("--------seu-lex--------");
        if(args.length == 0){
            System.out.println("Missing argument .l file");
        }
        else{
            File file = new File(args[0]);
            if(!file.canRead() || !file.isFile()){
                System.out.println("Not a valid file");
                return;
            }
            System.out.println("Running...");
            ParseResult parseResult = LexParser.getParseResult(file);
//            System.out.println(parseResult.getRegexAction());
            NFA nfa = NFABuilder.buildNFA(parseResult);
            DFA bigDfa = DFABuilder.buildDFA(nfa);
            DFA dfa = DFABuilder.minimizeDFA(bigDfa);
            String code = CodeGenerator.generateCode(parseResult, dfa);
            // 命令行交互，回车后stdin可能已经刷新，因此不能再识别第一次匹配到内容以后的内容，因为是直接匹配stdin流中的内容
            /**
             * 考虑在.y文件里调用yylex()，while(_cur_char != EOF) {
             *      int rsl = yylex();
             *      if(rsl == -1) //异常
             *      else{
             *         // 返回值是不同词法单元对应的宏，语义动作在.l文件中已经定义好（比如标识符会添加一个name-val映射）
             *         // 词法单元是ID宏+标识符名
             *         // 数字的词法单元则是NUM宏+数值
             *
             *         // 这里应该有添加词法单元到序列的代码
             *      }
             * }
             *
             */


            // 生成c文件
            File yyFile = new File("yy.seulex.c");
            try {
                FileWriter fileWriter = new FileWriter(yyFile);
                fileWriter.write(code);
                fileWriter.flush();
                fileWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            // 可视化
            VisualizeUtils.visualizeFA(dfa); // 图片默认生成在jar包同级目录下

        }
    }
}
