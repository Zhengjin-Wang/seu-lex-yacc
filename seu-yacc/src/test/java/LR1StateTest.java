import dto.LR1Item;
import dto.LR1State;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LR1StateTest {

    @Test
    public void equalItemsTest(){
        LR1Item item1 = new LR1Item(1, 2, 3);
        LR1Item item2 = new LR1Item(2, 2, 3);
        LR1Item item3 = new LR1Item(2, 2, 3);
        LR1Item item4 = new LR1Item(1, 2, 3);
        List<LR1Item> a = new ArrayList<>();
        a.add(item1);
        a.add(item2);
        List<LR1Item> b = new ArrayList<>();
        b.add(item3);
        b.add(item4);
        LR1State s1 = new LR1State();
        LR1State s2 = new LR1State();
        s1.setItems(a);
        s2.setItems(b);
        System.out.println(s1.getItems());
        System.out.println(s2.getItems());
        System.out.println(s1.equalItems(s2));
    }

}
