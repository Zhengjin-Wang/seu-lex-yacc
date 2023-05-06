import core.CodeGenerator;
import core.LR1Builder;
import core.TableGenerator;
import core.YaccParser;
import dto.LR1;
import dto.ParseResult;
import utils.ExcelUtils;
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

            boolean visualize = false;
            boolean lalrModified = false;
            for (int i = 1; i < args.length; i++) {
                if(args[i].equals("-v")){
                    visualize = true;
                }
                if(args[i].equals("-lalr")){
                    lalrModified = true;
                }
            }

            ParseResult parseResult = YaccParser.getParseResult(file);

            // LALR优化
            LR1 lr;
            TableGenerator tableGenerator;
            if(lalrModified) {
                lr = LR1Builder.buildLALR(parseResult); // 可选项
                tableGenerator = new TableGenerator(lr, lr.getLalrTransGraph());
            }
            else{
                lr = LR1Builder.buildLR1(parseResult);
                tableGenerator = new TableGenerator(lr, lr.getTransGraph());
            }

            String yTabHCode = CodeGenerator.generateYTabH(lr);
            String yTabCCode = CodeGenerator.generateYTabC(parseResult, lr, tableGenerator);

            if(visualize) {
                if((!lalrModified && lr.getTransGraph().size() > 50) ||
                        (lalrModified && lr.getLalrTransGraph().size() > 50)){
                    System.out.println("lr分析状态数大于50，不进行可视化");
                }
                else {
                    VisualizeUtils.visualizeLR1(lr);
                    ExcelUtils.exportActionAndGotoTable(tableGenerator);
                }
            }

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
