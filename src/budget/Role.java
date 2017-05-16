package budget;

public class Role
{
	private Integer id;
	private String name;
	
	public Integer getId()
	{
		return id;
	}
	public void setId(Integer id)
	{
		this.id = id;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String name)
	{
		this.name = name;
	}
	public boolean isAdmin()
	{
		return (id == 0);
	}
	public String toString()
	{
	    return name;
	}
}
