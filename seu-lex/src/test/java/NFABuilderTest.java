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

public class NFABuilderTest {

    public static final String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";

    @Test
    public void createAtomNFATest() {
        NFA a = NFABuilder.createAtomNFA('a');
        VisualizeUtils.visualizeNFA(a, workDir);
    }

    @Test
    public void copyNFATest() {
        NFA a = NFABuilder.createAtomNFA('a');
        a.addEdge(0, a.getEndStates(), 'b');
        NFA b = NFABuilder.copyNFA(a);
        b.addEdge(b.getStartState(),b.getEndStates(),'e');
        VisualizeUtils.visualizeNFA(a, workDir, "a");
        VisualizeUtils.visualizeNFA(b, workDir, "b");

    }

    @Test
    public void kleeneTest() {
        NFA a = NFABuilder.createAtomNFA('a');
        a = NFABuilder.kleene(a);
        VisualizeUtils.visualizeNFA(a, workDir);
    }

    @Test
    public void serialTest() {
        NFA a = NFABuilder.createAtomNFA('a');
        a = NFABuilder.kleene(a);
        NFA b = NFABuilder.createAtomNFA('b');
        NFA c = NFABuilder.serial(a, b);
        NFA d = NFABuilder.copyNFA(c);
        NFA e = NFABuilder.serial(c,d);
        VisualizeUtils.visualizeNFA(e, workDir);
    }

    @Test
    public void parallelTest() {
        NFA a = NFABuilder.createAtomNFA('a');
        a = NFABuilder.kleene(a);
        NFA b = NFABuilder.createAtomNFA('b');
        NFA c = NFABuilder.parallel(a, b);
        // VisualizeUtils.visualizeNFA(c, workDir);
        NFA d = NFABuilder.copyNFA(c);
        NFA e = NFABuilder.parallel(c,d);
        VisualizeUtils.visualizeNFA(e, workDir);
    }

    @Test
    public void buildSingleNFATest(){
        String s = "ab|c";
        Regex regex = new Regex(s);
        LexAction lexAction = new LexAction(1, "printf();");
        NFA nfa = NFABuilder.buildSingleNFA(regex, lexAction);
        VisualizeUtils.visualizeNFA(nfa, workDir);
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

        VisualizeUtils.visualizeNFA(nfa, workDir);
        for (Integer i : nfa.getActionMap().keySet()) {
            System.out.println(i + "状态，action: " + nfa.getActionMap().get(i));
        }
    }

    @Test
    public void buildNFATest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test.l");
        ParseResult parseResult = LexParser.getParseResult(file);
        NFA nfa = NFABuilder.buildNFA(parseResult);
        VisualizeUtils.visualizeNFA(nfa, workDir);
        for (Integer i : nfa.getActionMap().keySet()) {
            System.out.println(i + "状态，action: " + nfa.getActionMap().get(i));
        }
    }


}
