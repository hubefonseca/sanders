package lse.neko.demo.sanders;

public class Constants {

	// Tipos de mensagem do monitor
	public static final int MSG_CSREQ = 1000;
	public static final int MSG_CSRELEASE = 1001;
	public static final int MSG_VOTERELINQUISH = 1002;
	public static final int MSG_VOTEYES = 1003;
	public static final int MSG_VOTEINQUIRE = 1004;
	
	// Tipos de mensagem da aplicaçao
	public static final int APP_CSREQ = 1010;
	public static final int APP_CSAVAIL = 1011;
	public static final int APP_CSRELEASE = 1012;
	
	public static final int MAX_TIME_IN_CS = 3000;
	
	
	public static int[][] coteries = {
    		{1,2}, //S0
    		{0,3}, //S1
    		{0,3}, //S2
    		{1,2}, //S3
    	};
	
}
