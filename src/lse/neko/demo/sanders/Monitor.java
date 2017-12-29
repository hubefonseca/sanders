package lse.neko.demo.sanders;

import java.util.Collections;
import java.util.Comparator;
import java.util.Properties;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;

import lse.neko.ActiveLayer;
import lse.neko.MessageTypes;
import lse.neko.NekoMessage;
import lse.neko.NekoProcess;
import lse.neko.util.logging.NekoLogger;

public class Monitor extends ActiveLayer {

	private static final Logger logger =
		NekoLogger.getLogger(Monitor.class.getName());

	private static double startTime = 0;

	// registering the message types and associating names with the types.
	static {
		MessageTypes.instance().register(Constants.MSG_CSREQ, "MSG_CSREQ");
		MessageTypes.instance().register(Constants.MSG_CSRELEASE, "MSG_CSRELEASE");
		MessageTypes.instance().register(Constants.MSG_VOTEINQUIRE, "MSG_VOTEINQUIRE");
		MessageTypes.instance().register(Constants.MSG_VOTERELINQUISH, "MSG_VOTERELINQUISH");
		MessageTypes.instance().register(Constants.MSG_VOTEYES, "MSG_VOTEYES");
	}

	public Monitor(NekoProcess process) {
		super(process, "Monitor_p" + process.getID());
		start();
		logger.info((clock() - startTime) + " mon_p" + process.getID() + " was created");
	}

