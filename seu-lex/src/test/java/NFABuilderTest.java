import core.LexParser;
import core.NFABuilder;
import dto.LexAction;
import dto.NFA;
import dto.ParseResult;
import dto.Regex;
import org.junit.Test;
import utils.VisualizeUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NFABuilderTest {

    public static final String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";

    @Test
    public void createAtomNFATest() {
        NFA a = NFABuilder.createAtomNFA('a');
        VisualizeUtils.visualizeFA(a, workDir);
    }

    @Test
    public void copyNFATest() {
        NFA a = NFABuilder.createAtomNFA('a');
        a.addEdge(0, a.getEndStates(), 'b');
        NFA b = NFABuilder.copyNFA(a);
        b.addEdge(b.getStartState(),b.getEndStates(),'e');
        VisualizeUtils.visualizeFA(a, workDir, "a");
        VisualizeUtils.visualizeFA(b, workDir, "b");

    }

    @Test
    public void kleeneTest() {
        NFA a = NFABuilder.createAtomNFA('a');
        a = NFABuilder.kleene(a);
        VisualizeUtils.visualizeFA(a, workDir);
    }

    @Test
    public void serialTest() {
        NFA a = NFABuilder.createAtomNFA('a');
        a = NFABuilder.kleene(a);
        NFA b = NFABuilder.createAtomNFA('b');
        NFA c = NFABuilder.serial(a, b);
        NFA d = NFABuilder.copyNFA(c);
        NFA e = NFABuilder.serial(c,d);
        VisualizeUtils.visualizeFA(e, workDir);
    }

    @Test
    public void parallelTest() {
        NFA a = NFABuilder.createAtomNFA('a');
        a = NFABuilder.kleene(a);
        NFA b = NFABuilder.createAtomNFA('b');
        NFA c = NFABuilder.parallel(a, b);
        // VisualizeUtils.visualizeFA(c, workDir);
        NFA d = NFABuilder.copyNFA(c);
        NFA e = NFABuilder.parallel(c,d);
        VisualizeUtils.visualizeFA(e, workDir);
    }

    @Test
    public void buildSingleNFATest(){
        String s = "ab|c";
        Regex regex = new Regex(s);
        LexAction lexAction = new LexAction(1, "printf();");
        NFA nfa = NFABuilder.buildSingleNFA(regex, lexAction);
        VisualizeUtils.visualizeFA(nfa, workDir);
        for (Integer i : nfa.getActionMap().keySet()) {
            System.out.println(i + "状态，action: " + nfa.getActionMap().get(i));
        }
    }

    @Test
    public void parallelAllTest(){

        int ord = 0;
        List<NFA> nfas = new ArrayList<>();

        String[] strs = new String[]{"ab|c", "de", "c+", "a?"};

        for (String str : strs) {
            Regex regex = new Regex(str);
            LexAction lexAction = new LexAction(ord++, "printf(" + ord + ");");
            NFA nfa = NFABuilder.buildSingleNFA(regex, lexAction);
            nfas.add(nfa);
        }

        NFA nfa = NFABuilder.parallelAll(nfas);

        VisualizeUtils.visualizeFA(nfa, workDir);
        for (Integer i : nfa.getActionMap().keySet()) {
            System.out.println(i + "状态，action: " + nfa.getActionMap().get(i));
        }
    }

    @Test
    public void buildNFATest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test.l");
        ParseResult parseResult = LexParser.getParseResult(file);
        NFA nfa = NFABuilder.buildNFA(parseResult);
        VisualizeUtils.visualizeFA(nfa, workDir);
        for (Integer i : nfa.getActionMap().keySet()) {
            System.out.println(i + "状态，action: " + nfa.getActionMap().get(i));
        }
    }

    @Test
    public void algorithmNFATest(){
        String s = "c(.+|e)b";
        Regex regex = new Regex(s);
        LexAction lexAction = new LexAction(1, "printf();");
        NFA nfa = NFABuilder.buildSingleNFA(regex, lexAction);
        VisualizeUtils.visualizeFA(nfa, workDir);

        Set<Integer> startStates = new HashSet<>();
        startStates.add(nfa.getStartState());
        Set<Integer> epsilonClosure = nfa.getEpsilonClosure(startStates);
        Set<Integer> next = nfa.move(epsilonClosure, 'c');
        //System.out.println(next);
        Set<Integer> middle = nfa.move(next, 'e');
        System.out.println(nfa.move(middle, 'z'));
    }

}
