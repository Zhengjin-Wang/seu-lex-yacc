import dto.Regex;
import org.junit.Test;


public class RegexTest {

    @Test
    public void quoteReplaceTest(){
        String s = "\"[]*?+()|.^{},-\\\"]\"\"\"[].";
        String s2 = "[.\\.]";
        Regex regex = new Regex(s2);
        System.out.println(regex.getEscapeRegex());
    }

    @Test
    public void expandTest(){
        String s = "[^0-9a-zA-Z][1-3]\"[x]\"";
        Regex regex = new Regex(s);
        System.out.println(regex.getExpandRegex());
    }


    @Test
    public void charTest(){

        System.out.println((char) ('a'+1));
    }
}
