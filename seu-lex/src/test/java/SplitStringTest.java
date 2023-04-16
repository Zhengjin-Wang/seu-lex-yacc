import constant.SpAlpha;
import org.junit.Test;

public class SplitStringTest {

    @Test
    public void test(){
        String s = " asd  ads   ";
        System.out.println(s.substring(0, 2));
    }

    @Test
    public void asciiTest(){
        char c = '\"';
        System.out.println(c);
    }

    @Test
    public void operandTest(){
        char c = '-';
        System.out.println(SpAlpha.isLexOperand(c));
    }

}
