package utils;

import core.TableGenerator;
import dto.LR1;
import jxl.Workbook;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ExcelUtils {

    private static void addCell(WritableSheet sheet, int row, int col, String content){
        Label label = new Label(col, row, content);
        try {
            sheet.addCell(label);
        } catch (WriteException e) {
            e.printStackTrace();
        }
    }

    public static void exportActionAndGotoTable(TableGenerator tableGenerator, String exportFileName){

        // 创建Excel文件对象
        File file = new File(exportFileName);
        WritableWorkbook workbook = null;
        try {
            workbook = Workbook.createWorkbook(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 创建工作表对象
        WritableSheet sheet = workbook.createSheet("action_goto_table", 0);
        LR1 lr1 = tableGenerator.getLr1();

        addCell(sheet, 0, 0, "pid");
        addCell(sheet, 0, 1, "production");

        for (Integer pid : lr1.getProductionIdToProduction().keySet()) {
            addCell(sheet, pid, 0, pid.toString());

            List<Integer> production = lr1.getProductionIdToProduction().get(pid);
            StringBuilder productionBuilder = new StringBuilder();

            int left = production.get(0);
            productionBuilder.append(lr1.getNumberToSymbol().get(left) + " -> ");
            for (int i = 1; i < production.size(); i++) {
                int symbol = production.get(i);
                productionBuilder.append(lr1.getNumberToSymbol().get(symbol) + " ");
            }

            addCell(sheet, pid, 1, productionBuilder.toString());
        }

        int colHeaderShift = 3;
        int actionColShift = 4;
        int gotoColShift = 4;
        int rowShift = 0;

        int col = tableGenerator.getActionTable()[0].length;

//        addCell(sheet, 0, actionColShift, "action");

        for (int i = 0; i < col; i++) {
            if(lr1.getOccurredSymbols().contains(i)){
                String symbolName = lr1.getNumberToSymbol().get(i);
                addCell(sheet, rowShift, gotoColShift++, symbolName);
            }
        }

//        addCell(sheet, 0, gotoColShift, "goto");

        col = tableGenerator.getNonTerminalCount();
        for (int i = 0; i < col; i++) {
            Integer symbolId = tableGenerator.convertGotoTableColumnIndexToSymbolId(i);
            String symbolName = lr1.getNumberToSymbol().get(symbolId);
            addCell(sheet, rowShift, gotoColShift + i, symbolName);
        }


        int row = 0;
        ++rowShift;
        for (int[] ints : tableGenerator.getActionTable()) {

            int state = tableGenerator.getTableRowIndexToStateId().get(row); // lalr状态是离散的
            addCell(sheet, rowShift + row, colHeaderShift, "state: " + state);

            int j = 0; // j用于枚举终结符
            int excelCol = 0;
            for (int action : ints) {

                if(lr1.getOccurredSymbols().contains(j)) { //只有在出现符号里的j才会被显示，j并非显示的第几列
                    if(action > 0){ // shift然后转移到下一个状态，要转换，也是lalr原因
                        action = tableGenerator.getTableRowIndexToStateId().get(action);
                    }
                    if(action != 0) {
                        String actionString = String.valueOf(action);
                        if(action == -1){
                            actionString = actionString + "(acc)";
                        }
                        addCell(sheet, rowShift + row, actionColShift + excelCol, actionString);
                    }
                    ++excelCol;
                }
                ++j;
            }
            for (int nextState : tableGenerator.getGotoTable()[row]) {
                if(nextState > 0){ // == 0表示出错
                    nextState = tableGenerator.getTableRowIndexToStateId().get(nextState);
                }
                if (nextState != 0) {
                    String gotoString = String.valueOf(nextState);
                    addCell(sheet, rowShift + row, actionColShift + excelCol, gotoString);
                }
                ++excelCol;
            }

            ++row;
        }


        try {
            workbook.write();
            workbook.close();
        } catch (IOException | WriteException e) {
            e.printStackTrace();
        }

    }

    public static void exportActionAndGotoTable(TableGenerator tableGenerator){
        exportActionAndGotoTable(tableGenerator, "action_goto_table.xls");
    }

}
