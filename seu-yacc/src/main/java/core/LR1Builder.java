package core;

import constant.SpSymbol;
import dto.LR1;
import dto.ParseResult;

import java.util.ArrayList;
import java.util.List;

public class LR1Builder {

    private Integer terminalId = 128;
    private Integer nonTerminalId = -1;
    private Integer getTerminalId() {return terminalId++;}
    private Integer getNonTerminalId() {return nonTerminalId--;}

    private Integer productionId = 1; // 从1开始计，在动作表里-production_id表示规约该产生式，正数则表示移进对应编号终结符
    private Integer getProductionId() {return  productionId++;}

    // 为ParseResult已存在的终结符，非终结符分配id
    public void assignID(LR1 lr1, ParseResult parseResult){

        // 分配ascii
        for(int i = 0; i < 128; ++i){
            String symbol = "'" + (char) i + "'";
            lr1.getSymbolToNumber().put(symbol, i);
            lr1.getNumberToSymbol().put(i, symbol);
        }

        // 分配定义的终结符
        for (String terminal : parseResult.getTerminals()) {
            int id = getTerminalId();
            lr1.getSymbolToNumber().put(terminal, id);
            lr1.getNumberToSymbol().put(id, terminal);
        }

        // 分配特殊字符
        int epsilonId = getTerminalId();
        int dollarId = getTerminalId();
        lr1.getSymbolToNumber().put(SpSymbol.EPSILON, epsilonId);
        lr1.getSymbolToNumber().put(SpSymbol.DOLLAR, dollarId);
        lr1.getNumberToSymbol().put(epsilonId, SpSymbol.EPSILON);
        lr1.getNumberToSymbol().put(dollarId, SpSymbol.DOLLAR);
        lr1.setEpsilon(epsilonId);
        lr1.setDollar(dollarId);

        // 分配定义的非终结符
        for (String nonTerminal : parseResult.getNonTerminals()) {
            lr1.getSymbolToNumber().put(nonTerminal, getNonTerminalId());
        }

        // 设置终结符优先级
        for (String s : parseResult.getSymbolPriority().keySet()) {
            int index = lr1.getSymbolToNumber().get(s);
            int priority = parseResult.getSymbolPriority().get(s);
            lr1.getSymbolPriority().put(index, priority);
        }

    }

    // @deprecated（旧注释） 开头为'说明是ASCII终结符，{说明要生成一个哑元非终结符，加到numberToSymbol和dummyNonTerminalAction中，否则判断是否在terminals或nonterminals，取出对应编码
    // 将production变成数字形式，也就是把终结符和非终结符替换为编好的码
    public void encodeProduction(LR1 lr1, ParseResult parseResult){
        for (List<String> strings : parseResult.getExtendedProductionList()) {
            int pid = getProductionId();
            List<Integer> production = new ArrayList<>(); // 是完整的表达式，包括左部
            String leftStr = strings.get(0);
            int left = lr1.getSymbolToNumber().get(leftStr);
            if(strings.size() == 1){ // 这是一个空产生式 A->epsilon

            }
            for (int i = 1; i < strings.size(); i++) { // 遍历右部
                String symbol = strings.get(i); // 可能是终结符或非终结符，也可能是动作
                if(symbol.charAt(0) == '{'){ // 暂时认为动作只在最后，一个产生式只有一个

                }
                else{

                }
            }
            // productionPriority productions numberToProduction
        }

    }

    public void calculateFirstSet(LR1 lr1){

    }

    public LR1 buildLR1(ParseResult parseResult){
        LR1 lr1 = new LR1();

        return lr1;
    }

}
