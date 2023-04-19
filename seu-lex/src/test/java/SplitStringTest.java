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

    @Test
    public void checkAllChar(){

        char ch = '点';
        int intValue = (int) ch;
        System.out.println("字符 " + ch + " 对应的整数值是 " + intValue);

    }



}
