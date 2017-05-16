package budget;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

@Component
public class MessageDigestService 
{
	private static final Log log = LogFactory.getLog(MessageDigestService.class);
	
	private MessageDigest md;
	
	private MessageDigestService()
	{
		try
		{
			md = MessageDigest.getInstance("SHA-1");
		}
		catch(NoSuchAlgorithmException e)
		{
			log.fatal(e);
		}
	}
	
	public String getMessageDigest(String string)
	{
		try
		{
			byte[] bytes = md.digest(string.getBytes("UTF-8"));
			return Base64.getEncoder().encodeToString(bytes);
		}
		catch(UnsupportedEncodingException e)
		{
			log.fatal(e);
			return null;
		}
	}
}
