package budget;

import java.security.Principal;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes(value = {"currentUser", "pageTitle", "accountList", "lastBalance", "overallBalance", "account", "transferAccountList", "profile"})
public class BudgetController
{
	private static final Log log = LogFactory.getLog(BudgetController.class);
	
	@Autowired
	private BudgetDAO budgetDAO;

	@Autowired
	private MessageDigestService messageDigestService;
	
	@Autowired
	private BudgetValidator budgetValidator;
	
	@Autowired
	private CurrencyFormatter currencyFormatter;

	@RequestMapping(value = "403", method = RequestMethod.GET)
	public String errorPage403(Model model)
	{
		model.addAttribute("pageTitle", "Error");
		return "403";
	}

	@RequestMapping(value = "404", method = RequestMethod.GET)
	public String errorPage404(Model model)
	{
		model.addAttribute("pageTitle", "Error");
		return "404";
	}

	@RequestMapping(value = "500", method = RequestMethod.GET)
	public String errorPage500(Model model)
	{
		model.addAttribute("pageTitle", "Error");
		return "500";
	}
	
	@RequestMapping(value = {"/", "/login"}, method = RequestMethod.GET)
	public String login(Model model)
	{
		model.addAttribute("pageTitle", "Login");
		return "login";
	}
	
	@RequestMapping(value = "/profile", method = RequestMethod.GET)
	public String profile(Model model)
	{
	    User currentUser = (User) model.asMap().get("currentUser");
	    Profile profile = new Profile();
	    profile.setShow(currentUser.getShow());
	    if (currentUser.getLim() != 0) 
	    {
	        profile.setLim(currentUser.getLim().toString());
	    }
		model.addAttribute("profile", profile);
		model.addAttribute("pageTitle", "Profile");
		return "profile";
	}
	
	@RequestMapping(value = "/profile", params="change", method = RequestMethod.POST)
	public String changePassword(Model model, @ModelAttribute("profile") Profile profile)
	{
		try
		{
            Integer lim = 0;
            try
            {
                if (profile.getLim() != null && !profile.getLim().equals(""))
                {
                    lim = Integer.parseInt(profile.getLim());
                }
                if (lim < 0)
                {
                    throw new Exception();
                }
            }
            catch(Exception e)
            {
                model.addAttribute("errorText", "Limit must be an integer >= 0");
                return "profile";
            }
            User currentUser = (User) model.asMap().get("currentUser");
			if ((profile.getCurrentPassword() != null && !profile.getCurrentPassword().equals("")) ||
			    (profile.getNewPassword()     != null && !profile.getNewPassword().equals(""))     ||
			    (profile.getNewPassword2()    != null && !profile.getNewPassword2().equals("")))
			{
	            String errorText = budgetValidator.validatePassword(currentUser, profile);
	            if (errorText != null)
	            {
	                model.addAttribute("errorText", errorText);
	                return "profile";
	            }
	                
	            String password = messageDigestService.getMessageDigest(profile.getNewPassword());
	            budgetDAO.updateProfile(currentUser, password, profile.getShow(), lim);
	            currentUser.setShow(profile.getShow());
	            currentUser.setLim(lim);
			}
			else
			{
			    budgetDAO.updateProfile(currentUser, null, profile.getShow(), lim);
			    currentUser.setShow(profile.getShow());
			    currentUser.setLim(lim);
			}
			model.addAttribute("messageText", "The profile has been updated");
			return accounts(model);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
			return "profile";
		}
	}

	@RequestMapping(value = "/profile", params="back", method = RequestMethod.POST)
	public String changePassword(Model model)
	{
		return accounts(model);
	}
	
	@RequestMapping(value = "loginSuccess", method = RequestMethod.GET)
	public String loginSuccess(Model model, Principal p)
	{
		try
		{
		    User currentUser = budgetDAO.getUserByName(p.getName());
			model.addAttribute("currentUser", currentUser);
			if (budgetDAO.getMyPendingRequestListSize(currentUser) > 0)
			{
			    model.addAttribute("msgText", "You have pending requests");
			    return requests(model);
			}
			else
			{
			    return accounts(model);
			}
		}
		catch(BudgetException e)
		{
			log.error("error during login", e);
		}
		return "500";
	}
	
