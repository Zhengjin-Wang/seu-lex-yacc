import dto.NFA;
import org.junit.Test;
import utils.VisualizeUtils;

import java.util.UUID;

public class VisualizeUtilsTest {
    @Test
    public void stringTest(){
        String dotFileName = "a.dot";
        String pngFileName = UUID.randomUUID().toString() + "NFA.png";
        System.out.println(String.format("dot -Tpng %s -o %s", dotFileName, pngFileName));
    }

    @Test
    public void visualizerTest(){
        String workDir = "D:\\SEU DOCUMENTS\\编译原理实践\\graphviz";
        NFA nfa = new NFA('b',2,6);
        VisualizeUtils.visualizeFA(nfa, workDir);
    }
}
