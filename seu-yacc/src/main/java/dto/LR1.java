package dto;

import constant.SpSymbol;
import lombok.Data;

import java.util.*;

@Data
// 需要设置一个S'->S, $ 保证S'只出现在左部
public class LR1 {

    private Integer epsilonId; // epsilon的终结符编号
    private Integer dollarId; // dollar的终结符编号
    private Integer startNonTerminalId; // 文法开始符编号

    // symbol(包括终结符和非终结符）的标号和字符串形式的双向映射，0~127是ASCII字符，大于等于128是自定义终结符，小于等于-1是自定义非终结符
    private Map<Integer, String> numberToSymbol = new HashMap<>(); // 方便可视化
    private Map<String, Integer> symbolToNumber = new HashMap<>();

    // 非终结符的产生式，可有多个产生式，每个产生式有多个symbol，产生空串的产生式就是一个长度为0的List，key应该<0
    // key 非终结符编号 value 产生式编号集合
    private Map<Integer, List<Integer>> nonTerminalToProductionIds = new HashMap<>();

    // key 是产生式编号，value是编码后的产生式
    private Map<Integer, List<Integer>> productionIdToProduction = new HashMap<>(); // 由产生式序号得到产生式（编码后的symbol序列）
    // private Map<List<Integer>, Integer> productionToNumber = new HashMap<>(); // 由产生式得到产生式序号
    // key 是产生式编号，value是动作C代码
    private Map<Integer, String> productionAction = new HashMap<>(); // 有语义动作的产生式才会出现在这个map里，目前只支持语义动作出现在产生式最后，而且只有$$=$1+$2这种赋值操作

    // 终结符编号-优先级
    private Map<Integer, Integer> symbolPriority = new HashMap<>();
    // 产生式编号-优先级
    private Map<Integer, Integer> productionPriority = new HashMap<>();

    // key 非终结符编号 value 对应first集的终结符编号结合
    private Map<Integer, Set<Integer>> nonTerminalFirstSet = new HashMap<>();// 每个非终结符的first集，可能存在epsilon

    // 非终结符是否有空串，如果不在map里，说明还未被计算出来
    private Map<Integer, Boolean> nonTerminalHasEpsilon = new HashMap<>();

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
     * 根据一串序列（一定是右部）计算first，作为拓展后的预测符
     * 左递归问题 https://www.zhihu.com/question/407704983
     * @param sequence
     * @param preNonTerminals
     * @return
     */
    public Set<Integer> initialCalculateFirst(List<Integer> sequence, Set<Integer> preNonTerminals){

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
                    Set<Integer> subSet = initialCalculateFirst(sequence.subList(1, sequence.size()), preNonTerminals);
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
                        Set<Integer> tmpFirstSet = initialCalculateFirst(right, preNonTerminals);
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
                    Set<Integer> subSet = initialCalculateFirst(sequence.subList(1, sequence.size()), preNonTerminals);
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
