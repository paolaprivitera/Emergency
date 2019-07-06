package it.polito.tdp.emergency.model;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import it.polito.tdp.emergency.model.Evento.TipoEvento;
import it.polito.tdp.emergency.model.Paziente.StatoPaziente;

public class Simulatore {

	// Coda degli eventi
	private PriorityQueue<Evento> queue = new PriorityQueue<>();

	// Modello del Mondo
	private List<Paziente> pazienti; // pazienti
	// Mi serve una lista di pazienti in attesa e poi la devo scandire
	// per capire quale paziente far entrare nello studio in base alla priorita'
	// allora faccio una coda prioritaria!
	private PriorityQueue<Paziente> salaAttesa;
	private int studiLiberi; // medici

	// Parametri di simulazione -> le costanti (in mano all'utente)
	private int NS = 3; // numero di studi medici
	private int NP = 50; // numero di pazienti in arrivo
	private Duration T_ARRIVAL = Duration.ofMinutes(15); // intervallo di tempo tra i pazienti

	private LocalTime T_inizio = LocalTime.of(8, 0);
	private LocalTime T_fine = LocalTime.of(20, 0);

	private int DURATION_TRIAGE = 5;
	private int DURATION_WHITE = 10;
	private int DURATION_YELLOW = 15;
	private int DURATION_RED = 30;
	private int TIMEOUT_WHITE = 120;
	private int TIMEOUT_YELLOW = 60;
	private int TIMEOUT_RED = 90;

	// Statistiche da calcolare
	private int numDimessi;
	private int numAbbandoni;
	private int numMorti;

	// Variabili interne
	// Meccanismo interno al simulatore per sapere quale triage simulare
	private StatoPaziente nuovoStatoPaziente; // si ricorda qual e' il prossimo stato paziente da assegnare
	
	private Duration intervalloPolling = Duration.ofMinutes(5);

	public Simulatore() { // costruttore
		this.pazienti = new ArrayList<Paziente>();
	}

	public void init() {
		// 1. Creare i pazienti
		LocalTime oraArrivo = T_inizio; // le 8 del mattino
		pazienti.clear(); // poiche' init puo' essere chiamato piu' volte
		for (int i = 0; i < NP; i++) {
			Paziente p = new Paziente(i + 1, oraArrivo); // creo un nuovo paziente
			pazienti.add(p); 

			// Attenzione al dettaglio
			oraArrivo = oraArrivo.plus(T_ARRIVAL); // nuovo oggetto uguale a quello vecchio con la modifica applicata
		}

		// Inizializzo la sala d'attesa vuota
		this.salaAttesa = new PriorityQueue<>(new PrioritaPaziente());

		// Creare gli studi medici -> inizializzo gli studi liberi
		studiLiberi = NS;

		// Inizializzo lo stato paziente
		nuovoStatoPaziente = nuovoStatoPaziente.WAITING_WHITE;

		// 2. Creare gli eventi iniziali
		queue.clear(); // puliamo la coda per sicurezza anche se dovrebbe essere pulita
		for (Paziente p : pazienti) { // Devo generare un evento di arrivo per ciascun paziente
			queue.add(new Evento(p.getOraArrivo(), TipoEvento.ARRIVO, p));
		}

		// lancia l'osservatore in polling
		queue.add(new Evento(T_inizio.plus(intervalloPolling), TipoEvento.POLLING, null));

		// 3. Resettare le statistiche
		numDimessi = 0;
		numAbbandoni = 0;
		numMorti = 0;
	}