	@RequestMapping(value = "/requests/action-accept", params = "id", method = RequestMethod.GET)
	public String actionAccept(Model model, @RequestParam Integer id)
	{
        try
        {
            User currentUser = (User) model.asMap().get("currentUser");
            Request request = budgetDAO.getRequestById(id);
            if (request == null)
            {
                throw new BudgetException("The request was not found");
            }
            if (!request.getFrom().getOwner().equals(currentUser))
            {
                log.error("attempt to accept an unowned request: " + currentUser);
                return "403";
            }
            Integer val = request.getAmount();
            String note = "Request accepted";
            if (request.getNote().length() > 0)
            {
                note = note + ": " + request.getNote();
            }
            boolean balanced = budgetDAO.isBalanced();
            budgetDAO.transfer(request.getFrom(), request.getTo(), val, note);
            if (balanced)
            {
                budgetDAO.balance();
            }
            budgetDAO.deleteRequest(id);
        }
        catch(BudgetException e)
        {
            log.error(e);
            model.addAttribute("errorText", e.getMessage());
        }
        return requests(model);
	}

    @RequestMapping(value = "/requests/action-decline", params = "id", method = RequestMethod.GET)
    public String actionDecline(Model model, @RequestParam Integer id)
    {
        try
        {
            User currentUser = (User) model.asMap().get("currentUser");
            Request request = budgetDAO.getRequestById(id);
            if (request == null)
            {
                throw new BudgetException("The request was not found");
            }
            if (!request.getFrom().getOwner().equals(currentUser))
            {
                log.error("attempt to decline an unowned request: " + currentUser);
                return "403";
            }
            budgetDAO.updateRequestState(id, Request.STATE_DECLINED);
        }
        catch(BudgetException e)
        {
            log.error(e);
            model.addAttribute("errorText", e.getMessage());
        }
        return requests(model);
    }

    @RequestMapping(value = "/requests/action-delete", params = "id", method = RequestMethod.GET)
    public String actionDelete(Model model, @RequestParam Integer id)
    {
        try
        {
            User currentUser = (User) model.asMap().get("currentUser");
            Request request = budgetDAO.getRequestById(id);
            if (request == null)
            {
                throw new BudgetException("The request was not found");
            }
            if (!request.getTo().getOwner().equals(currentUser))
            {
                log.error("attempt to delete an unowned request: " + currentUser);
                return "403";
            }
            budgetDAO.deleteRequest(id);
        }
        catch(BudgetException e)
        {
            log.error(e);
            model.addAttribute("errorText", e.getMessage());
        }
        return requests(model);
    }
    
	@RequestMapping(value = "/requests", method = RequestMethod.GET)
	public String requests(Model model)
	{
	    try
	    {
	        User currentUser = (User) model.asMap().get("currentUser");
	        List<Request> list = budgetDAO.getRequestList(currentUser);
	        model.addAttribute("pageTitle", "Requests");
	        model.addAttribute("requestList", list);
	    }
	    catch(BudgetException e)
	    {
            log.error(e);
            model.addAttribute("errorText", e.getMessage());
	    }
	    model.addAttribute("pageTitle", "Requests");
	    return "requests";
	}
	
    @RequestMapping(value = "/requests", params="back", method = RequestMethod.POST)
    public String back(Model model)
    {
        return accounts(model);
    }
	
	@RequestMapping(value = "/accounts", method = RequestMethod.GET)
	public String accounts(Model model)
	{
		try
		{
			User currentUser = (User) model.asMap().get("currentUser");
			List<Account> list = budgetDAO.getAccountList(currentUser);
			List<Account> listAll = budgetDAO.getAccountListAll(currentUser);
			model.addAttribute("pageTitle", "List Accounts");
			model.addAttribute("accountList", list);
			Balance b = budgetDAO.getLastBalance();
			if (b == null)
			{
				model.addAttribute("lastBalance", 0);
			}
			else
			{
				model.addAttribute("lastBalance", b.getTransactId());
			}
			
			model.addAttribute("overallBalance", currencyFormatter.format(getOverallBalance(listAll)));
			model.addAttribute("overallLastBalance", currencyFormatter.format(getOverallLastBalance(listAll)));
			CreateBean createBean = new CreateBean();
			model.addAttribute("createBean", createBean);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
		}
		return "accounts";
	}

