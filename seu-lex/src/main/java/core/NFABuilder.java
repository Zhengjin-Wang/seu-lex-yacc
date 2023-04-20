package core;

import constant.SpAlpha;
import dto.LexAction;
import dto.NFA;
import dto.ParseResult;
import dto.Regex;

import java.util.*;

public class NFABuilder {

    // 保证状态的唯一性
    private static Integer stateCount = 0;

    private static Integer getNewState(){
        return stateCount++;
    }

    public static NFA createAtomNFA(char c){
        int startState = getNewState();
        int endState = getNewState();
        return new NFA(c, startState, endState);
    }

    // 进行深拷贝，状态号需要对应增加
    public static NFA copyNFA(NFA oldNfa){

        Map<Integer, Integer> oldToNewMap = new HashMap<>();
        for (Integer oldState : oldNfa.getStates()) {
            oldToNewMap.put(oldState, getNewState());
        }
        NFA nfa = new NFA();

        nfa.setStartState(oldToNewMap.get(oldNfa.getStartState()));
        for (Integer endState : oldNfa.getEndStates()) {
            nfa.getEndStates().add(oldToNewMap.get(endState));
        }
        for (Integer state : oldNfa.getStates()) {
            nfa.getStates().add(oldToNewMap.get(state));
        }
        for (Character character : oldNfa.getAlphabet()) {
            nfa.getAlphabet().add(character);
        }

        for (Integer begin : oldNfa.getTransGraph().keySet()) {
            Map<Character, List<Integer>> outEdges = oldNfa.getTransGraph().get(begin);
            for (Character c : outEdges.keySet()) {
                Set<Integer> newEndSet = new HashSet<>();
                for (Integer end : outEdges.get(c)) {
                    newEndSet.add(oldToNewMap.get(end));
                }
                nfa.addEdge(oldToNewMap.get(begin), newEndSet, c);
            }
        }

        for (Integer endSate : oldNfa.getActionMap().keySet()) {
            LexAction lexAction = oldNfa.getActionMap().get(endSate);
            nfa.getActionMap().put(oldToNewMap.get(endSate), lexAction);
        }

        return nfa;
    }


    /**
     * 将输入NFA做Kleene闭包（星闭包），见龙书3.7.1节图3-34
     * ```
     *      ________________ε_______________
     *     |                                ↓
     * 新开始 -ε-> 旧开始 --...--> 旧接收 -ε-> 新接收
     *              ↑______ε______|
     * ```
     * 返回的是原来的对象
     */
    public static NFA kleene(NFA nfa){

        int newStart = getNewState();
        int newEnd = getNewState();

        //添加新状态，还未连边和设置新开始接收
        nfa.addState(newStart);
        nfa.addState(newEnd);

        Set<Integer> oldStartState = new HashSet<>();
        oldStartState.add(nfa.getStartState());
        Set<Integer> newEndState = new HashSet<>();
        newEndState.add(newEnd);
        for (Integer endState : nfa.getEndStates()) {
            nfa.addEdge(endState, oldStartState, SpAlpha.EPSILON); // 旧接收->旧开始
            nfa.addEdge(endState, newEndState, SpAlpha.EPSILON); // 旧接收->新接收
        }
        nfa.addEdge(newStart, oldStartState, SpAlpha.EPSILON); // 新开始->旧开始

        // 重置开始和接收状态
        nfa.setStartState(newStart);
        nfa.setEndStates(newEndState);

        nfa.addEdge(newStart, newEndState, SpAlpha.EPSILON); // 新开始->旧开始

        // 在这个时候，还在构建一个lex表达式的NFA，不涉及到终态动作，build完一个singleNFA再添加动作

        return nfa;
    }

    /**
     * 串联两个NFA，丢弃所有动作
     * ```
     * NFA1 --epsilon--> NFA2
     * ```
     */
    public static NFA serial(NFA a, NFA b){
        NFA nfa = new NFA();

        //先合并alphabet, states 和 transGraph（这时是两个不连通的图）
        Set<Character> newAlphabet = a.getAlphabet();
        newAlphabet.addAll(b.getAlphabet());

        Set<Integer> newStates = a.getStates();
        newStates.addAll(b.getStates());

        Map<Integer, Map<Character, List<Integer>>> newTransGraph = a.getTransGraph();
        newTransGraph.putAll(b.getTransGraph());

        nfa.setAlphabet(newAlphabet);
        nfa.setStates(newStates);
        nfa.setTransGraph(newTransGraph);

        Set<Integer> bStartState = new HashSet<>();
        bStartState.add(b.getStartState());

        // a终->b起
        for (Integer endState : a.getEndStates()) {
            nfa.addEdge(endState, bStartState, SpAlpha.EPSILON);
        }

        nfa.setStartState(a.getStartState());
        nfa.setEndStates(b.getEndStates());

        return nfa;
    }

