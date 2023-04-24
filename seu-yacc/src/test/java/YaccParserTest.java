import core.YaccParser;
import dto.ParseResult;
import org.junit.Test;

import java.io.File;

public class YaccParserTest {

    @Test
    public void ParserTest(){
        File file = new File("C:\\Users\\Lilac\\Desktop\\新建文件夹\\minic.y");
        ParseResult parseResult = YaccParser.getParseResult(file);
    }

}
