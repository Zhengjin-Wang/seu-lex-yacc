import core.CodeGenerator;
import core.LR1Builder;
import core.YaccParser;
import dto.LR1;
import dto.ParseResult;
import org.junit.Test;

import java.io.File;

public class CodeGeneratorTest {

    @Test
    public void generateYTabHTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\minic.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = LR1Builder.buildLR1(parseResult);
//        LR1 lalr = LR1Builder.buildLALRFromLR1(lr1);

        System.out.println(CodeGenerator.generateYTabH(lr1));

    }

}
