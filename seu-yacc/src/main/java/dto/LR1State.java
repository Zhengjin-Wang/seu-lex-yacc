package dto;


import lombok.Data;

import java.util.*;

/**
 * LR1的DFA中的一个状态
 *
 */
@Data
public class LR1State {

    private Integer stateId;
    private Map<Integer, LR1State> edges = new HashMap<>(); // --symbol编号 -> 对应下一个状态
    private List<LR1Item> items = new ArrayList<>(); // 所有的item

    public void sortItems(){
        Collections.sort(this.items, new Comparator<LR1Item>() {
            @Override
            public int compare(LR1Item o1, LR1Item o2) {
                if (o1.equals(o2)) return 0;
                boolean b =  o1.getProductionId() == o2.getProductionId() ?
                        ( o1.getDotPos() == o2.getDotPos() ?
                                o1.getPredictSymbol() < o2.getPredictSymbol()
                                :o1.getDotPos() < o2.getDotPos() ):
                        o1.getProductionId() < o2.getProductionId();
                return b?-1:1;
            }
        });
    }

    // 不知道可不可以优化
    public boolean equalItems(LR1State lr1State) {

        if(items.size() != lr1State.items.size()){
            return false;
        }
//        lr1State.sortItems();
//        this.sortItems();
//
//        return  this.items.equals(lr1State.items);
        // 排序会影响可视化中item的顺序

        for (LR1Item item : lr1State.getItems()) {
            if(!this.items.contains(item)) return false;
        }

        return true;

    }

    @Override
    public boolean equals(Object obj){
        LR1State lr1State = (LR1State) obj;
        return this.equalItems(lr1State);
    }


}
