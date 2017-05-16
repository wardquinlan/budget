package budget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

public class Request
{
	private static CurrencyFormatter currencyFormatter = new CurrencyFormatter();
	
	public static final Integer STATE_PENDING = 0;
	public static final Integer STATE_DECLINED = 1;
	public static final Map<Integer, String> map;
	static
	{
	    map = new HashMap<Integer, String>();
	    map.put(STATE_PENDING, "PENDING");
	    map.put(STATE_DECLINED, "DECLINED");
	}
	
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Integer id;
	private Date ts;
	private Account accountFrom;
	private Account accountTo;
	private Integer state;
	private Integer amount;
	private String note;
	
	public Integer getId()
	{
		return id;
	}
	public void setId(Integer id)
	{
		this.id = id;
	}
	public Date getTs()
	{
		return ts;
	}
	public String getFormattedTs()
	{
		return df.format(ts);
	}
	public void setTs(Date ts)
	{
		this.ts = ts;
	}
	public Integer getAmount()
	{
		return amount;
	}
	public void setAmount(Integer amount)
	{
		this.amount = amount;
	}
	public String getNote()
	{
		return note;
	}
	public void setNote(String note)
	{
		this.note = note;
	}
	public String getFormattedAmount()
	{
		return currencyFormatter.format(amount);
	}
	public String getStateAsString()
	{
	    return map.get(state);
	}
    public Integer getState()
    {
        return state;
    }
    public void setState(Integer state)
    {
        this.state = state;
    }
    public Account getFrom()
    {
        return accountFrom;
    }
    public void setFrom(Account accountFrom)
    {
        this.accountFrom = accountFrom;
    }
    public Account getTo()
    {
        return accountTo;
    }
    public void setTo(Account accountTo)
    {
        this.accountTo = accountTo;
    }
}