	@PreAuthorize("hasRole('admin')")
	@RequestMapping(value = "/accounts", params="balance", method = RequestMethod.POST)
	public String balance(Model model)
	{
		try
		{
			budgetDAO.balance();
			return accounts(model);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
			return "accounts";
		}
	}

	@PreAuthorize("hasRole('admin')")
	@RequestMapping(value = "/accounts", params="unbalance", method = RequestMethod.POST)
	public String unbalance(Model model)
	{
		try
		{
			budgetDAO.unbalance();
			return accounts(model);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
			return "accounts";
		}
	}
	
	@RequestMapping(value = "/accounts", params="create", method = RequestMethod.POST)
	public String accounts(Model model, @ModelAttribute("createBean") CreateBean createBean)
	{
		try
		{
			User currentUser = (User) model.asMap().get("currentUser");
			String errorText = budgetValidator.validateAccountName(currentUser, createBean.getAccountName());
			if (errorText != null)
			{
				model.addAttribute("errorText", errorText);
				return "accounts";
			}
			budgetDAO.createAccount(currentUser, createBean.getAccountName());
			return accounts(model);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
			return "accounts";
		}
	}

	@PreAuthorize("hasRole('admin')")
	@RequestMapping(value = "/accounts/delete", params = {"accountId", "txId"}, method = RequestMethod.GET)
	public String delete(Model model, @RequestParam Integer accountId, @RequestParam Integer txId)
	{
		try
		{
			budgetDAO.deleteTransaction(txId);
			return account(model, accountId);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
			return "account";
		}
	}
	
	@RequestMapping(value = "/accounts/{id}", method = RequestMethod.GET)
	public String account(Model model, @PathVariable("id") Integer id)
	{
		try
		{
			User currentUser = (User) model.asMap().get("currentUser");
			model.addAttribute("pageTitle", "Account Details");
			Account account = budgetDAO.getAccountById(id);
			if (account == null)
			{
				return "404";
			}
			if (!currentUser.getRole().isAdmin() && !currentUser.equals(account.getOwner()))
			{
			    log.error("non-privileged user attempting to read unowned account: " + currentUser);
			    return "403";
			}
			account.setTransactList(budgetDAO.getTransactList(currentUser, id));
			model.addAttribute("account", account);
			List<Account> list = budgetDAO.getTransferAccountList(currentUser, account);
			model.addAttribute("transferAccountList", list);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
		}
		return "account";
	}	

    @RequestMapping(value = "/accounts/transact", params="rename", method = RequestMethod.POST)
    public String rename(Model model, @ModelAttribute("account") Account account)
    {
        try
        {
            User currentUser = (User) model.asMap().get("currentUser");
            Account accountTmp = budgetDAO.getAccountById(account.getId());
            if (!currentUser.equals(accountTmp.getOwner()))
            {
                log.error("attempt to rename unowned account: " + currentUser);
                return "403";
            }
            String errorText = budgetValidator.validateAccountName(currentUser, account.getName());
            if (errorText != null)
            {
                model.addAttribute("errorText", errorText);
                return "account";
            }
            budgetDAO.renameAccount(account);
            return account(model, account.getId());
        }
        catch(BudgetException e)
        {
            log.error(e);
            model.addAttribute("errorText", e.getMessage());
            return "account";
        }
    }
    
