package budget;

import java.util.StringTokenizer;

public class CurrencyFormatter
{
	public String format(Integer value)
	{
		if (value == null)
		{
			return "";
		}
		String sign = "";
		if (value < 0)
		{
			value = -value;
			sign = "-";
		}
		Integer dollars = value / 100;
		Integer cents = value % 100;
		if (cents == 0)
		{
			return "$" + sign + dollars + ".00";
		}
		else if (cents < 10)
		{
			return "$" + sign + dollars + ".0" + cents;
		}
		else
		{
			return "$" + sign + dollars + "." + cents;
		}
	}
	
	public Integer parse(String text) throws BudgetException
	{
		try
		{
			if (text.startsWith("."))
			{
				text = "0" + text;
			}
			if (text == null || text.length() == 0)
			{
				throw new BudgetException("Invalid currency: no data");
			}
			StringTokenizer st = new StringTokenizer(text, ".");
			if (st.countTokens() == 0)
			{
				throw new BudgetException("Invalid currency: no digits: " + text);
			}
			if (st.countTokens() > 2)
			{
				throw new BudgetException("Invalid currency: multiple decimal points: " + text);
			}
			Integer value = Integer.parseInt(st.nextToken()) * 100;
			if (value < 0)
			{
				throw new BudgetException("Invalid currency: negative: " + text);
			}
			if (value > 100000000)
			{
				throw new BudgetException("Invalid currency: too large: " + text);
			}
			if (st.hasMoreTokens())
			{
				String tmp = st.nextToken();
				if (tmp.length() == 1)
				{
					value += Integer.parseInt(tmp) * 10;
				}
				else if (tmp.length() == 2)
				{
					value += Integer.parseInt(tmp);
				}
				else if (tmp.length() > 2)
				{
					throw new BudgetException("Invalid currency: too many decimals: " + text);
				}
			}
			return value;
		}
		catch(NumberFormatException e)
		{
			throw new BudgetException("Invalid currency: not numeric: " + text, e);
		}
	}
}