	public void run() {

		while (!queue.isEmpty()) {
			Evento ev = queue.poll();
//			System.out.println(ev);

			Paziente p = ev.getPaziente();

			/*
			 * se la simulazione dovesse terminare alle 20:00
			 * if(ev.getOra().isAfter(T_fine)) break ;
			 */

			switch (ev.getTipo()) {

			case ARRIVO:
				// tra 5 minuti verra' assegnato un codice colore
				queue.add(new Evento(ev.getOra().plusMinutes(DURATION_TRIAGE), TipoEvento.TRIAGE, ev.getPaziente()));
				break;
				
				// devo aggiornare lo stato del mondo?
				// ho cambiato qualcosa nelle informazioni dei pazienti?
				// no, lo stato del paziente era new e rimane new fino a quando non prende un colore
				// cambia il numero di studi disponibili? no
				// devo aggiornare le statistiche? no

			case TRIAGE:
				p.setStato(nuovoStatoPaziente);

				if (p.getStato() == StatoPaziente.WAITING_WHITE)
					queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_WHITE), TipoEvento.TIMEOUT, p));
					// imposto il timeout dopo il quale te ne vai
				else if (p.getStato() == StatoPaziente.WAITING_YELLOW)
					queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_YELLOW), TipoEvento.TIMEOUT, p));
				else if (p.getStato() == StatoPaziente.WAITING_RED)
					queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_RED), TipoEvento.TIMEOUT, p));

				salaAttesa.add(p); // aggiungo il paziente alla sala d'attesa

				ruotaNuovoStatoPaziente();

				break;

			case VISITA:
				// determina il paziente con max priorita'�
				Paziente pazChiamato = salaAttesa.poll();
				if (pazChiamato == null)
					break;

				// paziente entra in uno studio
				StatoPaziente vecchioStato = pazChiamato.getStato();
				pazChiamato.setStato(StatoPaziente.TREATING);

				// studio diventa occupato
				studiLiberi--;

				// schedula l'uscita (CURATO) del paziente
				if (vecchioStato == StatoPaziente.WAITING_RED) {
					queue.add(new Evento(ev.getOra().plusMinutes(DURATION_RED), TipoEvento.CURATO, pazChiamato));
				} else if (vecchioStato == StatoPaziente.WAITING_YELLOW) {
					queue.add(new Evento(ev.getOra().plusMinutes(DURATION_YELLOW), TipoEvento.CURATO, pazChiamato));
				} else if (vecchioStato == StatoPaziente.WAITING_WHITE) {
					queue.add(new Evento(ev.getOra().plusMinutes(DURATION_WHITE), TipoEvento.CURATO, pazChiamato));
				}

				break;

			case CURATO:
				// paziente e' fuori
				p.setStato(StatoPaziente.OUT);

				// aggiorna numDimessi
				numDimessi++;

				// schedula evento VISITA "adesso" -> lo studio e' libero
				studiLiberi++;
				// Chiamo subito un nuovo paziente
				queue.add(new Evento(ev.getOra(), TipoEvento.VISITA, null));
				// RISCHIO: sto schedulando un evento VISITA anche se non c'e'
				// nessuno in lista d'attesa
				// Quindi aggiungo nel case VISITA un controllo nel caso in cui
				// pazChiamato = null
				
				break;

				// Meccanismo aggiuntivo per alimentare gli studi quando sono vuoti
				// e ci sono persone in lista d'attesa
				// c'e' uno studio libero -> non e' un evento
				// ma una condizione del mondo
				// Creo un meccanismo automatico per osservare lo stato:
				// considero un evento che si ripete periodicamente il cui scopo sia
				// osservare lo stato (stato di polling)
				// da aggiungere alla coda degli eventi
				// che nel caso in cui si verifichi una condizione (almeno una persona
				// nello studio e almeno uno studio libero) scheduli un evento
				// --> trasformare condizione di stato in evento da simulare
				// L'osservatore e' un tipo di evento nuovo (POLLING)
				
			case TIMEOUT:
				// rimuovi il paziente dalla lista d'attesa
				salaAttesa.remove(p) ;
				
				if (p.getStato() == StatoPaziente.WAITING_WHITE) {
					p.setStato(StatoPaziente.OUT);
					numAbbandoni++;
				} else if (p.getStato() == StatoPaziente.WAITING_YELLOW) {
					p.setStato(StatoPaziente.WAITING_RED);
					queue.add(new Evento(ev.getOra().plusMinutes(TIMEOUT_RED), TipoEvento.TIMEOUT, p));
					salaAttesa.add(p) ;
				} else if (p.getStato() == StatoPaziente.WAITING_RED) {
					p.setStato(StatoPaziente.BLACK);
					numMorti++;
				} else {
					System.out.println("Timeout anomalo nello stato " + p.getStato());
				}

				break;

			case POLLING:
				// verifica se ci sono pazienti in attesa con studi liberi
				if (!salaAttesa.isEmpty() && studiLiberi > 0) {
					queue.add(new Evento(ev.getOra(), TipoEvento.VISITA, null));
				}
				// rischedula se stesso
				if (ev.getOra().isBefore(T_fine)) { // perche' se no va all'infinito
					queue.add(new Evento(ev.getOra().plus(intervalloPolling), TipoEvento.POLLING, null));
				}
				break ;
			}

		}

	}

	private void ruotaNuovoStatoPaziente() {
		if (nuovoStatoPaziente == StatoPaziente.WAITING_WHITE)
			nuovoStatoPaziente = StatoPaziente.WAITING_YELLOW;
		else if (nuovoStatoPaziente == StatoPaziente.WAITING_YELLOW)
			nuovoStatoPaziente = StatoPaziente.WAITING_RED;
		else if (nuovoStatoPaziente == StatoPaziente.WAITING_RED)
			nuovoStatoPaziente = StatoPaziente.WAITING_WHITE;
	}

	public int getNS() {
		return NS;
	}

	public void setNS(int nS) {
		NS = nS;
	}

	public int getNP() {
		return NP;
	}

	public void setNP(int nP) {
		NP = nP;
	}

	public Duration getT_ARRIVAL() {
		return T_ARRIVAL;
	}

	public void setT_ARRIVAL(Duration t_ARRIVAL) {
		T_ARRIVAL = t_ARRIVAL;
	}

	public LocalTime getT_inizio() {
		return T_inizio;
	}

	public void setT_inizio(LocalTime t_inizio) {
		T_inizio = t_inizio;
	}

	public LocalTime getT_fine() {
		return T_fine;
	}

	public void setT_fine(LocalTime t_fine) {
		T_fine = t_fine;
	}

	public int getDURATION_TRIAGE() {
		return DURATION_TRIAGE;
	}

	public void setDURATION_TRIAGE(int dURATION_TRIAGE) {
		DURATION_TRIAGE = dURATION_TRIAGE;
	}

	public int getDURATION_WHITE() {
		return DURATION_WHITE;
	}

	public void setDURATION_WHITE(int dURATION_WHITE) {
		DURATION_WHITE = dURATION_WHITE;
	}

	public int getDURATION_YELLOW() {
		return DURATION_YELLOW;
	}

	public void setDURATION_YELLOW(int dURATION_YELLOW) {
		DURATION_YELLOW = dURATION_YELLOW;
	}

	public int getDURATION_RED() {
		return DURATION_RED;
	}

	public void setDURATION_RED(int dURATION_RED) {
		DURATION_RED = dURATION_RED;
	}

	public int getTIMEOUT_WHITE() {
		return TIMEOUT_WHITE;
	}

	public void setTIMEOUT_WHITE(int tIMEOUT_WHITE) {
		TIMEOUT_WHITE = tIMEOUT_WHITE;
	}

	public int getTIMEOUT_YELLOW() {
		return TIMEOUT_YELLOW;
	}

	public void setTIMEOUT_YELLOW(int tIMEOUT_YELLOW) {
		TIMEOUT_YELLOW = tIMEOUT_YELLOW;
	}

	public int getTIMEOUT_RED() {
		return TIMEOUT_RED;
	}

	public void setTIMEOUT_RED(int tIMEOUT_RED) {
		TIMEOUT_RED = tIMEOUT_RED;
	}

	public int getNumDimessi() {
		return numDimessi;
	}

	public int getNumAbbandoni() {
		return numAbbandoni;
	}

	public int getNumMorti() {
		return numMorti;
	}
}
