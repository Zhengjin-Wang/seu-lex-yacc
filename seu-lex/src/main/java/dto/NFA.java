package dto;

import constant.SpAlpha;
import lombok.Data;

import java.util.*;

/**
 * 根据LexParser获取到的ParseResult生成的NFA
 */
@Data
public class NFA { // 正则表达式转NFA时，遇到转义字符\，当作一个操作符，可以操作[ \（lex操作符），也可以操作n r t（用作转义），如果是其他普通字符，则保持原字符
                    // 只有遇到n r t才替换为对应转义后字符，其他就保留原字符
                    // 当某条边上的字符是SpAlpha.ANY，表明任何读入的字符都能被接收，转移到下一状态


    private int startState = -1; // 初态
    private List<Integer> endStates = new ArrayList<>(); // 终态（接受态），会有多个
    private Set<Integer> states = new HashSet<>();  // 所有状态
    private Set<Character> alphabet = new HashSet<>(); // 所有字符（边），包括ε
    private Map<Integer, Map<Character, List<Integer>>> transGraph = new HashMap<>(); // 状态转移图，邻接表形式,当某条边上的字符是SpAlpha.ANY，表明任何读入的字符都能被接收，转移到下一状态
    private Map<Integer, String> actionMap; // 终态对应的动作

    public boolean isAtom() { // 是否只有一个字符，如果是这样，合并时可以直接将字符串联或并联到另一个NFA上（加边
        return states.size() == 2 && alphabet.size() == 1;
    }

    // 需要保证所有状态在集合中
    public void addEdge(int beginState, List<Integer> endS, char c){

        if(!transGraph.containsKey(beginState)){
            transGraph.put(beginState, new HashMap<>());
        }
        Map<Character, List<Integer>> outEdges = transGraph.get(beginState);
        if(!outEdges.containsKey(c)){
            outEdges.put(c, new ArrayList<>());
        }
        List<Integer> reachStates = outEdges.get(c);

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

