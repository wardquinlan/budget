package budget;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BudgetDAO
{
	private static final Log log = LogFactory.getLog(BudgetDAO.class);
	
	private String url;
	
	public BudgetDAO(String url) throws BudgetException
	{
		this.url = url;
		try
		{
			Class.forName("org.postgresql.Driver");
		}
		catch(Exception e)
		{
			throw new BudgetException(e);
		}
	}
	
	public void deleteTransaction(Integer id) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
	        Transact tx = getTransactByIdInternal(c, id);
	        if (tx == null)
	        {
	            // already deleted
	            return;
	        }
	        Balance b = getLastBalanceInternal(c);
	        if (b != null && tx.getId() <= b.getTransactId())
	        {
	            throw new BudgetException("transaction has already been balanced");
	        }
			PreparedStatement ps = c.prepareStatement("delete from transact where uuid = ?");
			ps.setString(1, tx.getUuid());
			log.info("Issuing update: " + ps.toString());
			ps.executeUpdate();
			recalculateBalances(c);
			commit(c);
		}
		catch(Exception e)
		{
			rollback(c);
			throw new BudgetException("deleteTransaction failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	private void recalculateBalances(Connection c) throws SQLException
	{
	    PreparedStatement ps = c.prepareStatement("select id from account");
	    ResultSet resultSet = ps.executeQuery();
	    while (resultSet.next())
	    {
	        Integer id = resultSet.getInt("id");
	        recalculateBalancesForAccount(c, id);
	    }
	}
	
	private void recalculateBalancesForAccount(Connection c, Integer accountId) throws SQLException
	{
	    Integer balance;
	    PreparedStatement ps = null;
	    Transact tx = getBalancedTransactForAccount(c, accountId);
	    if (tx == null)
	    {
            balance = 0;
            ps = c.prepareStatement("select * from transact tx where tx.accountid = ? order by id asc");
            ps.setInt(1, accountId);
	    }
	    else
	    {
	        balance = tx.getBalance();
            ps = c.prepareStatement("select * from transact tx where tx.accountid = ? and tx.id > ? order by id asc");
            ps.setInt(1, accountId);
            ps.setInt(2, tx.getId());
	    }
        ResultSet resultSet = ps.executeQuery();
        while (resultSet.next())
        {
            Integer id2 = resultSet.getInt("id");
            Integer amount = resultSet.getInt("amount");
            balance += amount;
            ps = c.prepareStatement("update transact set balance = ? where id = ?");
            ps.setInt(1, balance);
            ps.setInt(2, id2);
            log.info("Issuing update: " + ps.toString());
            ps.executeUpdate();            
        }
	}
	
	public void request(Integer fromId, Integer toId, Integer amount, String note) throws BudgetException
	{
        Connection c = null;
        try
        {
            c = open();
            PreparedStatement ps = c.prepareStatement("insert into request(ts, fromid, toid, state, amount, note) values(?, ?, ?, ?, ?, ?)");
            ps.setTimestamp(1, new Timestamp((new Date()).getTime()));
            ps.setInt(2, fromId);
            ps.setInt(3, toId);;
            ps.setInt(4, Request.STATE_PENDING);
            ps.setInt(5, amount);
            ps.setString(6, note);
            log.info("Issuing update: " + ps.toString());
            ps.executeUpdate();
            commit(c);
        }
        catch(SQLException e)
        {
            rollback(c);
            throw new BudgetException("request failed", e);
        }
        finally
        {
            close(c);
        }
	}
	
	public void transact(Integer accountId, Integer amount, String note) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			transactInternal(c, accountId, amount, note, UUID.randomUUID().toString());
			commit(c);
		}
		catch(SQLException e)
		{
			rollback(c);
			throw new BudgetException("transact failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public void transfer(Account account, Account accountTo, Integer amount, String note) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			String uuid = UUID.randomUUID().toString();
			String from;
			String to;
			if (account.getOwner().equals(accountTo.getOwner()))
			{
				to = "[To " + accountTo.getName() + "] " + note;
				from = "[From " + account.getName() + "] " + note;
			}
			else
			{
				to = "[To " + accountTo.getExtendedName() + "] " + note;
				from = "[From " + account.getExtendedName() + "] " + note;
			}
			transactInternal(c, account.getId(), -amount, to, uuid);
			transactInternal(c, accountTo.getId(), amount, from, uuid);
			commit(c);
		}
		catch(SQLException e)
		{
			rollback(c);
			throw new BudgetException("transfer failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public void updateProfile(User user, String password, Boolean show, Integer lim) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			if (password != null)
			{
	            PreparedStatement ps = c.prepareStatement("update users set password = ?, show = ?, lim = ? where id = " + user.getId());
	            ps.setString(1, password);
	            ps.setInt(2, (show ? 1 : 0));
	            ps.setInt(3, lim);
	            log.info("Issuing update: " + ps.toString());
	            ps.executeUpdate();
			}
			else
			{
	            PreparedStatement ps = c.prepareStatement("update users set show = ?, lim = ? where id = " + user.getId());
	            ps.setInt(1, (show ? 1 : 0));
	            ps.setInt(2, lim);
	            log.info("Issuing update: " + ps.toString());
	            ps.executeUpdate();
			}
			commit(c);
		}
		catch(SQLException e)
		{
			rollback(c);
			throw new BudgetException("updateProfile failed", e);
		}
		finally
		{
			close(c);
		}
	}

    public void markVisibility(Account account, boolean hide) throws BudgetException
    {
        Connection c = null;
        try
        {
            c = open();
            PreparedStatement ps = c.prepareStatement("update account set hide = ? where id = ?");
            ps.setInt(1, (hide ? 1 : 0));
            ps.setInt(2, account.getId());
            log.info("Issuing update: " + ps.toString());
            ps.executeUpdate();
            commit(c);
        }
        catch(SQLException e)
        {
            rollback(c);
            throw new BudgetException("markVisibility failed", e);
        }
        finally
        {
            close(c);
        }
    }
	
	public void markPrivacy(Account account, boolean pub) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			PreparedStatement ps = c.prepareStatement("update account set pub = ? where id = ?");
			ps.setInt(1, (pub ? 1 : 0));
			ps.setInt(2, account.getId());
			log.info("Issuing update: " + ps.toString());
			ps.executeUpdate();
			commit(c);
		}
		catch(SQLException e)
		{
			rollback(c);
			throw new BudgetException("markPrivacy failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public void renameAccount(Account account) throws BudgetException
	{
        Connection c = null;
        try
        {
            c = open();
            PreparedStatement ps = c.prepareStatement("update account set name = ? where id = ?");
            ps.setString(1, account.getName());
            ps.setInt(2, account.getId());
            log.info("Issuing update: " + ps.toString());
            ps.executeUpdate();
            commit(c);
        }
        catch(SQLException e)
        {
            rollback(c);
            throw new BudgetException("renameAccount failed", e);
        }
        finally
        {
            close(c);
        }
	}
	
	public void createAccount(User owner, String accountName) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			PreparedStatement ps = c.prepareStatement("insert into account(ownerid, name, pub, hide) values(?,?,?,0)");
			ps.setInt(1, owner.getId());
			ps.setString(2, accountName);
			ps.setInt(3, 0);
			log.info("Issuing update: " + ps.toString());
			ps.executeUpdate();
			commit(c);
		}
		catch(SQLException e)
		{
			rollback(c);
			throw new BudgetException("createAccount failed", e);
		}
		finally
		{
			close(c);
		}
	}	

	public Account getAccountByName(User owner, String name) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			PreparedStatement ps = c.prepareStatement("select * from account a where a.ownerid = ? and a.name = ?");
			ps.setInt(1, owner.getId());
			ps.setString(2, name);
			ResultSet resultSet = ps.executeQuery();
			if (!resultSet.next())
			{
				return null;
			}
			Account account = new Account();
			account.setId(resultSet.getInt("id"));
			account.setName(resultSet.getString("name"));
			account.setOwner(getUserById(resultSet.getInt("ownerid")));
			account.setPub(resultSet.getInt("pub") == 1);
			return account;
		}
		catch(SQLException e)
		{
			throw new BudgetException("getAccountByName failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	private Account getAccountByIdInternal(Connection c, int id) throws SQLException
	{
        PreparedStatement ps = c.prepareStatement("select * from account a where a.id = ?");
        ps.setInt(1, id);
        ResultSet resultSet = ps.executeQuery();
        if (!resultSet.next())
        {
            return null;
        }
        Account account = new Account();
        account.setId(resultSet.getInt("id"));
        account.setName(resultSet.getString("name"));
        account.setNameOrig(resultSet.getString("name"));
        account.setOwner(getUserByIdInternal(c, resultSet.getInt("ownerid")));
        account.setPub(resultSet.getInt("pub") == 1);
        account.setHidden(resultSet.getInt("hide") == 1);
        return account;
	}
	
	public Request getRequestById(Integer id) throws BudgetException
	{
	    Connection c = null;
	    try
	    {
	        c = open();
	        return getRequestByIdInternal(c, id);
	    }
	    catch(SQLException e)
	    {
	        throw new BudgetException("getRequestById failed", e);
	    }
	    finally
	    {
	        close(c);
	    }
	}
	
	private Request getRequestByIdInternal(Connection c, Integer id) throws SQLException
	{
	    PreparedStatement ps = c.prepareStatement("select * from request r where r.id = ?");
	    ps.setInt(1, id);
        ResultSet resultSet = ps.executeQuery();
        if (!resultSet.next())
        {
            return null;
        }
        Request request = new Request();
        request.setId(resultSet.getInt("id"));
        request.setTs(resultSet.getTimestamp("ts"));
        request.setFrom(getAccountByIdInternal(c, resultSet.getInt("fromid")));
        request.setTo(getAccountByIdInternal(c, resultSet.getInt("toid")));
        request.setState(resultSet.getInt("state"));
        request.setAmount(resultSet.getInt("amount"));
        request.setNote(resultSet.getString("note"));
        return request;
	}
	
	public void updateRequestState(Integer id, Integer state) throws BudgetException
	{
	    Connection c = null;
	    try
	    {
	        c = open();
	        PreparedStatement ps = c.prepareStatement("update request set state = ? where id = ?");
            ps.setInt(1, state);
            ps.setInt(2, id);
            log.info("Issuing update: " + ps.toString());
            ps.executeUpdate();
            commit(c);
	    }
	    catch(SQLException e)
	    {
	        rollback(c);
	        throw new BudgetException("updateRequestState failed", e);
	    }
	    finally
	    {
	        close(c);
	    }
	}
	
	public void deleteRequest(Integer id) throws BudgetException
	{
        Connection c = null;
        try
        {
            c = open();
            PreparedStatement ps = c.prepareStatement("delete from request where id = ?");
            ps.setInt(1, id);
            log.info("Issuing update: " + ps.toString());
            ps.executeUpdate();
            commit(c);
        }
        catch(SQLException e)
        {
            rollback(c);
            throw new BudgetException("deleteRequest failed", e);
        }
        finally
        {
            close(c);
        }
	}
	
	public Account getAccountById(int id) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			return getAccountByIdInternal(c, id);
		}
		catch(SQLException e)
		{
			throw new BudgetException("getAccountById failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public void unbalance() throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			Balance lastBalance = getLastBalanceInternal(c);
			if (lastBalance != null)
			{
				PreparedStatement ps = c.prepareStatement("delete from balance where id = ?");
				ps.setInt(1, lastBalance.getId());
				log.info("Issuing update: " + ps.toString());
				ps.executeUpdate();
				commit(c);
			}
		}
		catch(SQLException e)
		{
			rollback(c);
			throw new BudgetException("unbalance failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public void balance() throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			Transact tx = getLastTransactInternal(c, null);
			if (tx == null)
			{
			    // no transactions; nothing to do
			    return;
			}
			Balance lastBalance = getLastBalanceInternal(c);
			if (lastBalance == null || lastBalance.getTransactId() < tx.getId())
			{
				PreparedStatement ps = c.prepareStatement("insert into balance(transactid) values(?)");
				ps.setInt(1, tx.getId());
				log.info("Issuing update: " + ps.toString());
				ps.executeUpdate();
				commit(c);
			}
		}
		catch(SQLException e)
		{
			rollback(c);
			throw new BudgetException("balance failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public boolean isBalanced() throws BudgetException
	{
        Connection c = null;
        try
        {
            c = open();
            Transact tx = getLastTransactInternal(c, null);
            if (tx == null)
            {
                return true;
            }
            Balance b = getLastBalanceInternal(c);
            if (b == null)
            {
                return false;
            }
            return (b.getTransactId().equals(tx.getId()));
        }
        catch(SQLException e)
        {
            throw new BudgetException("isBalanced failed", e);
        }
        finally
        {
            close(c);
        }
	}
	
	public Balance getLastBalance() throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			return getLastBalanceInternal(c);
		}
		catch(SQLException e)
		{
			throw new BudgetException("getLastBalance failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public Transact getLastTransact(Integer accountId) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			return getLastTransactInternal(c, accountId);
		}
		catch(SQLException e)
		{
			throw new BudgetException("getLastTransact failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public List<Transact> getTransactList(User currentUser, Integer accountId) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			List<Transact> list = new ArrayList<Transact>();
			PreparedStatement ps = c.prepareStatement("select * from transact where accountid = ? and id >= ? order by id desc");
			ps.setInt(1, accountId);
			ps.setInt(2, currentUser.getLim());
			ResultSet resultSet = ps.executeQuery();
			while (resultSet.next())
			{
				Transact tx = new Transact();
				tx.setId(resultSet.getInt("id"));
				tx.setTs(resultSet.getTimestamp("ts"));
				tx.setBalance(resultSet.getInt("balance"));
				Integer amount = resultSet.getInt("amount");
				if (amount >= 0)
				{
					tx.setCredit(amount);
				}
				else
				{
					tx.setDebit(-amount);
				}
				tx.setNote(resultSet.getString("note"));
				tx.setUuid(resultSet.getString("uuid"));
				list.add(tx);
			}
			return list;
		}
		catch(Exception e)
		{
			throw new BudgetException("getTransactList failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public List<Account> getTransferAccountList(User currentUser, Account accountThis)
	{
		Connection c = null;
		List<Account> list = new ArrayList<Account>();
		try
		{
			c = open();
			String sql;
			PreparedStatement ps;
			if (accountThis.getPub())
			{
			    if (accountThis.getOwner().equals(currentUser))
			    {
			        // Owner trying to transfer from public account
			        sql = "select * from account a where a.id <> ? and (a.ownerid = ? or a.pub = 1) order by a.name";
			        ps = c.prepareStatement(sql);
		            ps.setInt(1, accountThis.getId());
		            ps.setInt(2, currentUser.getId());
			    }
			    else
			    {
			        // Non-owner trying to transfer from another's public account
			        sql = "select * from account a where a.id <> ? and a.pub = 1 order by a.name";
			        ps = c.prepareStatement(sql);
			        ps.setInt(1, accountThis.getId());
			    }
			}
			else
			{
			    if (accountThis.getOwner().equals(currentUser))
			    {
			        // Owner try to transfer from private account
			        sql = "select * from account a where a.id <> ? and a.ownerid = ? order by a.name";
			        ps = c.prepareStatement(sql);
		            ps.setInt(1, accountThis.getId());
		            ps.setInt(2, currentUser.getId());
			    }
			    else
			    {
			        // Non-owner trying to transfer from another's private account
			        return list;
			    }
			}
			ResultSet resultSet = ps.executeQuery();
			while (resultSet.next())
			{
				Account account = new Account();
				account.setId(resultSet.getInt("id"));
				account.setName(resultSet.getString("name"));
				account.setOwner(getUserById(resultSet.getInt("ownerid")));
				account.setHidden(resultSet.getInt("hide") == 1);
				if (currentUser.getShow() || !account.getHidden())
				{
    				Transact tx = getLastTransact(account.getId());
    				if (tx == null)
    				{
    					account.setBalance(0);
    				}
    				else
    				{
    					account.setBalance(tx.getBalance());
    				}
    				list.add(account);
				}
			}
		}
		catch(Exception e)
		{
			log.error("getTransferAccountList failed", e);
		}
		finally
		{
			close(c);
		}
		return list;
	}
	
	public Integer getMyPendingRequestListSize(User currentUser) throws BudgetException
	{
	    Integer count = 0;
        Connection c = null;
        try
        {
            c = open();
            String sql = "select * from request r where r.state = ? and r.fromid in (select a.id from account a where a.ownerid = ?)";
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setInt(1, Request.STATE_PENDING);
            ps.setInt(2, currentUser.getId());
            log.info("Executing: " + ps.toString()); 
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next())
            {
                count++;
            }
        }
        catch(Exception e)
        {
            throw new BudgetException("getMyPendingRequestListSize failed", e);
        }
        finally
        {
            close(c);
        }
        return count;
	}
	
	public List<Request> getRequestList(User currentUser) throws BudgetException
	{
	    Connection c = null;
	    List<Request> list = new ArrayList<Request>();
	    try
	    {
	        c = open();
	        String sql = "select * from request r where r.fromid in (select a.id from account a where a.ownerid = ?) or r.toid in (select a.id from account a where a.ownerid = ?) order by r.ts desc";
	        PreparedStatement ps = c.prepareStatement(sql);
	        ps.setInt(1, currentUser.getId());
	        ps.setInt(2, currentUser.getId());
	        log.info("Executing: " + ps.toString());
	        ResultSet resultSet = ps.executeQuery();
	        while (resultSet.next())
	        {
	            Request request = new Request();
	            request.setId(resultSet.getInt("id"));
	            request.setTs(resultSet.getTimestamp("ts"));
	            request.setFrom(getAccountByIdInternal(c, resultSet.getInt("fromid")));
	            request.setTo(getAccountByIdInternal(c, resultSet.getInt("toid")));
	            request.setState(resultSet.getInt("state"));
	            request.setAmount(resultSet.getInt("amount"));
	            request.setNote(resultSet.getString("note"));
                list.add(request);
	        }
	    }
        catch(Exception e)
        {
            throw new BudgetException("getRequestList failed", e);
        }
        finally
        {
            close(c);
        }
        return list;
	}

	public List<Account> getAccountList(User currentUser)
	{
		Connection c = null;
		List<Account> list = new ArrayList<Account>();
		try
		{
			c = open();
			String sql;
			if (currentUser.getRole().isAdmin())
			{
				sql = "select * from account a order by a.name";
			}
			else
			{
				sql = "select * from account a where a.ownerid = ? order by a.name";
			}
			PreparedStatement ps = c.prepareStatement(sql);
			if (!currentUser.getRole().isAdmin())
			{
				ps.setInt(1, currentUser.getId());
			}
			ResultSet resultSet = ps.executeQuery();
			while (resultSet.next())
			{
				Account account = new Account();
				account.setId(resultSet.getInt("id"));
				account.setName(resultSet.getString("name"));
				account.setOwner(getUserById(resultSet.getInt("ownerid")));
				account.setPub(resultSet.getInt("pub") == 1);
				account.setHidden(resultSet.getInt("hide") == 1);
                if (currentUser.getShow() || !account.getHidden())
                {
    				Transact tx = getLastTransact(account.getId());
    				if (tx == null)
    				{
    					account.setBalance(0);
    				}
    				else
    				{
    				    account.setLastTransactId(tx.getId());
    					account.setBalance(tx.getBalance());
    				}
    				list.add(account);
    			}
			}
		}
		catch(Exception e)
		{
			log.error("getAccountList failed", e);
		}
		finally
		{
			close(c);
		}
		return list;
	}

    public List<Account> getAccountListAll(User currentUser)
    {
        Connection c = null;
        List<Account> list = new ArrayList<Account>();
        try
        {
            c = open();
            String sql;
            if (currentUser.getRole().isAdmin())
            {
                sql = "select * from account a order by a.name";
            }
            else
            {
                sql = "select * from account a where a.ownerid = ? order by a.name";
            }
            PreparedStatement ps = c.prepareStatement(sql);
            if (!currentUser.getRole().isAdmin())
            {
                ps.setInt(1, currentUser.getId());
            }
            ResultSet resultSet = ps.executeQuery();
            while (resultSet.next())
            {
                Account account = new Account();
                account.setId(resultSet.getInt("id"));
                account.setName(resultSet.getString("name"));
                account.setOwner(getUserById(resultSet.getInt("ownerid")));
                account.setPub(resultSet.getInt("pub") == 1);
                account.setHidden(resultSet.getInt("hide") == 1);
                Transact tx = getLastTransact(account.getId());
                if (tx == null)
                {
                    account.setBalance(0);
                }
                else
                {
                    account.setLastTransactId(tx.getId());
                    account.setBalance(tx.getBalance());
                }
                Transact txBalanced = getBalancedTransactForAccount(c, account.getId());
                if (txBalanced == null)
                {
                    account.setLastBalance(0);
                }
                else
                {
                    account.setLastBalance(txBalanced.getBalance());
                }
                list.add(account);
            }
        }
        catch(Exception e)
        {
            log.error("getAccountListAll failed", e);
        }
        finally
        {
            close(c);
        }
        return list;
    }
	
	private Role getRoleByIdInternal(Connection c, int id) throws SQLException
	{
        PreparedStatement ps = c.prepareStatement("select * from roles r where r.id = ?");
        ps.setInt(1, id);
        ResultSet resultSet = ps.executeQuery();
        if (!resultSet.next())
        {
            return null;
        }
        Role role = new Role();
        role.setId(resultSet.getInt("id"));
        role.setName(resultSet.getString("name"));
        return role;
	}
	
	public Role getRoleById(int id) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			Role role = getRoleByIdInternal(c, id);
			if (role == null)
			{
			    throw new BudgetException("role not found: " + id);
			}
			return role;
		}
		catch(Exception e)
		{
			throw new BudgetException("getRoleById failed", e);
		}
		finally
		{
			close(c);
		}
	}

    public Transact getTransactById(Integer id) throws BudgetException
    {
        Connection c = null;
        try
        {
            c = open();
            return getTransactByIdInternal(c, id);
        }
        catch(SQLException e)
        {
            throw new BudgetException("getTransactById failed", e);
        }
        finally
        {
            close(c);
        }
    }
	
    private User getUserByIdInternal(Connection c, Integer id) throws SQLException
    {
        PreparedStatement ps = c.prepareStatement("select * from users u where u.id = ?");
        ps.setInt(1, id);
        ResultSet resultSet = ps.executeQuery();
        if (!resultSet.next())
        {
            return null;
        }
        User user = new User();
        user.setId(resultSet.getInt("id"));
        user.setName(resultSet.getString("name"));
        user.setPassword(resultSet.getString("password"));
        Role role = getRoleByIdInternal(c, resultSet.getInt("roleid"));
        user.setRole(role);;
        return user;
    }
    
	public User getUserById(Integer id) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			User user = getUserByIdInternal(c, id);
			if (user == null)
			{
			    throw new BudgetException("user not found: " + id);
			}
			return user;
		}
		catch(Exception e)
		{
			throw new BudgetException("getUserById failed", e);
		}
		finally
		{
			close(c);
		}
	}
	
	public User getUserByName(String name) throws BudgetException
	{
		Connection c = null;
		try
		{
			c = open();
			PreparedStatement ps = c.prepareStatement("select * from users u where u.name = ?");
			ps.setString(1, name);
			ResultSet resultSet = ps.executeQuery();
			if (!resultSet.next())
			{
				throw new BudgetException("user not found: " + name);
			}
			User user = new User();
			user.setId(resultSet.getInt("id"));
			user.setName(resultSet.getString("name"));
			user.setPassword(resultSet.getString("password"));
			user.setShow(resultSet.getInt("show") == 1 ? true : false);
			user.setLim(resultSet.getInt("lim"));
			Role role = getRoleById(resultSet.getInt("roleid"));
			user.setRole(role);;
			return user;
		}
		catch(Exception e)
		{
			throw new BudgetException("getUserByName failed", e);
		}
		finally
		{
			close(c);
		}
	}

	private void transactInternal(Connection c, Integer accountId, Integer amount, String note, String uuid) throws SQLException
	{
		Transact tx = getLastTransactInternal(c, accountId);
		Integer balancePrev = (tx == null ? 0 : tx.getBalance());
		PreparedStatement ps = c.prepareStatement("insert into transact(ts, accountid, amount, balance, note, uuid) values(?, ?, ?, ?, ?, ?)");
		ps.setTimestamp(1, new Timestamp((new Date()).getTime()));
		ps.setInt(2, accountId);
		ps.setInt(3, amount);
		ps.setInt(4, balancePrev + amount);
		ps.setString(5, note);
		ps.setString(6, uuid);
		log.info("Issuing update: " + ps.toString());
		ps.executeUpdate();
	}
	
	private Balance getLastBalanceInternal(Connection c) throws SQLException
	{
		PreparedStatement ps = c.prepareStatement("select * from balance order by transactid desc limit 1");
		ResultSet resultSet = ps.executeQuery();
		if (!resultSet.next())
		{
			return null;
		}
		Balance b = new Balance();
		b.setId(resultSet.getInt("id"));
		b.setTransactId(resultSet.getInt("transactId"));
		return b;
	}

	private Transact getBalancedTransactForAccount(Connection c, Integer accountId) throws SQLException
	{
	    Balance b = getLastBalanceInternal(c);
	    if (b == null)
	    {
	        return null;
	    }
        PreparedStatement ps = c.prepareStatement("select * from transact tx where tx.accountid = ? and tx.id <= ? order by id desc limit 1");
        ps.setInt(1, accountId);
        ps.setInt(2, b.getTransactId());
        ResultSet resultSet = ps.executeQuery();
        if (!resultSet.next())
        {
            return null;
        }
        Transact tx = new Transact();
        tx.setId(resultSet.getInt("id"));
        tx.setTs(resultSet.getTimestamp("ts"));
        Integer amount = resultSet.getInt("amount");
        if (amount > 0)
        {
            tx.setCredit(amount);
        }
        else
        {
            tx.setDebit(-amount);
        }
        tx.setBalance(resultSet.getInt("balance"));
        tx.setNote(resultSet.getString("note"));
        tx.setUuid(resultSet.getString("uuid"));
        return tx;
	}
	
	private Transact getTransactByIdInternal(Connection c, Integer id) throws SQLException
	{
        PreparedStatement ps = c.prepareStatement("select * from transact tx where tx.id = ?");
        ps.setInt(1, id);
        ResultSet resultSet = ps.executeQuery();
        if (!resultSet.next())
        {
            return null;
        }
        Transact tx = new Transact();
        tx.setId(resultSet.getInt("id"));
        tx.setTs(resultSet.getTimestamp("ts"));
        Integer amount = resultSet.getInt("amount");
        if (amount > 0)
        {
            tx.setCredit(amount);
        }
        else
        {
            tx.setDebit(-amount);
        }
        tx.setBalance(resultSet.getInt("balance"));
        tx.setNote(resultSet.getString("note"));
        tx.setUuid(resultSet.getString("uuid"));
        return tx;
	}
	
	private Transact getLastTransactInternal(Connection c, Integer accountId) throws SQLException
	{
		PreparedStatement ps = null;
		if (accountId == null)
		{
			ps = c.prepareStatement("select * from transact order by id desc limit 1");
		}
		else
		{
			ps = c.prepareStatement("select * from transact where accountid = ? order by id desc limit 1");
			ps.setInt(1, accountId);
		}
		ResultSet resultSet = ps.executeQuery();
		if (!resultSet.next())
		{
			return null;
		}
		Transact tx = new Transact();
		tx.setId(resultSet.getInt("id"));
		tx.setTs(resultSet.getTimestamp("ts"));
		Integer amount = resultSet.getInt("amount");
		if (amount > 0)
		{
			tx.setCredit(amount);
		}
		else
		{
			tx.setDebit(-amount);
		}
		tx.setBalance(resultSet.getInt("balance"));
		tx.setNote(resultSet.getString("note"));
		tx.setUuid(resultSet.getString("uuid"));
		return tx;
	}

	private Connection open() throws SQLException
	{
		Connection c = DriverManager.getConnection(url);
		c.setAutoCommit(false);
		return c;
	}

	private void commit(Connection c) throws SQLException
	{
		c.commit();
	}
	
	private void rollback(Connection c)
	{
		try
		{
			if (c != null)
			{
				c.rollback();
			}
		}
		catch(SQLException e)
		{
			log.error("rollback failed", e);
		}
	}
	
	private void close(Connection c)
	{
		try
		{
			if (c != null)
			{
				c.close();
			}
		}
		catch(SQLException e)
		{
			log.fatal("close failed", e);
		}
	}
}