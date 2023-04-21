package dto;

import lombok.Data;

import java.io.Serializable;
import java.util.*;

@Data
public class DFA implements Serializable, FA {
    private int startState = -1; // 初态
    private Set<Integer> endStates = new HashSet<>(); // 终态（接受态），会有多个
    private Set<Integer> states = new HashSet<>();  // 所有状态
    private Set<Character> alphabet = new HashSet<>(); // 所有字符（边），包括ε
    private Map<Integer, Map<Character, List<Integer>>> transGraph = new HashMap<>(); // 状态转移图，邻接表形式,当某条边上的字符是SpAlpha.ANY，是说该状态的其他边都不匹配当前字符，最后一定会走ANY这条边
    private Map<Integer, LexAction> actionMap = new HashMap<>(); // 终态对应的动作

    public boolean isAtom() { // 是否只有一个字符，如果是这样，合并时可以直接将字符串联或并联到另一个NFA上（加边
        return states.size() == 2 && alphabet.size() == 1;
    }

    public void addState(int newState){
        states.add(newState);
    }

    /**
     * 将当前NFA加一条新边（从开始态到每一个终态）
     * ```
     *      ______c______
     *     |             ↓
     *    开始 --...--> 接收
     *
     * ```
     */
    // 状态可以不在集合当中，若不在，要添加到states和alphabet中，而startState和endStates的改变放在别的函数中处理
    public void addEdge(int beginState, Set<Integer> endS, char c){

        if(!alphabet.contains(c)){
            alphabet.add(c);
        }
        if(!states.contains(beginState)){
            states.add(beginState);
        }
        for (Integer i : endS) {
            if(!states.contains(i)){
                states.add(i);
            }
        }
        // 以上的判断不知道会不会影响效率

        if(!transGraph.containsKey(beginState)){
            transGraph.put(beginState, new HashMap<>());
        }
        Map<Character, List<Integer>> outEdges = transGraph.get(beginState);
        if(!outEdges.containsKey(c)){
            outEdges.put(c, new ArrayList<>());
        }
        List<Integer> reachStates = outEdges.get(c);

        // 需要加这个判断吗
        for (Integer reachState : reachStates) {
            if(endS.contains(reachState)){
                throw new RuntimeException("start --c--> end之前已经被添加过");
            }
        }

        // 会不会有重复的状态呢？ 如之前已经有start --c--> end 的边了
        reachStates.addAll(endS);

    }

    public DFA(){

    }
    // 构造一个原子NFA
    public DFA(char c, int startState, int endState){

        this.startState = startState;
        this.endStates.add(endState);
        this.states.add(startState);
        this.states.add(endState);
        this.alphabet.add(c);

        addEdge(this.startState, this.endStates, c);

    }
}
