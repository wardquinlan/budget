package budget;

public class BudgetException extends Exception
{
	public BudgetException(String msg)
	{
		super(msg);
	}
	
	public BudgetException(String msg, Throwable cause)
	{
		super(msg, cause);
	}
	
	public BudgetException(Throwable cause)
	{
		super(cause);
	}
}
