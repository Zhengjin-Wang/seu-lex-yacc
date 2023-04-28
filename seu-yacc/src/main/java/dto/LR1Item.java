package dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

/**
 * LR1State 中的一行就代表一个item，包括kernel
 */
@Data
@AllArgsConstructor
public class LR1Item {

    private int productionNumber; // 对应产生式编号
    private int dotPos = 0; // 点的位置，在右部第几个元素之前，从0开始
    private int predictSymbol; // 预测符（一定是终结符）的编号，不同预测符同一kernel在不同的item里，方便比较

    @Override
    public boolean equals(Object obj){
        LR1Item item = (LR1Item) obj;
        return item.dotPos == this.dotPos && item.productionNumber == this.productionNumber && item.predictSymbol == this.predictSymbol;
    }

}
