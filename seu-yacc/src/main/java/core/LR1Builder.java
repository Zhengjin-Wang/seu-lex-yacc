package core;

import constant.Associativity;
import constant.SpSymbol;
import dto.*;

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
        parseResult.getExtendedProductionPriority().put(startProduction, null);


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

        // 设置终结符优先级，只有.y文件里设置了的终结符才有优先级，有优先级一定有结合性，没设置的默认优先级为0，结合性为LEFT
        for (String s : parseResult.getSymbolPriority().keySet()) {
            Integer index = lr1.getSymbolToNumber().get(s);
            if(index == null){ // parseResult.symbolPriority 的key不一定是终结符，也可能是给%prec的一个符号，用于判断产生式优先级
                continue;
            }
            Integer priority = parseResult.getSymbolPriority().get(s);
            Associativity associativity = parseResult.getSymbolAssociativity().get(s);
            lr1.getSymbolPriority().put(index, priority);
            lr1.getSymbolAssociativity().put(index, associativity);
        }

        // 需要分配默认的优先级和结合性吗? 暂时不分配，产生式若未设置优先级也是null
//        for (Integer symbol : lr1.getNumberToSymbol().keySet()) {
//            if(symbol >= 0 && !lr1.getSymbolPriority().containsKey(symbol)){ // 一个终结符，而且没有优先级，分配0作为优先级
//                lr1.getSymbolPriority().put(symbol, 0);
//                lr1.getSymbolAssociativity().put(symbol, Associativity.LEFT);
//            }
//        }

        // 可能存在着union定义
        for (String symbol : lr1.getSymbolToNumber().keySet()) {
            if(parseResult.getSymbolToUnionAttr().containsKey(symbol)){
                int symbolId = lr1.getSymbolToNumber().get(symbol);
                String unionAttr = parseResult.getSymbolToUnionAttr().get(symbol);
                lr1.getSymbolToUnionAttr().put(symbolId, unionAttr);
            }
        }

        lr1.setUnionString(parseResult.getUnionString());

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
                        // System.out.println(symbol);
                        int number = lr1.getSymbolToNumber().get(symbol); // 获取终结符或非终结符的编号
                        production.add(number);
                    }
                }
            }

            // productionPriority productions numberToProduction
            Integer priority = parseResult.getExtendedProductionPriority().get(strings);
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
        lr1.getStateIdToState().put(startStateId, startState);
        if(!lr1.getTransGraph().containsKey(startStateId)) {
            lr1.getTransGraph().put(startStateId, new HashMap<>());
        }
        lr1.setStartState(startState);

        // 开始不断扩展状态
        Queue<LR1State> queue = new ArrayDeque<>();
        queue.add(startState);
        while(!queue.isEmpty()){
            LR1State curState = queue.poll();
            Map<Integer, LR1State> nextStates = lr1.outerExpand(curState); // 获得的是之前没出现过的状态
            for (Integer shiftSymbol : nextStates.keySet()) { // 如果为空，本轮就不会添加任何状态，不为空则添加状态
                LR1State nextState = nextStates.get(shiftSymbol);
                int stateId = getLr1StateId();
                nextState.setStateId(stateId);
                lr1.getStateToStateId().put(nextState, stateId);
                lr1.getStateIdToState().put(stateId, nextState);
                if(!lr1.getTransGraph().containsKey(stateId)){
                    lr1.getTransGraph().put(stateId, new HashMap<>()); // 新添加的状态先加到转移图里
                }
                lr1.getTransGraph().get(curState.getStateId()).put(shiftSymbol, stateId); // 连上边，注意在outerExpand里还要连上重复的状态

                queue.add(nextState);
            }

//            for (LR1State lr1State : lr1.getStateToStateId().keySet()) {
//                System.out.println(lr1State.getStateId() + " : " + lr1State.getItems());
//            }
//            System.out.println("-----------------------");
        }

    }


    public static LR1 buildLR1(ParseResult parseResult){
        LR1 lr1 = new LR1();

        assignID(lr1, parseResult);
        encodeProduction(lr1, parseResult);
        long startTime = System.currentTimeMillis();
        calculateFirstSet(lr1);
        long endTime = System.currentTimeMillis();
        System.out.println("计算first集时间：" + (endTime - startTime) + " 毫秒");
        startTime = System.currentTimeMillis();
        generateLR1Dfa(lr1);
        endTime = System.currentTimeMillis();
        System.out.println("计算LR1状态转移图时间：" + (endTime - startTime) + " 毫秒");

        return lr1;
    }

    private static LR1 buildLALRFromLR1(LR1 lr1){

        Map<Set<LR1ItemCore>, LR1State> stateCoreToNewState = new HashMap<>(); // state核-新状态的所有item
        Map<Integer, Integer> oldStateIdToNewStateId = new HashMap<>(); // 旧状态号-新的代表状态号（同state核集合中的首个state）

        // 找到旧项集-新项集（同stateCore集合的代表状态）映射，把新项集预测符扩展为旧项集的所有预测符
        for (LR1State state : lr1.getStateToStateId().keySet()) { // 如果StateToStateId用LinkedHashMap，状态号小的就成为代表状态，否则顺序不确定，不知道顺序有什么影响
            Set<LR1ItemCore> stateCore = state.getLr1StateCore();

            int newStateId = state.getStateId();
            LR1State newState = stateCoreToNewState.get(stateCore);

            if(newState == null){ // 遇到了新的stateCore
                stateCoreToNewState.put(stateCore, state);
            }
            else{
                newStateId = stateCoreToNewState.get(stateCore).getStateId();
                newState.getItems().addAll(state.getItems());
            }

            oldStateIdToNewStateId.put(state.getStateId(), newStateId);

        }

        // 遍历旧的transGraph，根据oldStateIdToNewStateId构造lalrTransGraph，并修改每个state的边
        for (Integer fromState : lr1.getTransGraph().keySet()) {
            Integer mapState = oldStateIdToNewStateId.get(fromState);
            if (!fromState.equals(mapState)){ //只有新状态旧-新状态号映射是相同的，抛弃那些被代表的状态
                continue;
            }
            lr1.getLalrTransGraph().put(fromState, new HashMap<>());
            Map<Integer, Integer> edges = lr1.getTransGraph().get(fromState);
            LR1State concreteFromState = lr1.getStateIdToState().get(fromState);
            Map<Integer, LR1State> newOwnEdges = new HashMap<>(); // 当前state自己的状态，因为visual部分用的是指针形式，得改它的边
            for (Integer shiftSymbol : edges.keySet()) {
                Integer nextState = edges.get(shiftSymbol);
                Integer toState = oldStateIdToNewStateId.get(nextState);

                lr1.getLalrTransGraph().get(fromState).put(shiftSymbol, toState); // 先在转移图设置

                LR1State concreteToState = lr1.getStateIdToState().get(toState);
                newOwnEdges.put(shiftSymbol, concreteToState); // 还要设置本节点的边
            }
            concreteFromState.setEdges(newOwnEdges); // 还要设置本节点的边
        }

//        System.out.println(stateCoreToNewState.size());
//        System.out.println(oldStateIdToNewStateId);
//        System.out.println(lr1.getTransGraph().size());
//        System.out.println(lr1.getLalrTransGraph().size());

        return lr1;
    }

    public static LR1 buildLALR(ParseResult parseResult){
        LR1 lr1 = buildLR1(parseResult);
        LR1 lalr = buildLALRFromLR1(lr1);
        return lalr;
    }



}