	public void run() {
		
		// Id deste processo
		int me = process.getID();
		int meArray[] = new int[1];
		meArray[0] = me;

		logger.info(clock() - startTime + " [MONITOR] Process " + me + ": monitor started");

		// Inicializa o relógio lógico de Lamport
		int message_timestamp = 0; // timestamp recebido em uma mensagem
		int local_timestamp = 0; // timestamp atual no processo local
		int request_cs_timestamp = 0; // timestamp em que o processo atual requisitou a entrada na CS
		int candidate_timestamp = 0; // timestamp do processo candidato a entrar na CS no momento

		NekoMessage message;

		int yes_votes = 0;
		boolean inCS = false;
		boolean hasVoted = false;
		boolean inquired = false;
		int candidate_id = 0;
		
		Vector<Process> deferredQueue = new Vector<Process>();
		int[] to;
		Random generator;
		Process candidate;


		while(true) {
			// Aguarda o recebimento de uma mensagem
			message = receive();
			if (message == null) {
				System.out.println(clock() - startTime + " [MONITOR " + me + "]: null message");
			}

			if (me == message.getSource()) {
				
				switch(message.getType()) {

				// Mensagens da aplicação
				case Constants.APP_CSREQ:
					// Requisita entrada na CS
					if (!hasVoted) {
						request_cs_timestamp = local_timestamp;
						sender.send(new NekoMessage(me, Constants.coteries[me], new Integer(local_timestamp), Constants.MSG_CSREQ));
					}
					else {
						deferredQueue.add(new Process(me, local_timestamp));
					}
					break;
					
				case Constants.MSG_VOTEYES:
					// Requisita entrada na CS
					request_cs_timestamp = local_timestamp;
					sender.send(new NekoMessage(me, Constants.coteries[me], new Integer(local_timestamp), Constants.MSG_CSREQ));
					break;

				case Constants.APP_CSRELEASE:
					// Avisa demais monitores da liberação
					inCS = false;
					sender.send(new NekoMessage(me, Constants.coteries[me], new Integer(local_timestamp), Constants.MSG_CSRELEASE));
					break;
				}

			}
			else if (me != message.getSource()) {

				// Ajusta o relógio lógico de Lamport
				message_timestamp = ((Integer) message.getContent()).intValue();
				local_timestamp = (message_timestamp > local_timestamp) ? message_timestamp : local_timestamp + 1;

				// Simula atraso de canal para gerar os casos de relinquish
				generator = new Random();
				try {
					double slp = generator.nextDouble() * 1000;
					//logger.info(clock() - startTime + "[MONITOR] " + me + ": Espera " + slp + "ms para processar a mensagem");
					sleep(slp);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				switch(message.getType()) {					

				// Mensagens entre monitores
				case Constants.MSG_CSREQ:
					System.out.println(local_timestamp + " [MONITOR " + me + "] recebe requsição de " + message.getSource());
					
					if (!hasVoted || inCS) {
						// Responde sim	
						to = new int[1];
						to[0] = message.getSource();
						System.out.println(local_timestamp + " [MONITOR " + me + "] responde YES para pedido de " + to[0]);
						sender.send(new NekoMessage(me, to, new Integer(local_timestamp), Constants.MSG_VOTEYES));
						candidate_id = message.getSource();
						candidate_timestamp = message_timestamp;
						hasVoted = true;
					}
					else {
						deferredQueue.add(new Process(message.getSource(), message_timestamp));
						System.out.println(local_timestamp + " [MONITOR " + me + "] guarda requisição de " + message.getSource());
						if (request_cs_timestamp < candidate_timestamp && !inquired) {
							// Pede anulação do voto dado anteriormente
							to = new int[1];
							to[0] = candidate_id;
							System.out.println(local_timestamp + " [MONITOR " + me + "] pede anulação para " + to[0]);
							sender.send(new NekoMessage(me, to, new Integer(candidate_timestamp), Constants.MSG_VOTEINQUIRE));
							inquired = true;
						}
					}
					break;

				case Constants.MSG_VOTERELINQUISH:

					deferredQueue.add(new Process(candidate_id, candidate_timestamp));
					candidate = buscaMenorTimestamp(deferredQueue);
					to = new int[1];          
					to[0] = candidate.id;
					System.out.println(local_timestamp + " [MONITOR " + me + "] tem confirmação da anulação de " + to[0]);
					sender.send(new NekoMessage(me, to, new Integer(local_timestamp), Constants.MSG_VOTEYES));
					candidate_id = candidate.id;
					candidate_timestamp = candidate.timestamp;
					inquired = false;
					break;

				case Constants.MSG_CSRELEASE:

					if (deferredQueue.size() > 0) {
						candidate = buscaMenorTimestamp(deferredQueue);
						to = new int[1];
						to[0] = candidate.id;
						System.out.println(local_timestamp + " [MONITOR " + me + "] responde YES para o pedido na pilha de " + to[0]);
						sender.send(new NekoMessage(me, to, new Integer(local_timestamp), Constants.MSG_VOTEYES));
						candidate_id = candidate.id;
						candidate_timestamp = candidate.timestamp;
					}
					else {
						hasVoted = false;
					}
					inquired = false;
					break;


				case Constants.MSG_VOTEYES:
					// Contabiliza o voto positivo
					yes_votes++;
					if (yes_votes == Constants.coteries[me].length) {
						inCS = true;
						receiver.deliver(new NekoMessage(me, meArray, new Integer(0), Constants.APP_CSAVAIL));
						yes_votes = 0;
					}
					break;

				case Constants.MSG_VOTEINQUIRE:
					// Contabiliza o voto negativo se o timestamp for anterior
					System.out.println(local_timestamp + " [MONITOR " + me + "] recebe pedido de anulação de " + message.getSource() + " com timestamp " + message.getContent());
					if (request_cs_timestamp >= message_timestamp) {
						to = new int[1];
						to[0] = message.getSource();
						System.out.println(local_timestamp + " [MONITOR " + me + "] aceita anulação de " + message.getSource());
						sender.send(new NekoMessage(me, to, new Integer(local_timestamp), Constants.MSG_VOTERELINQUISH));
						yes_votes--;
					}
					else {
						System.out.println(local_timestamp + " [MONITOR " + me + "] não aceita anulação de " + message.getSource());						
					}
					break;

				default:
					System.out.println("[MONITOR] message unknown me " + me + " from " + message.getSource()+" type="+message.getType());
				break;					

				}	
			}		
		}
	}

	private Process buscaMenorTimestamp(Vector<Process> processes) {
		int min_timestamp = processes.elementAt(0).timestamp;
		Process p = null;
		Process q = null;

		for (int i = 0; i < processes.size(); i++) {
			if (processes.elementAt(i).timestamp <= min_timestamp) {
				min_timestamp = processes.elementAt(i).timestamp;
				p = processes.elementAt(i);
			}
		}		
		q = new Process(p.id, p.timestamp);
		processes.remove(p);
		return q;
	}


	class Process {
		int id;
		int timestamp;

		Process(int i, int t) {
			id = i;
			timestamp = t;
		}
	}
}
