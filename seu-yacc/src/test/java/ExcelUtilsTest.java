import core.LR1Builder;
import core.TableGenerator;
import core.YaccParser;
import dto.LR1;
import dto.ParseResult;
import org.junit.Test;
import utils.ExcelUtils;

import java.io.File;

public class ExcelUtilsTest {

    @Test
    public void exportActionAndGotoTableTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\test2.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
        LR1 lr1 = LR1Builder.buildLR1(parseResult);
        TableGenerator tableGenerator = new TableGenerator(lr1, lr1.getTransGraph());
        ExcelUtils.exportActionAndGotoTable(tableGenerator);
    }
}
