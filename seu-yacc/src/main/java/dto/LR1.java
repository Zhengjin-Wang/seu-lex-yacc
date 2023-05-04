package dto;

import constant.Associativity;
import constant.SpSymbol;
import lombok.Data;

import java.util.*;

@Data
// 需要设置一个S'->S, $ 保证S'只出现在左部
public class LR1 {

    private Integer epsilonId; // epsilon的终结符编号
    private Integer dollarId; // dollar的终结符编号
    private Integer startNonTerminalId; // 文法开始符编号

    private Set<Integer> occurredSymbols; // 出现过的symbol，用于可视化，action表只列出出现过的终结符

    private LR1State startState; // LR1Dfa的开始状态
    private Map<LR1State, Integer> stateToStateId = new LinkedHashMap<>(); // 状态到状态号的映射，用于判断重复的状态，linkedHashMap是为了生成LALR时合并状态的代表状态是状态号最小的
    private Map<Integer, LR1State> stateIdToState = new LinkedHashMap<>(); // 状态号-状态
    private Map<Integer, Map<Integer, Integer>> transGraph = new HashMap<>(); // 状态 --symbol--> 状态
    private Map<Integer, Map<Integer, Integer>> lalrTransGraph = new HashMap<>();

    // symbol(包括终结符和非终结符）的标号和字符串形式的双向映射，0~127是ASCII字符，大于等于128是自定义终结符，小于等于-1是自定义非终结符
    private Map<Integer, String> numberToSymbol = new LinkedHashMap<>(); // 方便可视化
    private Map<String, Integer> symbolToNumber = new HashMap<>();

    // 非终结符的产生式，可有多个产生式，每个产生式有多个symbol，产生空串的产生式就是一个长度为0的List，key应该<0
    // key 非终结符编号 value 产生式编号集合
    private Map<Integer, List<Integer>> nonTerminalToProductionIds = new HashMap<>();

    // key 是产生式编号，value是编码后的产生式
    private Map<Integer, List<Integer>> productionIdToProduction = new LinkedHashMap<>(); // 由产生式序号得到产生式（编码后的symbol序列） 1号是acc S'-> S
    // private Map<List<Integer>, Integer> productionToNumber = new HashMap<>(); // 由产生式得到产生式序号
    // key 是产生式编号，value是动作C代码
    private Map<Integer, String> productionAction = new HashMap<>(); // 有语义动作的产生式才会出现在这个map里，目前只支持语义动作出现在产生式最后，而且只有$$=$1+$2这种赋值操作

    // 终结符编号-优先级
    private Map<Integer, Integer> symbolPriority = new HashMap<>();
    // 终结符编号-结合性
    private Map<Integer, Associativity> symbolAssociativity = new HashMap<>();
    // 产生式编号-优先级
    private Map<Integer, Integer> productionPriority = new HashMap<>();

    // key 非终结符编号 value 对应first集的终结符编号结合
    private Map<Integer, Set<Integer>> nonTerminalFirstSet = new HashMap<>();// 每个非终结符的first集，可能存在epsilon

    // 非终结符是否有空串，如果不在map里，说明还未被计算出来
    private Map<Integer, Boolean> nonTerminalHasEpsilon = new HashMap<>();

    // symbol编号 - node.val.<type>
    private Map<Integer, String> symbolToUnionAttr = new HashMap<>();
    private String unionString;

    public Set<Integer> getOccurredSymbols(){
        if(occurredSymbols != null){
            return occurredSymbols;
        }
        occurredSymbols = new HashSet<>();
        for (List<Integer> production : productionIdToProduction.values()) {
            for (Integer symbol : production) {
                occurredSymbols.add(symbol);
            }
        }
        occurredSymbols.add(dollarId);

        return occurredSymbols;
    }

