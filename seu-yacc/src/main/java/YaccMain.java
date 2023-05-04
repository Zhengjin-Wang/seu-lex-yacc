import core.CodeGenerator;
import core.LR1Builder;
import core.YaccParser;
import dto.LR1;
import dto.ParseResult;
import utils.VisualizeUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

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

            // LALR优化
            LR1 lalr = LR1Builder.buildLALR(parseResult); // 可选项
            String yTabHCode = CodeGenerator.generateYTabH(lalr);
            String yTabCCode = CodeGenerator.generateYTabC(parseResult, lalr, lalr.getLalrTransGraph());
//            VisualizeUtils.visualizeLR1(lalr);

//            LR1 lr1 = LR1Builder.buildLR1(parseResult);
//            String yTabHCode = CodeGenerator.generateYTabH(lr1);
//            String yTabCCode = CodeGenerator.generateYTabC(parseResult, lr1, lr1.getTransGraph());
//            VisualizeUtils.visualizeLR1(lr1);

            File yTabHFile = new File("y.tab.h");
            try{
                FileWriter fileWriter = new FileWriter(yTabHFile);
                fileWriter.write(yTabHCode);
                fileWriter.flush();
                fileWriter.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }

            File yTabCFile = new File("y.tab.c");
            try{
                FileWriter fileWriter = new FileWriter(yTabCFile);
                fileWriter.write(yTabCCode);
                fileWriter.flush();
                fileWriter.close();
            }
            catch (IOException e){
                e.printStackTrace();
            }

        }

    }
}
