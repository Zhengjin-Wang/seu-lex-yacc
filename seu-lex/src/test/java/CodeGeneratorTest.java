import core.CodeGenerator;
import core.DFABuilder;
import core.LexParser;
import core.NFABuilder;
import dto.DFA;
import dto.NFA;
import dto.ParseResult;
import org.junit.Test;
import utils.VisualizeUtils;

import java.io.File;

public class CodeGeneratorTest {

    public static final String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";

    @Test
    public void generateStringTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test.l");
        ParseResult parseResult = LexParser.getParseResult(file);
        NFA nfa = NFABuilder.buildNFA(parseResult);
        VisualizeUtils.visualizeFA(nfa, workDir, "nfa");
        System.out.println("----------------------nfa----------------------");
        for (Integer i : nfa.getActionMap().keySet()) {
            System.out.println(i + "状态，action: " + nfa.getActionMap().get(i));
        }

        DFA dfa = DFABuilder.buildDFA(nfa);
        VisualizeUtils.visualizeFA(dfa, workDir, "dfa");
        System.out.println("----------------------dfa----------------------");
        for (Integer i : dfa.getActionMap().keySet()) {
            System.out.println(i + "状态，action: " + dfa.getActionMap().get(i));
        }

        DFA minDFA = DFABuilder.minimizeDFA(dfa);
        VisualizeUtils.visualizeFA(minDFA, workDir, "min_dfa");
        System.out.println("----------------------min dfa----------------------");
        for (Integer i : minDFA.getActionMap().keySet()) {
            System.out.println(i + "状态，action: " + minDFA.getActionMap().get(i));
        }

        dfa = minDFA;

        System.out.println(CodeGenerator.generateCode(parseResult, dfa));


    }
}
