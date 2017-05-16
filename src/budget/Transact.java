package budget;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;

public class Transact
{
	private static CurrencyFormatter currencyFormatter = new CurrencyFormatter();
	
	private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private Integer id;
	private Date ts;
	private Integer debit;
	private Integer credit;
	private Integer balance;
	private String note;
	private String uuid;
	
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
	public Integer getDebit()
	{
		return debit;
	}
	public void setDebit(Integer debit)
	{
		this.debit = debit;
	}
	public Integer getCredit()
	{
		return credit;
	}
	public void setCredit(Integer credit)
	{
		this.credit = credit;
	}
	public Integer getBalance()
	{
		return balance;
	}
	public void setBalance(Integer balance)
	{
		this.balance = balance;
	}
	public String getNote()
	{
		return note;
	}
	public void setNote(String note)
	{
		this.note = note;
	}
	public String getFormattedDebit()
	{
		return currencyFormatter.format(debit);
	}
	public String getFormattedCredit()
	{
		return currencyFormatter.format(credit);
	}
	public String getFormattedBalance()
	{
		return currencyFormatter.format(balance);
	}
	public String getUuid() {
		return uuid;
	}
	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
