package marketprofile;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.math.BigDecimal;
import java.util.Date;

@RunWith(PowerMockRunner.class)
public class MarketProfileRendererTest {

    @Mock
    Date dateMock;

    @Test
    public void roundValueWithTickSizeTest() throws Exception {
        MarketProfileRenderer mock = PowerMockito.spy(new MarketProfileRenderer(dateMock, false));
        mock.setTickSize(0.05);
        BigDecimal exp = new BigDecimal("0.15");
        BigDecimal res = Whitebox.invokeMethod(mock, "roundValueWithTickSize", 0.16);
        Assert.assertEquals(exp, res);
    }

    @Test
    public void getSymbolTest() throws Exception {
        MarketProfileRenderer mock = PowerMockito.spy(new MarketProfileRenderer(dateMock, false));

        char exp = 'B';
        char res = Whitebox.invokeMethod(mock, "getSymbol", 'A');
        Assert.assertEquals(exp, res);

        exp = 'Z';
        res = Whitebox.invokeMethod(mock, "getSymbol", 'Y');
        Assert.assertEquals(exp, res);

        exp = 'a';
        res = Whitebox.invokeMethod(mock, "getSymbol", 'Z');
        Assert.assertEquals(exp, res);

        exp = 'A';
        res = Whitebox.invokeMethod(mock, "getSymbol", 'z');
        Assert.assertEquals(exp, res);
    }
}