    /**
     * 对dotPos == production.size()的item，就不需要移动了，已到达可规约状态
     *
     * 对已经进行内部扩展的LR1状态进行外部扩展，根据core向右移动点，生成多个初始次状态，
     * 先对所有初始次状态做内扩展，然后用对应symbol编号连上这个状态，
     * 然后判断是否和之前的状态重复，只保留之前没生成过的状态，返回到LR1Builder中，设置状态号，然后把新状态加到队列里继续外扩展
     * @param lr1State
     * @return 生成的新状态集合（没有之前已经出现过的状态），返回后要在LR1Builder中给它们设置编号，然后把新状态加到队列里继续外扩展
     */
    public Map<Integer, LR1State> outerExpand(LR1State lr1State){
        // 先求出初始次状态
        // 然后对初始次状态做内扩展，无论是否重复，用对应symbol编号连上这个状态，判断是否重复，不重复就加到返回集合中，后续设置编号并加入队列
        // List<LR1State> newStates = new ArrayList<>();
        Map<Integer, LR1State> newStates = new LinkedHashMap<>();

        Map<Integer, List<LR1Item>> symbolToItems = new LinkedHashMap<>(); // 将item按移进符号分类，对应的是移进该symbol后的item
        // 先按symbol划分好item，构建初始的新状态
        for (LR1Item item : lr1State.getItems()) {
            if(item.isReducible(this)){ // 可规约的item就不会有移进了
                continue;
            }
            Integer shiftSymbol = item.getCurrentSymbolFromLR1(this);
            LR1Item nextItem = new LR1Item(item.getProductionId(), item.getDotPos() + 1, item.getPredictSymbol()); // 移进操作
            if(!symbolToItems.containsKey(shiftSymbol)){
                symbolToItems.put(shiftSymbol, new ArrayList<>());
            }
            symbolToItems.get(shiftSymbol).add(nextItem);
        }

        for (Integer shiftSymbol : symbolToItems.keySet()) {
            List<LR1Item> initItems = symbolToItems.get(shiftSymbol);
            LR1State state = new LR1State();
            state.getItems().addAll(initItems);
            innerExpand(state); // 先进行内扩展

            boolean repeat = false;
            for (LR1State oldState : stateToStateId.keySet()) {
                if(oldState.equalItems(state)){
                    repeat = true;
                    state = oldState; // 有重复的，边加在原来的state上
                    transGraph.get(lr1State.getStateId()).put(shiftSymbol, oldState.getStateId()); // 在状态图里也加上
                    break;
                }
            }

            lr1State.getEdges().put(shiftSymbol, state);

            if(!repeat){
                newStates.put(shiftSymbol, state);
            }

        }

        return newStates;
    }


    /**
     * 对一个初始的LR1状态进行内部扩展，扩展在原LR1状态上
     * @param lr1State
     * @return
     */
    public LR1State innerExpand(LR1State lr1State){

        Set<LR1Item> searchedItems = new HashSet<>();
        searchedItems.addAll(lr1State.getItems());
        Queue<LR1Item> queue = new ArrayDeque();
        queue.addAll(lr1State.getItems());

        while(!queue.isEmpty()){
            LR1Item item = queue.poll();
            // System.out.println(item.getProductionId() + "|" + item.getDotPos() + "|" + item.getPredictSymbol());
            if(item.isReducible(this)){ // 可规约，没得内扩展
                continue;
            }
            Integer symbol = item.getCurrentSymbolFromLR1(this);
            if(symbol >= 0){ // 是终结符，也不用内扩展
                continue;
            }
            // 是非终结符，有内扩展的可能
            // 先计算扩展symbol后边序列的first集，作为预测符
            List<Integer> production = item.getProductionFromLR1(this);

//            List<Integer> sequence = production.subList(item.getDotPos() + 1, production.size()); // 不能直接赋值，传递了对象的引用
            List<Integer> sequence = new ArrayList<>();
            sequence.addAll(production.subList(item.getDotPos() + 1, production.size()));// 获取扩展符之后的序列
            sequence.add(item.getPredictSymbol()); // 要把预测符加到序列中
            Set<Integer> firstSet = calculateFirstSet(sequence);

            for (Integer productionId : nonTerminalToProductionIds.get(symbol)) {
                for (Integer predictSymbol : firstSet) {
                    LR1Item tempItem = new LR1Item(); // 新item点符默认在1处
                    tempItem.setProductionId(productionId);
                    tempItem.setPredictSymbol(predictSymbol);

                    if(!searchedItems.contains(tempItem)){ // 是没被搜索到的item，标记为已搜索，加入到state中，并添加到队列
                        searchedItems.add(tempItem);
                        lr1State.getItems().add(tempItem);
                        queue.add(tempItem);
                    }
                }
            }

        }

        return lr1State;
    }


