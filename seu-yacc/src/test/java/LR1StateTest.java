import dto.LR1Item;
import dto.LR1ItemCore;
import dto.LR1State;
import org.junit.Test;

import java.util.*;

public class LR1StateTest {

    @Test
    public void equalItemsTest(){
        LR1Item item1 = new LR1Item(1, 2, 3);
        LR1Item item2 = new LR1Item(2, 2, 3);
        LR1Item item3 = new LR1Item(2, 2, 3);
        LR1Item item4 = new LR1Item(1, 2, 3);
        Set<LR1Item> a = new LinkedHashSet<>();
        a.add(item1);
        a.add(item2);
        Set<LR1Item> b = new LinkedHashSet<>();
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

    @Test
    public void equalSetTest(){
        LR1Item item1 = new LR1Item(1, 2, 3);
        LR1Item item2 = new LR1Item(2, 2, 3);
        LR1Item item3 = new LR1Item(2, 2, 3);
        LR1Item item4 = new LR1Item(1, 2, 3);
        Set<LR1Item> a = new HashSet<>();
        a.add(item1);
        a.add(item2);
        Set<LR1Item> b = new HashSet<>();
        b.add(item3);
        b.add(item4);
        Set<Set<LR1Item>> x = new HashSet<>();
        x.add(a);
        System.out.println(x.contains(b));
        System.out.println(a.equals(b));

    }

    @Test
    public void coreSetTest(){
        LR1Item item1 = new LR1Item(1, 2, 3);
        LR1Item item2 = new LR1Item(1, 2, 4);

        Set<LR1ItemCore> set = new HashSet<>();
        set.add(item1.getLr1ItemCore());
        set.add(item2.getLr1ItemCore());
        System.out.println(set.size());

    }

}
