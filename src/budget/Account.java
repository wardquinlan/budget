package budget;

import java.util.List;

public class Account
{
	private static CurrencyFormatter currencyFormatter = new CurrencyFormatter();

	private Integer id;
	private String name;
	private String nameOrig;
	private String transactionAmount;
	private Integer idTo;
	private String note;
	private Integer lastTransactId;
	private Integer balance;
	private Integer lastBalance;
	private List<Transact> transactList;
	private User owner;
	private Boolean pub;
	private Boolean hidden;

	public Integer getId()
	{
		return id;
	}

	public void setId(Integer id)
	{
		this.id = id;
	}

	public String getExtendedName()
	{
		return name + " (" + owner.getName() + ")";
	}
	
	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getTransactionAmount()
	{
		return transactionAmount;
	}

	public void setTransactionAmount(String transactionAmount)
	{
		this.transactionAmount = transactionAmount;
	}

	public String getNote()
	{
		return note;
	}

	public void setNote(String note)
	{
		this.note = note;
	}

	public List<Transact> getTransactList()
	{
		return transactList;
	}
	
	public void setTransactList(List<Transact> transactList)
	{
		this.transactList = transactList;
	}

	public Integer getBalance()
	{
		return balance;
	}

	public void setBalance(Integer balance)
	{
		this.balance = balance;
	}
	public Integer getLastBalance()
    {
        return lastBalance;
    }

    public void setLastBalance(Integer lastBalance)
    {
        this.lastBalance = lastBalance;
    }

    public String getFormattedLastBalance()
    {
        return currencyFormatter.format(lastBalance);
    }
    public String getFormattedBalance()
	{
		return currencyFormatter.format(balance);
	}

	public Integer getIdTo()
	{
		return idTo;
	}

	public void setIdTo(Integer idTo)
	{
		this.idTo = idTo;
	}

	public User getOwner()
	{
		return owner;
	}

	public void setOwner(User owner)
	{
		this.owner = owner;
	}

	public Boolean getPub()
	{
		return pub;
	}

	public void setPub(Boolean pub)
	{
		this.pub = pub;
	}

	public Boolean getHidden()
    {
        return hidden;
    }

    public void setHidden(Boolean hidden)
    {
        this.hidden = hidden;
    }

    public Integer getLastTransactId() {
		return lastTransactId;
	}

	public void setLastTransactId(Integer lastTransactId) {
		this.lastTransactId = lastTransactId;
	}

	public String getNameOrig()
    {
        return nameOrig;
    }

    public void setNameOrig(String nameOrig)
    {
        this.nameOrig = nameOrig;
    }

    @Override
    public int hashCode()
    {
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((id == null) ? 0 : id.hashCode());
	    return result;
    }

	@Override
    public boolean equals(Object obj)
    {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (getClass() != obj.getClass())
		    return false;
	    Account other = (Account) obj;
	    if (id == null)
	    {
		    if (other.id != null)
			    return false;
	    } else if (!id.equals(other.id))
		    return false;
	    return true;
    }
}