    @RequestMapping(value = "/accounts/transact", params="request", method = RequestMethod.POST)
    public String request(Model model, @ModelAttribute("account") Account account)
    {
        try
        {
            User currentUser = (User) model.asMap().get("currentUser");
            Account accountTo = budgetDAO.getAccountById(account.getIdTo());
            if (accountTo == null)
            {
                model.addAttribute("errorText", "The 'Target' account was not selected or not found");
                return "account";
            }
            Account accountTmp = budgetDAO.getAccountById(account.getId());
            if (!currentUser.equals(accountTmp.getOwner()))
            {
                log.error("attempt to request from unowned account: " + currentUser);
                return "403";
            }
            if (currentUser.equals(accountTo.getOwner()))
            {
                model.addAttribute("errorText", "You cannot request from an account you own");
                return "account";
            }
            if (!accountTmp.getPub() || !accountTo.getPub())
            {
                log.error("attempt to request from/to private account: " + currentUser);
                return "403";
            }
            String errorText = budgetValidator.validateRequest(account);
            if (errorText != null)
            {
                model.addAttribute("errorText", errorText);
                return "account";
            }
            Integer amount = currencyFormatter.parse(account.getTransactionAmount());
            if (amount == 0)
            {
                model.addAttribute("errorText", "You cannot request $0.00");
                return "account";
            }
            
            budgetDAO.request(accountTo.getId(), account.getId(), amount, account.getNote());
            model.addAttribute("msgText", "Request successfully submitted");
            return account(model, account.getId());
        }
        catch(BudgetException e)
        {
            log.error(e);
            model.addAttribute("errorText", e.getMessage());
            return "account";
        }
    }
    
	@RequestMapping(value = "/accounts/transact", params="transfer", method = RequestMethod.POST)
	public String transfer(Model model, @ModelAttribute("account") Account account)
	{
		try
		{
		    User currentUser = (User) model.asMap().get("currentUser");
		    Account accountTo = budgetDAO.getAccountById(account.getIdTo());
		    if (accountTo == null)
		    {
		        model.addAttribute("errorText", "The 'To' account was not selected or not found");
		        return "account";
		    }
		    Account accountTmp = budgetDAO.getAccountById(account.getId());
		    if (!currentUser.getRole().isAdmin())
		    {
		        if (!currentUser.equals(accountTmp.getOwner()))
		        {
		            log.error("non-privileged user attempting to transfer from unowned account: " + currentUser);
		            return "403";
		        }
		    }
            if (!currentUser.equals(accountTo.getOwner()))
            {
                if (!accountTmp.getPub() || !accountTo.getPub())
                {
                    log.error("attempt to transfer from/to private account: " + currentUser);
                    return "403";
                }
            }
			String errorText = budgetValidator.validateAccount(account, true);
			if (errorText != null)
			{
				model.addAttribute("errorText", errorText);
				return "account";
			}
            Integer val = currencyFormatter.parse(account.getTransactionAmount());
            if (val == 0)
            {
                model.addAttribute("errorText", "You cannot transfer $0.00");
                return "account";
            }
			boolean balanced = budgetDAO.isBalanced();
            budgetDAO.transfer(account, accountTo, val, account.getNote());
			if (balanced)
			{
			    budgetDAO.balance();
			    model.addAttribute("lastBalance", budgetDAO.getLastBalance().getTransactId());
			}
			return account(model, account.getId());
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
			return "account";
		}
	}

	private String transact(Model model, Account account, boolean deposit)
	{
		try
		{
			String errorText = budgetValidator.validateAccount(account, false);
			if (errorText != null)
			{
				model.addAttribute("errorText", errorText);
				return "account";
			}
			Account accountTmp = budgetDAO.getAccountById(account.getId());
			if (!accountTmp.getPub())
			{
			    User currentUser = (User) model.asMap().get("currentUser");
			    if (!currentUser.equals(accountTmp.getOwner()))
			    {
			        log.error("privileged user attempting to transact against unowned private account");
			        return "403";
			    }
			}
			Integer val = (deposit ? currencyFormatter.parse(account.getTransactionAmount()) : -currencyFormatter.parse(account.getTransactionAmount()));
			if (val == 0)
			{
			    model.addAttribute("errorText", "You cannot deposit or withdraw $0.00");
			    return "account";
			}
			budgetDAO.transact(account.getId(), val, account.getNote());
			return accounts(model);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
			return "account";
		}
	}
	
