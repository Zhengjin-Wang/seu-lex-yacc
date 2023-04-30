package core;

import constant.Associativity;
import dto.LR1;
import dto.LR1Item;
import dto.LR1State;
import lombok.Data;

import java.util.*;

import static java.util.Collections.sort;

/**
 * 状态表可能根据LR1或者LALR的dfa生成
 * LR1和LALR的symbol是一样的，区别在于状态直接的关系，LALR有些状态空缺了
 *
 *
 * 可视化：TableGeneratorTest.initializerLALRTest
 */
@Data
public class TableGenerator {

    private LR1 lr1;
    private Map<Integer, Map<Integer, Integer>> graph;
    private Map<Integer, Integer> stateIdToTableRowIndex; // 状态号到转移表行的映射，主要用于LALR状态表的缩减
    private Map<Integer, Integer> tableRowIndexToStateId; // 行号-状态号，主要用于可视化
    private Integer terminalCount; // 终结符数量，action表的列数
    private Integer nonTerminalCount; // 非终结符的数量，goto表的列数
    private Integer stateCount; // 状态数量

    private int[][] actionTable;
    private int[][] gotoTable;
    private Map<Integer, Map<Integer, Integer>> actionMap; // 用于可视化 状态-非终结符-移进或规约
    private Map<Integer, Map<Integer, Integer>> gotoMap; // 用于可视化   状态-非终结符-状态

    // 由lr1中负的非终结符编号，转换为goto表的列号
    public static Integer convertNonTerminalSymbolToTableColumnIndex(Integer symbolId){
        if(symbolId >= 0){
            throw new RuntimeException("required non-terminal symbol id, given terminal symbol id");
        }
        return -symbolId - 1;
    }

    // 由goto表的列号转换回非终结符id
    public static Integer convertGotoTableColumnIndexToSymbolId(Integer columnIndex){
        return -columnIndex - 1;
    }

    // 表中项0代表报错，大于0表示移进字符并转移到那个状态，小于0表示用哪个产生式规约
    private void generateActionTable(){
        for (Integer stateId : graph.keySet()) {
            Integer rowIndex = stateIdToTableRowIndex.get(stateId);
            // 先考虑移进
            Map<Integer, Integer> edges = graph.get(stateId);
            for (Integer shiftSymbol : edges.keySet()) {
                if (shiftSymbol < 0){ // 不考虑非终结符
                    continue;
                }
                Integer nextState = edges.get(shiftSymbol);
                Integer nextRowIndex = stateIdToTableRowIndex.get(nextState);
                actionTable[rowIndex][shiftSymbol] = nextRowIndex;
            }

            // 考虑规约
            LR1State lr1State = lr1.getStateIdToState().get(stateId);
            for (LR1Item item : lr1State.getItems()) {
                if(!item.isReducible(lr1)){ // 只考虑当前可规约的项目
                    continue;
                }
                int predictSymbol = item.getPredictSymbol(); // 一定是终结符
                int productionId = item.getProductionId();
                boolean shift = false; // 如果碰到冲突，是否移入，没冲突就一定不移入，设置规约

                if(actionTable[rowIndex][predictSymbol] != 0){ // 发生了移进-规约冲突，或者规约-规约冲突
                    // 如果这里报错了，可能要设置默认优先级和结合性
                    Integer shiftPriority = lr1.getSymbolPriority().get(predictSymbol);
                    Associativity shiftAssociativity = lr1.getSymbolAssociativity().get(predictSymbol);
                    Integer reducePriority = lr1.getProductionPriority().get(productionId);

                    if(reducePriority == null){ // 产生式没优先级就看结合性了
                        reducePriority = shiftPriority;
                    }
//                    if(shiftPriority == null){
//                        shiftPriority = 0;
//                    }

                    if(shiftPriority == null || reducePriority == null){
                        throw new RuntimeException("undefined priority but with shift/reduce conflict");
                    }

                    if(shiftPriority > reducePriority){ //移入
                        shift = true;
                    }
                    else if(shiftPriority < reducePriority){ //规约
                        shift = false;
                    }
                    else{ //优先级相同
                        if(shiftAssociativity == Associativity.LEFT){ // 左结合，规约
                            shift = false;
                        }
                        else if(shiftAssociativity == Associativity.RIGHT) { // 有结合，移入
                            shift = true;
                        }
                        else if(shiftAssociativity == Associativity.NONASSOC){
                            throw new RuntimeException("conflict with non-associative operator");
                        }
                    }
                }

                if(shift){ // 发生冲突，移入优先级高，就移入了
                    continue;
                }
                // 设置规约
                actionTable[rowIndex][predictSymbol] = - productionId; // -1就代表accept了，规约S' -> S·, $

            }

        }

    }

