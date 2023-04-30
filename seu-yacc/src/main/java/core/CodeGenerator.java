package core;

import constant.SpSymbol;
import dto.LR1;
import dto.ParseResult;

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
     *  4. 还需要一个产生式表，行号代表产生式号
     *  其中列的内容是symbol，非终结符仍然小于0，在C代码里用的时候要取相反数再-1转换一下
      */

    // 生成action表，goto表（有二义性要考虑优先级），生成解析token流的主函数
    public static String generateYTabC(ParseResult parseResult, LR1 lr1){
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(
                "// * ============== copyPart ================\n");
        stringBuilder.append(parseResult.getPreCopy());

        stringBuilder.append("// * ========== seu-yacc generation ============\n");
        /* main part */


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
                "#endif\n";

        return rsl;
    }

}
