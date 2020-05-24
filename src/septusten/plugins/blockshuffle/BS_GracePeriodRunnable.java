package septusten.plugins.blockshuffle;

public class BS_GracePeriodRunnable implements Runnable {

	BS_GracePeriodRunnable(BS_Main mainRef)
	{
		m_MainRef = mainRef;
	}
	
	@Override
	public void run() 
	{
		m_MainRef.startMatch();
	}
	
	private BS_Main m_MainRef;

}
