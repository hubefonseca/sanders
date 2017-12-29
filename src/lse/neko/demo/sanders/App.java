package lse.neko.demo.sanders;

import java.util.Random;
import java.util.logging.Logger;

import lse.neko.ActiveLayer;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.util.logging.NekoLogger;

public class App extends ActiveLayer {

	private static final Logger logger =
        NekoLogger.getLogger(App.class.getName());
	
	private int n;
	private int me;
	private int[] meArray;
	private static double startTime;
	private int my_timestamp;
    private int current_time;
    
    
    // registering the message types and associating names with the types.
    // already registered by monitor
//    static {
//	   	MessageTypes.instance().register(Constants.MSG_CSREQ, "MSG_CSREQ");
//	   	MessageTypes.instance().register(Constants.MSG_CSRELEASE, "MSG_CSRELEASE");
//	   	MessageTypes.instance().register(Constants.MSG_VOTEINQUIRE, "MSG_VOTEINQUIRE");
//	   	MessageTypes.instance().register(Constants.MSG_VOTERELINQUISH, "MSG_VOTERELINQUISH");
//	   	MessageTypes.instance().register(Constants.MSG_VOTEYES, "MSG_VOTEYES");
//    }
    
	public App(NekoProcess process) {
        super(process, "Monitor_p" + process.getID());
        start();
        
        // the number of processes
        n = process.getN();

        // Pega id deste processo
        me = process.getID();
        meArray = new int[1];
        meArray[0] = me;
    }
	
	public void run() {
		Random generator = new Random();

		while (true) {			
			// Delay para fazer o pedido em tempos aleatórios
			try {
				long outCSMaxTime = (long) (generator.nextDouble() * 10000);
				//logger.info(clock() - startTime + " [APP] me "+ me + " delay to request CS is " + outCSMaxTime + "ms");
				synchronized(this) {
					sleep(outCSMaxTime);
				}
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			
			// Entra na região crítica
			enterCS();

			// Faz o eventual processamento
			logger.info(clock()-startTime+" [APP " + me + "] está na região crítica!");

			try {
				long inCSMaxTime = (long) (generator.nextDouble() * 10000);
				//logger.info(clock() - startTime + " [APP] me "+ me + " delay to request CS is " + outCSMaxTime + "ms");
				synchronized(this) {
					sleep(inCSMaxTime);
				}
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}

			// Sai da região crítica
			exitCS();
		}
	}
	
	/**
	 * Pedido de entrada em região crítica. Pergunta a todos os processos
	 * em S e aguarda os votos.
	 *
	 */
	private void enterCS() {
		NekoMessage message = null;
		
		// send to all in conteire[me]
		sender.send(new NekoMessage(me, meArray, 0, Constants.APP_CSREQ));		
		logger.info(clock() - startTime + " [APP " + me + "] requisitou entrada na CS");
		
		boolean done = false;
		while (!done) {
			message = receive();
			if (message.getType() == Constants.APP_CSAVAIL)
				done = true;
		}
		
	}
	
	private void exitCS() {
		logger.info(clock() - startTime + " [APP " + me + "] saiu da região crítica");
		sender.send(new NekoMessage(me, meArray, 0, Constants.APP_CSRELEASE));
	}

}
