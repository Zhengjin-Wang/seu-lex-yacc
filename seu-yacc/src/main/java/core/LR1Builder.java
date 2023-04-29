package core;

import constant.SpSymbol;
import dto.LR1;
import dto.LR1Item;
import dto.LR1State;
import dto.ParseResult;

import java.util.*;

public class LR1Builder {

    private static Integer terminalId = 128;
    private static Integer nonTerminalId = -1;
    private static Integer getTerminalId() {return terminalId++;}
    private static Integer getNonTerminalId() {return nonTerminalId--;}

    private static Integer productionId = 1; // 从1开始计，在动作表里-production_id表示规约该产生式，正数则表示移进对应编号终结符
    private static Integer getProductionId() {return  productionId++;}

    private static Integer lr1StateId = 0;
    private static Integer getLr1StateId() {return  lr1StateId++;}

    // 为ParseResult已存在的终结符，非终结符分配id
    public static void assignID(LR1 lr1, ParseResult parseResult){

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
        lr1.setEpsilonId(epsilonId);
        lr1.setDollarId(dollarId);
        lr1.getSymbolToNumber().put(SpSymbol.EPSILON, epsilonId);
        lr1.getSymbolToNumber().put(SpSymbol.DOLLAR, dollarId);
        lr1.getNumberToSymbol().put(epsilonId, SpSymbol.EPSILON);
        lr1.getNumberToSymbol().put(dollarId, SpSymbol.DOLLAR);

        // 加上自定的文法开始符号
        int startNonTerminalId = getNonTerminalId();
        lr1.setStartNonTerminalId(startNonTerminalId);
        lr1.getSymbolToNumber().put(SpSymbol.START, startNonTerminalId); // 原.y文件不要出现S'作为非终结符
        lr1.getNumberToSymbol().put(startNonTerminalId, SpSymbol.START);
        // 顺便构造一个字符串产生式[S', %start]，方便后续产生式编码
        List<String> startProduction = new ArrayList<>();
        startProduction.add(SpSymbol.START);
        startProduction.add(parseResult.getStartSymbol());
        parseResult.getExtendedProductionList().add(0, startProduction);
        parseResult.getExtendedProductionPriority().put(startProduction, 0);


        // 分配定义的非终结符
        for (String nonTerminal : parseResult.getNonTerminals()) {
            int id = getNonTerminalId();
            lr1.getSymbolToNumber().put(nonTerminal, id);
            lr1.getNumberToSymbol().put(id, nonTerminal);
        }
//        lr1.getSymbolToNumber().forEach((k,v)->{
//            System.out.println(k + " : " + v);
//        });
//        parseResult.getSymbolPriority().forEach((k,v)->{
//            System.out.println(k + " : " + v);
//        });

        // 设置终结符优先级
        for (String s : parseResult.getSymbolPriority().keySet()) {
            Integer index = lr1.getSymbolToNumber().get(s);
            if(index == null){ // parseResult.symbolPriority 的key不一定是终结符，也可能是给%prec的一个符号，用于判断产生式优先级
                continue;
            }
            Integer priority = parseResult.getSymbolPriority().get(s);
            lr1.getSymbolPriority().put(index, priority);
        }

    }

    // @deprecated（旧注释） 开头为'说明是ASCII终结符，{说明要生成一个哑元非终结符，加到numberToSymbol和dummyNonTerminalAction中，否则判断是否在terminals或nonterminals，取出对应编码
    // 将production变成数字形式，也就是把终结符和非终结符替换为编好的码
    public static void encodeProduction(LR1 lr1, ParseResult parseResult){
        for (List<String> strings : parseResult.getExtendedProductionList()) {
            int pid = getProductionId();
            List<Integer> production = new ArrayList<>(); // 是完整的表达式，包括左部

            String leftStr = strings.get(0);
            int left = lr1.getSymbolToNumber().get(leftStr); // 左部非终结符编号
            production.add(left);

            if(strings.size() == 1){ // 这是一个空产生式 A->epsilon
                // 产生式长度设为1还是2? A->  和 A-> SpSymbol.epsilon 的区别，暂时让空产生式长度为1
            }
            else {
                for (int i = 1; i < strings.size(); i++) { // 遍历右部
                    String symbol = strings.get(i); // 可能是终结符或非终结符，也可能是动作
                    if (symbol.charAt(0) == '{') { // 暂时认为动作只在最后，一个产生式只有一个
                        if(!lr1.getProductionAction().containsKey(pid)){
                            lr1.getProductionAction().put(pid, "");
                        }
                        String action = lr1.getProductionAction().get(pid) + symbol;
                        lr1.getProductionAction().put(pid, action);
                    } else {
                        int number = lr1.getSymbolToNumber().get(symbol); // 获取终结符或非终结符的编号
                        production.add(number);
                    }
                }
            }

            // productionPriority productions numberToProduction
            int priority = parseResult.getExtendedProductionPriority().get(strings);
            lr1.getProductionPriority().put(pid, priority);
            if(!lr1.getNonTerminalToProductionIds().containsKey(left)){
                lr1.getNonTerminalToProductionIds().put(left, new ArrayList<>());
            }
            lr1.getNonTerminalToProductionIds().get(left).add(pid); // 以left为左部的产生式编号集合
            lr1.getProductionIdToProduction().put(pid, production); // 确定产生式编号和具体的产生式
        }

    }

