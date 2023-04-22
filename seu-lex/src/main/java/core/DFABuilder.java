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

        long diff = 0;
        
        while(!queue.isEmpty()){
            Set<Integer> curNfaStates = queue.poll();
            int curDfaState = nfaToDfaStateMap.get(curNfaStates);
            
            for (Character c : nfa.getAlphabet()) {
                if(c == SpAlpha.EPSILON){ // 排除空串边
                    continue;
                }
                long startTime = System.currentTimeMillis();
                Set<Integer> nextNfaStates = nfa.move(curNfaStates, c);
                long endTime = System.currentTimeMillis();
                diff += endTime - startTime;
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
                
                // 添加dfa中的边
                int nextDfaState = nfaToDfaStateMap.get(nextNfaStates);
                Set<Integer> nextDfaStates = new HashSet<>();
                nextDfaStates.add(nextDfaState);
                dfa.addEdge(curDfaState, nextDfaStates, c);
            }
        }
        System.out.println("计算move总时间：" + diff + " 毫秒");
        System.out.println("计算epsilon-closure时间：" + NFA.timeDiff + " 毫秒");
        // 最后要根据 nfaToDfaStateMap 中key是否和nfa的endState有交集判断dfa的endState, 并判断该添加哪个动作
        Set<Integer> endStates = new HashSet<>();
        Map<Integer, LexAction> actionMap = new HashMap<>();

        for (Set<Integer> set : nfaToDfaStateMap.keySet()) {
            int dfaState = nfaToDfaStateMap.get(set);

            Set<Integer> intersect = new HashSet<>();
            intersect.addAll(set);
            intersect.retainAll(nfa.getEndStates());

            // 当前states和原nfa终态有交集，说明是终态
            if(intersect.size() > 0){
                endStates.add(dfaState);
                LexAction finalAction = new LexAction(nfa.getEndStates().size() + 1, "");
                for (Integer nfaEndState : intersect) {
                    LexAction lexAction = nfa.getActionMap().get(nfaEndState);
                    if(lexAction.getOrder() < finalAction.getOrder()){ // 选择order最小，即.l文件最靠前的动作
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

    // 最小化DFA，不考虑有ANY边的情况
    public static DFA minimizeDFA(DFA dfa){
        if(dfa.getAlphabet().contains(SpAlpha.ANY)){
            return dfa;
        }
        // 该DFA不含epsilon和ANY
        // 划分开始的两个集合
        Set<Integer> endStates = new HashSet<>();
        endStates.addAll(dfa.getEndStates());

        Set<Integer> nonEndStates = new HashSet<>();
        nonEndStates.addAll(dfa.getStates());
        nonEndStates.removeAll(dfa.getEndStates());

        Map<LexAction, Set<Integer>> actionToSet = new HashMap<>(); // 不同的action应该划分在不同的集合

        for (Integer endState : endStates) {
            LexAction lexAction = dfa.getActionMap().get(endState);
            if(!actionToSet.containsKey(lexAction)){
                actionToSet.put(lexAction, new HashSet<>());
            }
            actionToSet.get(lexAction).add(endState);
        }

        //将I0加入队列
        Queue<Set<Integer>> queue = new ArrayDeque<>();
        queue.add(nonEndStates);
        queue.addAll(actionToSet.values());
        // System.out.println(queue);

        // 最终得到的最小化状态集集合
        List<Set<Integer>> finalStates = new ArrayList<>();

        Map<Integer, Integer> stateToSetIndex = new HashMap<>();
        while(!queue.isEmpty()){

            // 得建立状态-状态集的映射
            int cnt = 0;

            for (Set<Integer> set : queue) {
                for(Integer i : set){
                    stateToSetIndex.put(i, cnt);
                }
                ++cnt;
            }
            for (Set<Integer> set : finalStates) {
                for(Integer i : set){
                    stateToSetIndex.put(i, cnt);
                }
                ++cnt;
            }

            int size = queue.size();

//            System.out.println("-------当前状态划分------");
//            for (Set<Integer> finalState : finalStates) {
//                System.out.println(finalState);
//            }

            for(int i = 0; i < size; ++i) {
                Set<Integer> subSet = queue.poll();
                int sz = subSet.size(); // 原来的子集一共有多少个状态

                //System.out.println(subSet);

                List<Set<Integer>> sets = new ArrayList<>();
                sets.add(subSet); // sets一开始是当前划分，但这个划分可能在经过字符遍历move后，一步步形成更多细小划分

                for (Character c : dfa.getAlphabet()) {

                    List<Set<Integer>> nextPartition = new ArrayList<>();
                    for (Set<Integer> set : sets) {
                        Map<Integer, Set<Integer>> indexToStates = new HashMap<>(); // 在现在的这个通过先前字符c划分出来的更小划分中，再继续进行划分
                        for (Integer state : set) {
                            Integer nextState = dfa.move(state, c);
                            // System.out.println(String.format("%d --%c--> %d", state, c, nextState));
                            if(nextState != -1){ // 非死状态，换成代表状态
                                nextState = stateToSetIndex.get(nextState);
                            }
                            if(!indexToStates.containsKey(nextState)){
                                indexToStates.put(nextState, new HashSet<>());
                            }
                            indexToStates.get(nextState).add(state);
                        }
                        nextPartition.addAll(indexToStates.values());
                    }
                    sets = nextPartition;

                    if(sets.size() == sz){ // 之前所有的集合被划分成一个个单独的状态，直接跳出循环
                        break;
                    }
                }


                if (sets.size() == 1) { // 如果没划分，就是最终状态了
                    finalStates.add(subSet);
                }
                else{ // 加回到原来的队列
                    for (Set<Integer> set : sets) {
                        queue.add(set);
                    }
                }
            }
        }

//        System.out.println("最终状态划分");
//        for (Set<Integer> finalState : finalStates) {
//            System.out.println(finalState);
//        }
        /**
         * 当划分子集确定完毕后，将子集的第一个状态作为该子集的代表状态，并建立子集-子集代表状态的映射
         * 如果子集有开始状态，则把开始状态作为代表，并设置newDfa的开始状态 （newState）
         * 把所有开始状态加到newDfa的states中 （states）
         * 其中在原dfa的endStates中作为endStates (endStates)
         * 将原终态对应的action加到actionMap中 (actionMap) 这个动作是否可能有优先级之分？暂时没考虑
         * 找每个（新的）代表状态在原来dfa的下一状态，得到某个原状态，并查找子集每个状态-子集代表map，获取新的代表状态
         */
        Map<Integer, Integer> oldToNewStateMap = new HashMap<>();

        DFA newDfa = new DFA();
        newDfa.getAlphabet().addAll(dfa.getAlphabet()); // 加入字母表

        for (Set<Integer> states : finalStates) {

            Integer delegate = states.iterator().next();
            if(states.contains(dfa.getStartState())){
                delegate = dfa.getStartState();
                newDfa.setStartState(delegate);
            }

            newDfa.addState(delegate);
            if(dfa.getEndStates().contains(delegate)){
                newDfa.getEndStates().add(delegate);
                newDfa.getActionMap().put(delegate, dfa.getActionMap().get(delegate));
            }

            for (Integer oldState : states) {
                oldToNewStateMap.put(oldState, delegate);
            }

        }

        Set<Integer> delegateStates = new HashSet<>();
        delegateStates.addAll(oldToNewStateMap.values());

        for (Integer delegate : delegateStates) {
            if(!dfa.getTransGraph().containsKey(delegate)){ // 是没出边的状态，跳过
                continue;
            }
            Map<Character, List<Integer>> outEdges = dfa.getTransGraph().get(delegate);
            for (Character c : outEdges.keySet()) {
                Integer nextState = outEdges.get(c).get(0);
                Integer delegateNextState = oldToNewStateMap.get(nextState);
                Set<Integer> set = new HashSet<>();
                set.add(delegateNextState);
                newDfa.addEdge(delegate, set, c);
            }
        }

        return newDfa;

    }

}
