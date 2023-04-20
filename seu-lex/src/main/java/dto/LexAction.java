package dto;

import lombok.Data;

@Data
public class LexAction {
    private Integer order;
    private String action;

    public LexAction(Integer order, String action){
        this.order = order;
        this.action = action;
    }

    public static boolean isHigherPriority(LexAction a, LexAction b){
        return a.order < b.order;
    }
}