    /**
     * 根据symbol序列得到first集，用于产生LR1的item时计算预测符
     * @param sequence
     * @return
     */
    public Set<Integer> calculateFirstSet(List<Integer> sequence){
        Set<Integer> firstSet = new HashSet<>();
        if (sequence.size() == 0){
            firstSet.add(epsilonId);
            return firstSet;
        }

        int firstId = sequence.get(0);

        if(firstId >= 0){ // 是终结符
            firstSet.add(firstId);
        }
        else { // 非终结符
            Set<Integer> subFirstSet = nonTerminalFirstSet.get(firstId);

            firstSet.addAll(subFirstSet);

            if(subFirstSet.contains(epsilonId)){
                firstSet.remove(epsilonId);
                Set<Integer> subSet = calculateFirstSet(sequence.subList(1, sequence.size()));
                firstSet.addAll(subSet);
            }

        }

        return firstSet;
    }


    // 左递归问题 https://www.zhihu.com/question/407704983
    // 按照产生式，而不是非终结符来遍历
    /**
     * 遍历所有产生式来获取
     * @param sequence 右部的某一部分序列，初始就是某个非终结符的整个右部
     * @param preNonTerminals 已遇到的非终结符，初始是一个左部的非终结符
     * @return
     */
    public boolean judgeIfNonTerminalHasEpsilon(List<Integer> sequence, Set<Integer> preNonTerminals){

        if(sequence.size() == 0){ // 空产生式，一定有epsilon
            return true;
        }

        for (Integer symbol : sequence) {
            if(symbol >= 0) { // 有终结符，这个产生式一定没空串了
                return false;
            }
        }

        for (Integer symbol : sequence) { // 一定都是非终结符
            if(nonTerminalHasEpsilon.containsKey(symbol) && nonTerminalHasEpsilon.get(symbol) == true){ // 遇到了一个能产生空串的非终结符，往下走就行
                continue;
            }
            if(preNonTerminals.contains(symbol)){ // 遇到某种递归情况，忽视掉，从其他路径找
                return false;
            }

            List<Integer> productionIds = nonTerminalToProductionIds.get(symbol);

            boolean curSymbolHasEpsilon = false;
            for (Integer productionId : productionIds) {
                List<Integer> production = productionIdToProduction.get(productionId);
                preNonTerminals.add(symbol);
                boolean flag = judgeIfNonTerminalHasEpsilon(production.subList(1, production.size()), preNonTerminals);
                preNonTerminals.remove(symbol);
                if(flag){
                    curSymbolHasEpsilon = true;
                    break; // 当前的symbol可以产生一个空串，可以遍历下一个symbol了（针对于右部）
                }
                // 否则继续寻找有没有别的可能
            }

            if(!curSymbolHasEpsilon) return false;
        }

        return true; // 所有的非终结符都含有空串，那这个产生式可以把空串作为first

    }

