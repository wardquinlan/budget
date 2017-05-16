package budget;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class BudgetAuthenticationProvider implements AuthenticationProvider
{
	private static final Log log = LogFactory.getLog(AuthenticationProvider.class);
	
	@Autowired
	private BudgetDAO budgetDAO;
	
	@Autowired
	private MessageDigestService messageDigestService;
	
	@Override
	public Authentication authenticate(Authentication authentication)
	{
		String username = authentication.getName();
        String password = messageDigestService.getMessageDigest(authentication.getCredentials().toString());
        try
        {
        	User user = budgetDAO.getUserByName(username);
        	if (password.equals(user.getPassword()))
        	{
        		  List<GrantedAuthority> grantedAuthorities = new ArrayList<GrantedAuthority>();
                  grantedAuthorities.add(new SimpleGrantedAuthority(user.getRole().getName()));
                  return new UsernamePasswordAuthenticationToken(username, password, grantedAuthorities);
            }
        	return null;
        }
        catch(BudgetException e)
        {
        	log.error("authentication failed", e);
        	return null;
        }
	}
	
	@Override
	public boolean supports(Class<?> authentication) 
	{
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
