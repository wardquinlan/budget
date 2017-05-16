package budget;

import java.util.ArrayList;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class BudgetUser implements UserDetails
{
	private static final long serialVersionUID = -432278512425307117L;
	
	private User user;
	
	public BudgetUser(User user)
	{
		this.user = user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities()
	{
		Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
		GrantedAuthority authority = new GrantedAuthority()
		{
			public String getAuthority()
			{
				return user.getRole().getName();
			}
		};
		authorities.add(authority);
		return authorities;
	}
	
	@Override
	public String getUsername()
	{
		return user.getName();
	}
	
	@Override
	public String getPassword()
	{
		return user.getPassword();
	}

	@Override
	public boolean isAccountNonExpired()
	{
		return true;
	}
	
	@Override
	public boolean isAccountNonLocked()
	{
		return true;
	}
	
	@Override
	public boolean isCredentialsNonExpired()
	{
		return true;
	}
	
	@Override
	public boolean isEnabled()
	{
		return true;
	}
}