    // 表中项0代表报错，大于0表示转移到那个状态
    private void generateGotoTable(){
        for (Integer stateId : graph.keySet()) {
            Integer rowIndex = stateIdToTableRowIndex.get(stateId);
            // 非终结符只有移进
            Map<Integer, Integer> edges = graph.get(stateId);
            for (Integer shiftSymbol : edges.keySet()) {
                if (shiftSymbol >= 0) { // 不考虑终结符
                    continue;
                }
                Integer nextState = edges.get(shiftSymbol);
                Integer nextRowIndex = stateIdToTableRowIndex.get(nextState);
                Integer columnIndex = convertNonTerminalSymbolToTableColumnIndex(shiftSymbol);

                gotoTable[rowIndex][columnIndex] = nextRowIndex;
            }
        }
    }

    // 返回的项中有<0的别忘了取相反数，作为要规约的产生式号
    private void generateActionMap(){
        for (int i = 0; i < actionTable.length; i++) {
            int stateId = tableRowIndexToStateId.get(i);
            int[] row = actionTable[i];
            Map<Integer, Integer> rowMap = new LinkedHashMap<>();
            for (int j = 0; j < row.length; j++) {
                int action = row[j];
                if(action > 0){ // 移进的话要改一下跳转状态
                    action = tableRowIndexToStateId.get(action);
                }
                rowMap.put(j, action);
            }
            actionMap.put(stateId, rowMap);
        }
    }

    private void generateGotoMap(){
        for (int i = 0; i < gotoTable.length; i++) {
            int stateId = tableRowIndexToStateId.get(i);
            int[] row = gotoTable[i];
            Map<Integer, Integer> rowMap = new LinkedHashMap<>();
            for (int j = 0; j < row.length; j++) {
                int _goto = row[j];
                if(_goto > 0){
                    _goto = tableRowIndexToStateId.get(_goto);
                }
                else if(_goto < 0){
                    throw new RuntimeException("Error in goto table");
                }
                int symbolId = convertGotoTableColumnIndexToSymbolId(j);
                rowMap.put(symbolId, _goto);
            }
            gotoMap.put(stateId, rowMap);
        }
    }


    public TableGenerator(LR1 lr1, Map<Integer, Map<Integer, Integer>> graph){

        this.lr1 = lr1;
        this.graph = graph;
        this.terminalCount = Collections.max(lr1.getNumberToSymbol().keySet()) + 1; // 终结符的数量
        this.nonTerminalCount = - Collections.min(lr1.getNumberToSymbol().keySet());
        this.stateCount = graph.size();

        List<Integer> states = new ArrayList<>();
        states.addAll(graph.keySet());
        Collections.sort(states);

        stateIdToTableRowIndex = new HashMap<>();
        tableRowIndexToStateId = new HashMap<>();
        int cnt = 0;
        for (Integer stateId : states) {
            stateIdToTableRowIndex.put(stateId, cnt);
            tableRowIndexToStateId.put(cnt, stateId);
            ++cnt;
        }

        actionTable = new int[stateCount][terminalCount];
        gotoTable = new int[stateCount][nonTerminalCount];
        generateActionTable();
        generateGotoTable();

        actionMap = new LinkedHashMap<>();
        gotoMap = new LinkedHashMap<>();
        generateActionMap();
        generateGotoMap();

    }

}
