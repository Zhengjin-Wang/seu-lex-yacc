package dto;


import java.util.*;

public interface FA {
    int getStartState(); // 初态
    Set<Integer> getEndStates(); // 终态（接受态），会有多个
    Set<Integer> getStates();  // 所有状态
    Set<Character> getAlphabet(); // 所有字符（边），包括ε
    Map<Integer, Map<Character, List<Integer>>> getTransGraph(); // 状态转移图，邻接表形式,当某条边上的字符是SpAlpha.ANY，表明任何读入的字符都能被接收，转移到下一状态
    Map<Integer, LexAction> getActionMap(); // 终态对应的动作
}
