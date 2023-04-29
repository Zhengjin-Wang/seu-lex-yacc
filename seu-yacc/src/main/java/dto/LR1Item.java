package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * LR1State 中的一行就代表一个item，包括kernel
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LR1Item {

    private int productionId; // 对应产生式编号
    private int dotPos = 1; // 点的位置，在右部第几个元素之前，从1开始，因为产生式是包含左部的
    private int predictSymbol; // 预测符（一定是终结符）的编号，不同预测符同一kernel在不同的item里，方便比较

    public List<Integer> getProductionFromLR1(LR1 lr1){
        return lr1.getProductionIdToProduction().get(this.productionId);
    }

    public Integer getCurrentSymbolFromLR1(LR1 lr1){
        if(isReducible(lr1)){
            throw new RuntimeException("Try to get next symbol from a reducible item");
        }
        List<Integer> production = getProductionFromLR1(lr1);
        return production.get(dotPos);
    }

    // 点符号移动到产生式最后，说明可规约，就不会再内扩展新项目，也不会再外扩展到新状态的某一项
    public boolean isReducible(LR1 lr1){
        return dotPos == getProductionFromLR1(lr1).size();
    }

    @Override
    public boolean equals(Object obj){
        LR1Item item = (LR1Item) obj;
        return item.dotPos == this.dotPos && item.productionId == this.productionId && item.predictSymbol == this.predictSymbol;
    }

}
