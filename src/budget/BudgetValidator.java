package budget;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BudgetValidator
{
	@Autowired
	private MessageDigestService messageDigestService;
	
	@Autowired
	private BudgetDAO budgetDAO;

	public String validatePassword(User currentUser, Profile profile)
	{
	    if (!messageDigestService.getMessageDigest(profile.getCurrentPassword()).equals(currentUser.getPassword()))
	    {
	        return "The current password you've entered is incorrect";
	    }
		if (profile.getNewPassword() == null || profile.getNewPassword().equals(""))
		{
			return "The password cannot be empty";
		}
		if (profile.getNewPassword().length() <= 4)
		{
			return "The password is too short";
		}
		if (profile.getNewPassword().length() > 100)
		{
		    return "The password cannot be longer than 100 characters";
		}
		if (!profile.getNewPassword().equals(profile.getNewPassword2()))
		{
			return "The retyped new password does not match the new password";
		}
		if (messageDigestService.getMessageDigest(profile.getNewPassword()).equals(currentUser.getPassword()))
		{
			return "That password is already being used";
		}
		return null;
	}
	
	public String validateAccountName(User owner, String name) throws BudgetException
	{
		if (name== null || name.length() == 0)
		{
			return "The account name cannot be empty";
		}
		if (name.length() > 30)
		{
			return "The account name cannot be longer than 30 characters";
		}
		for (int i = 0; i < name.length(); i++)
		{
			Character ch = name.charAt(i);
			if (ch != '_' && ch != '-' && ch != '.' && !Character.isLetterOrDigit(ch))
			{
				return "The account name can only contain alphanumeric characters, the dash character (-), the period character (.) or the underscore character(_)";
			}
		}
		if (budgetDAO.getAccountByName(owner, name) != null)
		{
			return "That account already exists";
		}
		return null;
	}

    public String validateRequest(Account account) throws BudgetException
    {
        if (account.getId() == null)
        {
            return "The account has no id";
        }
        if (account.getNote() == null)
        {
            return "The account note must not be null";
        }
        if (account.getNote().length() > 200)
        {
            return "The account note cannot be longer than 200 characters";
        }
        if (budgetDAO.getAccountById(account.getIdTo()) == null)
        {
            return "The target account does not exist";
        }
        return null;
    }
	
	public String validateAccount(Account account, boolean transferFlag) throws BudgetException
	{
		if (account.getId() == null)
		{
			return "The account has no id";
		}
		if (account.getNote() == null)
		{
			return "The account note must not be null";
		}
		if (account.getNote().length() > 200)
		{
			return "The account note cannot be longer than 200 characters";
		}
		if (transferFlag)
		{
			if (budgetDAO.getAccountById(account.getIdTo()) == null)
			{
				return "The target account does not exist";
			}
		}
		else
		{
			if (budgetDAO.getAccountById(account.getIdTo()) != null)
			{
				return "The target account cannot exist";
			}
		}
		return null;
	}
}
