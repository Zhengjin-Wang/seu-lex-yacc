package core;

import constant.SpAlpha;
import dto.DFA;
import dto.LexAction;
import dto.NFA;

import java.util.*;

public class DFABuilder {

    // 保证状态的唯一性，和NFABuilder类似的
    private static Integer stateCount = 0;

    private static Integer getNewState(){
        return stateCount++;
    }

    public static DFA buildDFA(NFA nfa){

        DFA dfa = new DFA();
        // 设置字母表
        for (Character alpha : nfa.getAlphabet()) {
            if(alpha != SpAlpha.EPSILON){
                dfa.getAlphabet().add(alpha); // 添加除epsilon以外的字符
            }
        }

        // 获取nfa开始状态集I0
        Set<Integer> nfaStartStates = new HashSet<>();
        nfaStartStates.add(nfa.getStartState());
        nfaStartStates = nfa.getEpsilonClosure(nfaStartStates);

        // 设置dfa开始状态
        int dfaStartState = getNewState();
        dfa.addState(dfaStartState);
        dfa.setStartState(dfaStartState);

        // 建立nfa状态集与dfa状态号的映射
        Map<Set<Integer>, Integer> nfaToDfaStateMap = new HashMap<>();
        nfaToDfaStateMap.put(nfaStartStates, dfaStartState);
        
        //将I0加入队列
        Queue<Set<Integer>> queue = new ArrayDeque<>();
        queue.add(nfaStartStates);
        
        while(!queue.isEmpty()){
            Set<Integer> curNfaStates = queue.poll();
            int curDfaState = nfaToDfaStateMap.get(curNfaStates);
            
            for (Character c : nfa.getAlphabet()) {
                if(c == SpAlpha.EPSILON){ // 排除空串边
                    continue;
                }
                Set<Integer> nextNfaStates = nfa.move(curNfaStates, c);
                if(nextNfaStates.size() == 0){ // 无法转移，跳过
                    continue;
                }
                if(!nfaToDfaStateMap.containsKey(nextNfaStates)){
                    // 一个新的状态，加入dfa，nfa->dfa映射，队列
                    int newDfaState = getNewState();
                    dfa.addState(newDfaState);
                    nfaToDfaStateMap.put(nextNfaStates, newDfaState);
                    queue.add(nextNfaStates);
                }
                
                // 添加边
                int nextDfaState = nfaToDfaStateMap.get(nextNfaStates);
                Set<Integer> nextDfaStates = new HashSet<>();
                nextDfaStates.add(nextDfaState);
                dfa.addEdge(curDfaState, nextDfaStates, c);
            }
        }
        
        // 最后要根据 nfaToDfaStateMap 中key是否和nfa的endState有交集判断dfa的endState, 并判断该添加哪个动作
        Set<Integer> endStates = new HashSet<>();
        Map<Integer, LexAction> actionMap = new HashMap<>();

        for (Set<Integer> set : nfaToDfaStateMap.keySet()) {
            int dfaState = nfaToDfaStateMap.get(set);

            Set<Integer> intersect = new HashSet<>();
            intersect.addAll(set);
            intersect.retainAll(nfa.getEndStates());

            // 是终态
            if(intersect.size() > 0){
                endStates.add(dfaState);
                LexAction finalAction = new LexAction(nfa.getEndStates().size() + 1, "");
                for (Integer nfaEndState : intersect) {
                    LexAction lexAction = nfa.getActionMap().get(nfaEndState);
                    if(lexAction.getOrder() < finalAction.getOrder()){
                        finalAction = lexAction;
                    }
                }
                actionMap.put(dfaState, finalAction);
            }
        }

        dfa.setEndStates(endStates);
        dfa.setActionMap(actionMap);

        return dfa;
    }

}
