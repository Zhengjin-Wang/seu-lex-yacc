import constant.SpAlpha;
import dto.NFA;
import org.junit.Test;
import utils.VisualizeUtils;

public class NFATest {
    @Test
    public void addEdgeTest(){
        String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";
        NFA nfa = new NFA('b',1,2);
        nfa.addEdge(nfa.getStartState(), nfa.getEndStates(), SpAlpha.EPSILON);
        nfa.addEdge(nfa.getStartState(), nfa.getEndStates(), '[');
        nfa.addEdge(nfa.getStartState(), nfa.getEndStates(), ' ');
        VisualizeUtils.visualizeNFA(nfa, workDir);
    }

    @Test
    public void addEpsilonTest(){
        String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";
        NFA nfa = new NFA('b',1,2);
        nfa.addEdge(nfa.getStartState(), nfa.getEndStates(), SpAlpha.EPSILON);
        VisualizeUtils.visualizeNFA(nfa, workDir);
    }


}