	@PreAuthorize("hasRole('admin')")
	@RequestMapping(value = "/accounts/transact", params="deposit", method = RequestMethod.POST)
	public String deposit(Model model, @ModelAttribute("account") Account account)
	{
		return transact(model, account, true);
	}
	
	@PreAuthorize("hasRole('admin')")
	@RequestMapping(value = "/accounts/transact", params="withdraw", method = RequestMethod.POST)
	public String withdraw(Model model, @ModelAttribute("account") Account account)
	{
		return transact(model, account, false);
	}

	@RequestMapping(value = "/accounts/transact", params="back", method = RequestMethod.POST)
	public String back(Model model, @ModelAttribute("account") Account account)
	{
		return accounts(model);
	}

	@RequestMapping(value = "/accounts/transact", params="visible", method = RequestMethod.POST)
	public String visible(Model model, @ModelAttribute("account") Account account)
	{
        try
        {
            User currentUser = (User) model.asMap().get("currentUser");
            Account accountTmp = budgetDAO.getAccountById(account.getId());
            if (!currentUser.equals(accountTmp.getOwner()) && !currentUser.getRole().isAdmin())
            {
                log.error("user attempting to modify account visibility of unowned account: " + currentUser);
                return "403";
            }
            budgetDAO.markVisibility(account, false);
        }
        catch(BudgetException e)
        {
            log.error(e);
            model.addAttribute("errorText", e.getMessage());
        }
        return account(model, account.getId());
	}
	
	@RequestMapping(value = "/accounts/transact", params="hide", method = RequestMethod.POST)
    public String hidden(Model model, @ModelAttribute("account") Account account)
    {
	    try
	    {
            User currentUser = (User) model.asMap().get("currentUser");
            Account accountTmp = budgetDAO.getAccountById(account.getId());
            if (!currentUser.equals(accountTmp.getOwner()) && !currentUser.getRole().isAdmin())
            {
                log.error("user attempting to modify account visibility of unowned account: " + currentUser);
                return "403";
            }
            budgetDAO.markVisibility(account, true);
	    }
	    catch(BudgetException e)
	    {
            log.error(e);
            model.addAttribute("errorText", e.getMessage());
	    }
	    return account(model, account.getId());
    }
	
	@RequestMapping(value = "/accounts/transact", params="public", method = RequestMethod.POST)
	public String pub(Model model, @ModelAttribute("account") Account account)
	{
		try
		{
            User currentUser = (User) model.asMap().get("currentUser");
            Account accountTmp = budgetDAO.getAccountById(account.getId());
            if (!currentUser.equals(accountTmp.getOwner()))
            {
                log.error("user attempting to modify account privacy of unowned account: " + currentUser);
                return "403";
            }
			budgetDAO.markPrivacy(account, true);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
		}
		return account(model, account.getId());
	}

	@RequestMapping(value = "/accounts/transact", params="private", method = RequestMethod.POST)
	public String priv(Model model, @ModelAttribute("account") Account account)
	{
		try
		{
            User currentUser = (User) model.asMap().get("currentUser");
            Account accountTmp = budgetDAO.getAccountById(account.getId());
            if (!currentUser.equals(accountTmp.getOwner()))
            {
                log.error("user attempting to modify account visibility of unowned account: " + currentUser);
                return "403";
            }
			budgetDAO.markPrivacy(account, false);
		}
		catch(BudgetException e)
		{
			log.error(e);
			model.addAttribute("errorText", e.getMessage());
		}
		return account(model, account.getId());
	}
	
    private Integer getOverallLastBalance(List<Account> list)
    {
        Integer balance = 0;
        for (Account account: list)
        {
            balance += account.getLastBalance();
        }
        return balance;
    }
    
	private Integer getOverallBalance(List<Account> list)
	{
		Integer balance = 0;
		for (Account account: list)
		{
			balance += account.getBalance();
		}
		return balance;
	}
}
