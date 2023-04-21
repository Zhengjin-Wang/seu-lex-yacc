import constant.SpAlpha;
import core.LexParser;
import core.NFABuilder;
import dto.NFA;
import dto.ParseResult;
import org.junit.Test;
import utils.VisualizeUtils;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class NFATest {
    @Test
    public void addEdgeTest(){
        String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";
        NFA nfa = new NFA('b',1,2);
        nfa.addEdge(nfa.getStartState(), nfa.getEndStates(), SpAlpha.EPSILON);
        nfa.addEdge(nfa.getStartState(), nfa.getEndStates(), '[');
        nfa.addEdge(nfa.getStartState(), nfa.getEndStates(), ' ');
        VisualizeUtils.visualizeFA(nfa, workDir);
    }

    @Test
    public void addEpsilonTest(){
        String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";
        NFA nfa = new NFA('b',1,2);
        nfa.addEdge(nfa.getStartState(), nfa.getEndStates(), SpAlpha.EPSILON);
        VisualizeUtils.visualizeFA(nfa, workDir);
    }

    @Test
    public void getEpsilonClosureTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test.l");
        ParseResult parseResult = LexParser.getParseResult(file);
        NFA nfa = NFABuilder.buildNFA(parseResult);

        Set<Integer> startStates = new HashSet<>();
        startStates.add(nfa.getStartState());
        Set<Integer> epsilonClosure = nfa.getEpsilonClosure(startStates);
        System.out.println(epsilonClosure);
    }

    @Test
    public void moveOneStepTest(){
//        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test.l");
//        ParseResult parseResult = LexParser.getParseResult(file);
//        NFA nfa = NFABuilder.buildNFA(parseResult);
//
//        Set<Integer> startStates = new HashSet<>();
//        startStates.add(nfa.getStartState());
//        Set<Integer> epsilonClosure = nfa.getEpsilonClosure(startStates);
//        System.out.println(epsilonClosure);
//        Set<Integer> nextStates = nfa.moveOneStep(epsilonClosure, 'i');
//        System.out.println(nextStates);
    }

    @Test
    public void moveTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test.l");
        ParseResult parseResult = LexParser.getParseResult(file);
        NFA nfa = NFABuilder.buildNFA(parseResult);

        Set<Integer> startStates = new HashSet<>();
        startStates.add(nfa.getStartState());
        Set<Integer> epsilonClosure = nfa.getEpsilonClosure(startStates);
        System.out.println(epsilonClosure);
        Set<Integer> nextStates = nfa.move(epsilonClosure, 'i');
        System.out.println(nextStates);
    }


}
