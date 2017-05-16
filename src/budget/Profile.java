package budget;

public class Profile
{
	private String currentPassword;
	private String newPassword;
	private String newPassword2;
	private Boolean show;
	private String lim;
	
	public Profile()
	{
	}

	public String getCurrentPassword() {
		return currentPassword;
	}

	public void setCurrentPassword(String currentPassword) {
		this.currentPassword = currentPassword;
	}

	public String getNewPassword() {
		return newPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public String getNewPassword2() {
		return newPassword2;
	}

	public void setNewPassword2(String newPassword2) {
		this.newPassword2 = newPassword2;
	}

    public Boolean getShow()
    {
        return show;
    }

    public void setShow(Boolean show)
    {
        this.show = show;
    }

    public String getLim()
    {
        return lim;
    }

    public void setLim(String lim)
    {
        this.lim = lim;
    }
}