    public static void calculateNonTerminalHasEpsilon(LR1 lr1){

        for (Integer left : lr1.getNonTerminalToProductionIds().keySet()) { // 先把可以直接产生epsilon的非终结符找出来
            List<Integer> productionIds = lr1.getNonTerminalToProductionIds().get(left);

            for (Integer id : productionIds) {
                List<Integer> production = lr1.getProductionIdToProduction().get(id);
                if (production.size() == 1){
                    lr1.getNonTerminalHasEpsilon().put(left, true);
                    break;
                }
            }

        }

        for (Integer left : lr1.getNonTerminalToProductionIds().keySet()) {
            if (lr1.getNonTerminalHasEpsilon().containsKey(left)){ // 跳过上一步确定好是否含有epsilon的非终结符
                continue;
            }
            List<Integer> productionIds = lr1.getNonTerminalToProductionIds().get(left);
            Set<Integer> preNonterminalIds = new HashSet<>();
            preNonterminalIds.add(left);
            boolean flag = false;
            for (Integer id : productionIds) {
                List<Integer> production = lr1.getProductionIdToProduction().get(id);
                boolean hasEpsilon = lr1.judgeIfNonTerminalHasEpsilon(production.subList(1, production.size()), preNonterminalIds);
                if(hasEpsilon){ // 有一个产生式可以产生epsilon这个非终结符的first集就有epsilon
                    flag = true;
                    break;
                }
            }

            lr1.getNonTerminalHasEpsilon().put(left, flag);
        }
    }

    public static void calculateFirstSet(LR1 lr1){

        // 首先要获取所有非终结符first集是否包含空串边的情况
        calculateNonTerminalHasEpsilon(lr1);


        // 循环所有产生式，找first集
        for (Integer left : lr1.getNonTerminalToProductionIds().keySet()) {
//            if(lr1.getNonTerminalFirstSet().containsKey(left)){ // 已找到first集，不用再找
//                continue;
//            }

            List<Integer> productionIds = lr1.getNonTerminalToProductionIds().get(left);
            Set<Integer> firstSet = new HashSet<>();
            Set<Integer> preNonTerminals = new HashSet<>();

            for (Integer id : productionIds) {
                List<Integer> production = lr1.getProductionIdToProduction().get(id);

                List<Integer> right = production.subList(1, production.size());

                preNonTerminals.add(left);
                Set<Integer> tmpFirstSet = lr1.initialCalculateFirstSet(right, preNonTerminals); // 一个产生式的first集
                preNonTerminals.remove(left);

                firstSet.addAll(tmpFirstSet);
            }

            if(!lr1.getNonTerminalFirstSet().containsKey(left)){
                lr1.getNonTerminalFirstSet().put(left, firstSet);
            }
            else{
                lr1.getNonTerminalFirstSet().get(left).addAll(firstSet);
            }


        }

    }

    // 生成LR1状态图
    public static void generateLR1Dfa(LR1 lr1){
        // 获取文法开始符号的产生式（唯一的）
        Integer startNonTerminalId = lr1.getStartNonTerminalId();
        Integer startProductionId = lr1.getNonTerminalToProductionIds().get(startNonTerminalId).get(0);

        // 第一个item
        LR1Item startItem = new LR1Item();
        startItem.setProductionId(startProductionId);
        startItem.setPredictSymbol(lr1.getDollarId()); // 设置初始的预测符

        // 第一个state
        Integer startStateId = getLr1StateId();

        LR1State startState = new LR1State();
        startState.getItems().add(startItem);
        startState = lr1.innerExpand(startState);

        startState.setStateId(startStateId);
        lr1.getStateToStateId().put(startState, startStateId);
        lr1.setStartState(startState);

        // 开始不断扩展状态
        Queue<LR1State> queue = new ArrayDeque<>();
        queue.add(startState);
        while(!queue.isEmpty()){
            LR1State curState = queue.poll();
            List<LR1State> nextStates = lr1.outerExpand(curState); // 获得的是之前没出现过的状态
            for (LR1State nextState : nextStates) { // 如果为空，本轮就不会添加任何状态
                int stateId = getLr1StateId();
                nextState.setStateId(stateId);
                lr1.getStateToStateId().put(nextState, stateId);

                queue.add(nextState);
            }
        }


    }


    public static LR1 buildLR1(ParseResult parseResult){
        LR1 lr1 = new LR1();

        assignID(lr1, parseResult);
        encodeProduction(lr1, parseResult);
        calculateFirstSet(lr1);
        generateLR1Dfa(lr1);

        return lr1;
    }

}
