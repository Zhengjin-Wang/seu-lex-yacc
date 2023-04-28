package dto;


import lombok.Data;

import java.util.*;

/**
 * LR1的DFA中的一个状态
 *
 */
@Data
public class LR1State {

    private Integer number;
    private Map<Integer, Integer> edges = new HashMap<>(); // --终结符编号-> 状态号
    private List<LR1Item> items = new ArrayList<>(); // 所有的item

    public void sortItems(){
        Collections.sort(this.items, new Comparator<LR1Item>() {
            @Override
            public int compare(LR1Item o1, LR1Item o2) {
                if (o1.equals(o2)) return 0;
                boolean b =  o1.getProductionNumber() == o2.getProductionNumber() ?
                        ( o1.getDotPos() == o2.getDotPos() ?
                                o1.getPredictSymbol() < o2.getPredictSymbol()
                                :o1.getDotPos() < o2.getDotPos() ):
                        o1.getProductionNumber() < o2.getProductionNumber();
                return b?-1:1;
            }
        });
    }

    public boolean equalItems(LR1State lr1State) {

        if(items.size() != lr1State.items.size()){
            return false;
        }
        lr1State.sortItems();
        this.sortItems();

        return  this.items.equals(lr1State.items);

    }



}
