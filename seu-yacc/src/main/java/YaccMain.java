import core.LR1Builder;
import core.YaccParser;
import dto.LR1;
import dto.ParseResult;

import java.io.File;

public class YaccMain {
    public static void main(String[] args) {

        System.out.println("--------seu-yacc--------");
        if(args.length == 0){
            System.out.println("Missing argument .y file");
        }
        else {
            File file = new File(args[0]);
            if (!file.canRead() || !file.isFile()) {
                System.out.println("Not a valid file");
                return;
            }
            System.out.println("Running...");
            ParseResult parseResult = YaccParser.getParseResult(file);
            LR1 lr1 = LR1Builder.buildLR1(parseResult);
            // LALR优化
            LR1 lalr = LR1Builder.buildLALRFromLR1(lr1); // 可选项


        }

    }
}
