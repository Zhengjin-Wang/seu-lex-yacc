package dto;

import constant.SpAlpha;
import lombok.Data;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;
import java.util.*;

/**
 * 根据LexParser获取到的ParseResult生成的NFA
 */
@Data
public class NFA implements Serializable, FA { // 正则表达式转NFA时，遇到转义字符\，当作一个操作符，可以操作[ \（lex操作符），也可以操作n r t（用作转义），如果是其他普通字符，则保持原字符
                    // 只有遇到n r t才替换为对应转义后字符，其他就保留原字符
                    // 当某条边上的字符是SpAlpha.ANY，表明任何读入的字符都能被接收，转移到下一状态

    public static Long timeDiff = 0L;

    private int startState = -1; // 初态
    private Set<Integer> endStates = new HashSet<>(); // 终态（接受态），会有多个
    private Set<Integer> states = new HashSet<>();  // 所有状态
    private Set<Character> alphabet = new HashSet<>(); // 所有字符（边），包括ε
    private Map<Integer, Map<Character, List<Integer>>> transGraph = new HashMap<>(); // 状态转移图，邻接表形式,当某条边上的字符是SpAlpha.ANY，表明任何读入的字符都能被接收，转移到下一状态
    private Map<Integer, LexAction> actionMap = new HashMap<>(); // 终态对应的动作

    public boolean isAtom() { // 是否只有一个字符，如果是这样，合并时可以直接将字符串联或并联到另一个NFA上（加边
        return states.size() == 2 && alphabet.size() == 1;
    }

    public void addState(int newState){
        states.add(newState);
    }

    //返回某个状态所有epsilon可达状态，包括它自己
//    private Set<Integer> getEpsilonExpand(Integer initState){
//
//        Set<Integer> rsl = new HashSet<>();
//        rsl.add(initState);
//        Queue<Integer> queue = new ArrayDeque<>();
//        queue.add(initState);
//
//        while (!queue.isEmpty()){
//            Integer state = queue.poll();
//            Map<Character, List<Integer>> outEdges = transGraph.get(state);
//            if (outEdges == null) continue; // 判断是否有出边
//            if (outEdges.containsKey(SpAlpha.EPSILON)) {
//                List<Integer> epsilonExpandStates = outEdges.get(SpAlpha.EPSILON);
//                for (Integer nextState : epsilonExpandStates) {
//                    if(!rsl.contains(nextState)){
//                        queue.add(nextState);
//                        rsl.add(nextState);
//                    }
//                }
//            }
//        }
//
//        return rsl;
//    }

    // 可能有可以优化的空间
    // 获得某状态集的epsilon闭包扩展，包括原状态集
    public Set<Integer> getEpsilonClosure(Set<Integer> set){

        Set<Integer> closure = new HashSet<>();
        closure.addAll(set);
//        for (Integer state : set) {
//            closure.addAll(getEpsilonExpand(state));
//        }
        Queue<Integer> queue = new ArrayDeque<>();
        queue.addAll(closure);

        while (!queue.isEmpty()){
            Integer state = queue.poll();
            Map<Character, List<Integer>> outEdges = transGraph.get(state);
            if (outEdges == null) continue; // 判断是否有出边
            if (outEdges.containsKey(SpAlpha.EPSILON)) {
                List<Integer> epsilonExpandStates = outEdges.get(SpAlpha.EPSILON);
                for (Integer nextState : epsilonExpandStates) {
                    if(!closure.contains(nextState)){
                        queue.add(nextState);
                        closure.add(nextState);
                    }
                }
            }
        }

        return closure;
    }

    // 获得某状态集经过字符c转移的直接转移后状态集合，还没有进行epsilon扩展
    private Set<Integer> moveOneStep(Set<Integer> set, char c){
        Set<Integer> nextStates = new HashSet<>(); // 转移后的状态，和原set可能有重叠，但并不是完全包含的关系

        for (Integer state : set) {
            Map<Character, List<Integer>> outEdges = transGraph.get(state); // 可能有不存在出边的状态（某个终态）
            if(outEdges == null) continue;
            if (outEdges.containsKey(c)) {
                List<Integer> expandStates = outEdges.get(c);
                nextStates.addAll(expandStates);
            }
            if(outEdges.containsKey(SpAlpha.ANY) && c != '\n'){ // ANY可以匹配除了\n外的所有字符
                List<Integer> expandStates = outEdges.get(SpAlpha.ANY);
                nextStates.addAll(expandStates);
            }
        }

        return nextStates;
    }

    // 获得某状态集经过字符c转移的直接转移后状态集合，并已经进行完全扩展
    // 如果c是SpAlpha.ANY的话，生成的DFA中ANY这条边代表不在字母表中的字符到达时，会走这条边，也就是其他字符会走这条边
    public Set<Integer> move(Set<Integer> set, char c){

        Set<Integer> nextStates = moveOneStep(set, c);

        long startTime = System.currentTimeMillis();
        Set<Integer> epsilonClosure = getEpsilonClosure(nextStates);
        long endTime = System.currentTimeMillis();

        NFA.timeDiff += endTime - startTime;

        return epsilonClosure;

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

    public NFA(){

    }
    // 构造一个原子NFA
    public NFA(char c, int startState, int endState){

        this.startState = startState;
        this.endStates.add(endState);
        this.states.add(startState);
        this.states.add(endState);
        this.alphabet.add(c);

        addEdge(this.startState, this.endStates, c);

    }

}