    /**
     * 计算非终结符的first集
     * 根据一串序列（一定是右部）计算first，作为拓展后的预测符
     * 左递归问题 https://www.zhihu.com/question/407704983
     * @param sequence
     * @param preNonTerminals
     * @return
     */
    public Set<Integer> initialCalculateFirstSet(List<Integer> sequence, Set<Integer> preNonTerminals){

        Set<Integer> firstSet = new HashSet<>();
        if (sequence.size() == 0){
            firstSet.add(epsilonId);
            return firstSet;
        }

        int firstId = sequence.get(0);

        if(firstId >= 0){ // 是终结符
            firstSet.add(firstId);
        }
        else { // 非终结符
            if(preNonTerminals.contains(firstId)){ // 产生左递归
                if(nonTerminalHasEpsilon.get(firstId) == true){ // 能产生空串往下找就行
                    Set<Integer> subSet = initialCalculateFirstSet(sequence.subList(1, sequence.size()), preNonTerminals);
                    firstSet.addAll(subSet);
                }
                else { // 不能产生空串，终止在这了
                    return firstSet;
                }
            }
            else { // 没有左递归的正常情况

               // if(!nonTerminalFirstSet.containsKey(firstId)){ // firstId的first集还没被计算，则先计算

                    List<Integer> productionIds = nonTerminalToProductionIds.get(firstId);
                    Set<Integer> subFirstSet = new HashSet<>();
                    Integer left = firstId;

                    for (Integer productionId : productionIds) { //计算firstId所有产生式，构成first集
                        List<Integer> production = productionIdToProduction.get(productionId);
                        List<Integer> right = production.subList(1, production.size());

                        preNonTerminals.add(left);
                        Set<Integer> tmpFirstSet = initialCalculateFirstSet(right, preNonTerminals);
                        preNonTerminals.remove(left);

                        subFirstSet.addAll(tmpFirstSet); // 这个firstId非终结符的first集就是subFirstSet
                    }

                    if (!nonTerminalFirstSet.containsKey(firstId)){
                        nonTerminalFirstSet.put(firstId, subFirstSet);
                    }
                    else {
                        nonTerminalFirstSet.get(firstId).addAll(subFirstSet);
                    }
               // }

               // Set<Integer> subFirstSet = nonTerminalFirstSet.get(firstId);
                firstSet.addAll(subFirstSet);

                if(subFirstSet.contains(epsilonId)){
                    firstSet.remove(epsilonId);
                    Set<Integer> subSet = initialCalculateFirstSet(sequence.subList(1, sequence.size()), preNonTerminals);
                    firstSet.addAll(subSet);
                }

            }
        }

        return firstSet;
    }

//    /**
//     * 以动态规划的方式构建nonTerminal的first集
//     * @param nonTerminalId
//     * @param preNonTerminals 记录之前遍历
//     * @return
//     */
//    public Set<Integer> calculateNonTerminalFirst(Integer nonTerminalId, Set<Integer> preNonTerminals){
//
//        if(nonTerminalFirstSet.containsKey(nonTerminalId)){
//            return nonTerminalFirstSet.get(nonTerminalId);
//        }
//
//        Set<Integer> firstSet = new HashSet<>();
//
//        List<Integer> productions = leftToProductions.get(nonTerminalId); // 获取该非终结符的所有产生式编号
////        preNonTerminals.add(nonTerminalId);
//        for (Integer productionId : productions) {
//
//            List<Integer> production = numberToProduction.get(productionId);
//            List<Integer> rightPart = production.subList(1, production.size());
//            if (rightPart.size() == 0){ // 空产生式
//                firstSet.add(epsilonId);
//                continue;
//            }
//            else if(preNonTerminals.contains(rightPart.get(0))){ // 右部第一个非终结符在上级的first集查找出现过，避免递归自身
//
//            }
//
//            Set<Integer> subFirstSet = getFirst(rightPart); // 获取某一产生式右部的first集，如果右部为空，那就有epsilon
//            firstSet.addAll(subFirstSet);
//
//        }
//
//        nonTerminalFirstSet.put(nonTerminalId, firstSet);
//        return firstSet;
//    }


}