    /**
     * 并联两个NFA（对应于|或运算），收束尾部，丢弃所有动作
     * ```
     *             ε  NFA1  ε
     * new_start <             > new_accept
     *             ε  NFA2  ε
     * ```
     */
    public static NFA parallel(NFA a, NFA b){
        NFA nfa = new NFA();

        //先合并alphabet, states 和 transGraph（这时是两个不连通的图）
        Set<Character> newAlphabet = a.getAlphabet();
        newAlphabet.addAll(b.getAlphabet());

        Set<Integer> newStates = a.getStates();
        newStates.addAll(b.getStates());

        Map<Integer, Map<Character, List<Integer>>> newTransGraph = a.getTransGraph();
        newTransGraph.putAll(b.getTransGraph());

        nfa.setAlphabet(newAlphabet);
        nfa.setStates(newStates);
        nfa.setTransGraph(newTransGraph);

        // 合并两个nfa的旧开始状态和结束状态
        Set<Integer> oldStartState = new HashSet<>();
        oldStartState.add(a.getStartState());
        oldStartState.add(b.getStartState());

        Set<Integer> oldEndState = a.getEndStates();
        oldEndState.addAll(b.getEndStates());

        int newStart = getNewState();
        int newEnd = getNewState();
        Set<Integer> newEndState = new HashSet<>();
        newEndState.add(newEnd);

        // 新起->旧起
        nfa.addEdge(newStart, oldStartState, SpAlpha.EPSILON);

        // 旧终->新终
        for (Integer endState : oldEndState) {
            nfa.addEdge(endState, newEndState, SpAlpha.EPSILON);
        }

        nfa.setStartState(newStart);
        nfa.setEndStates(newEndState);

        return nfa;
    }

    /**
     *
     * @param regex 用到的是postFix
     * @param lexAction 就是C代码，到达接受态时执行C代码就行，可能要把C代码复制到特定的地方
     * @return
     */
    public static NFA buildSingleNFA(Regex regex, LexAction lexAction){

        String postFix = regex.getPostFix();
        Stack<NFA> nfaStack = new Stack<>();
        boolean slash = false;
        /*
            在处理过程中，要考虑几点:
            1. SpAlpha.ANY意味着任意字符可以转移状态 （在转regex的时候已经把非转义的.替换为SpAlpha.ANY
            2. 遇到\直接把下一个字符视作常规字符，创建新NFA加入状态栈中
         */
        for(int i = 0; i < postFix.length(); ++i){
            char c = postFix.charAt(i);
            if(slash){
                if(c == 'n'){
                    c = '\n';
                }
                else if(c == 'r'){
                    c = '\r';
                }
                else if(c == 't'){
                    c = '\t';
                }
                NFA nfa = createAtomNFA(c);
                nfaStack.push(nfa);
                slash = false;
                continue;
            }
            if(c == '\\'){
                slash = true;
            }
            else if(!SpAlpha.isPostfixOp(c)){
                NFA nfa = createAtomNFA(c);
                nfaStack.push(nfa);
            }
            else{ // 是操作符
                if(c == '|'){
                    NFA a = nfaStack.pop();
                    NFA b = nfaStack.pop();
                    NFA nfa = parallel(a, b);
                    nfaStack.push(nfa);
                }
                else if(c == SpAlpha.CONCAT){
                    NFA a = nfaStack.pop();
                    NFA b = nfaStack.pop();
                    NFA nfa = serial(b, a); // b在a的前边，应该是b->a
                    nfaStack.push(nfa);
                }
                else if(c == '*'){
                    NFA nfa = nfaStack.pop();
                    nfa = kleene(nfa);
                    nfaStack.push(nfa);
                }
                else if(c == '+'){
                    NFA nfa = nfaStack.pop();
                    NFA nfaKleene = kleene(copyNFA(nfa));
                    nfa = serial(nfa, nfaKleene);
                    nfaStack.push(nfa);
                }
                else if(c == '?'){
                    NFA nfa = nfaStack.pop();
                    nfa.addEdge(nfa.getStartState(), nfa.getEndStates(), SpAlpha.EPSILON);
                    nfaStack.push(nfa);
                }
            }
        }

        NFA nfa = nfaStack.pop();

        // 这个时候要设置动作了
        for (Integer endState : nfa.getEndStates()) {
            nfa.getActionMap().put(endState, lexAction);
        }

        //表达式正确的情况下，这个地方应该只有一个元素
        return nfa;
    }

    /**
     * 最后并联所有NFA
     * ```
     *             ε  NFA1
     * new_start <-   ...
     *             ε  NFAn
     * ```
     * @param nfas
     * @return
     */
    private static NFA parallelAll(List<NFA> nfas){

        return new NFA();
    }

    public static NFA buildNFA(ParseResult parseResult){

        Map<String, String> regexAction = parseResult.getRegexAction();
        List<NFA> nfas = new ArrayList<>();
        int orderCnt = 0;

        for(Map.Entry<String, String> entry: regexAction.entrySet()){
            String rawRegex = entry.getKey();
            String action = entry.getValue();

            Regex regex = new Regex(rawRegex);
            LexAction lexAction = new LexAction(orderCnt, action);
            NFA nfa = buildSingleNFA(regex, lexAction);

            nfas.add(nfa);
            ++orderCnt;
        }


        NFA finalNFA = parallelAll(nfas);

        return finalNFA;
    }

}
