import dto.LR1Item;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class LR1ItemTest {

    @Test
    public void equalsTest(){
        LR1Item lr1Item = new LR1Item(1,1,1);
        Set<LR1Item> set = new HashSet<>();
        set.add(lr1Item);
        LR1Item lr1Item2 = new LR1Item(1,1,1);
        System.out.println(lr1Item.equals(lr1Item2));
        System.out.println(set.contains(lr1Item2));
    }
}
