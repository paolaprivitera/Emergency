package it.polito.tdp.emergency.model;

import java.time.LocalTime;

public class Paziente {
	
	public enum StatoPaziente { // stato in cui si trova il paziente
								// informazione propria del paziente
		NEW,
		WAITING_WHITE,
		WAITING_YELLOW,
		WAITING_RED,
		TREATING, // in visita
		OUT,
		BLACK,
	}
	
	private int id ; // modo per distinguere un paziente dall'altro
	private StatoPaziente stato ;
	private LocalTime oraArrivo ; // ** ora simulata
	
	public Paziente(int id, LocalTime oraArrivo) { // costruttore
		this.id = id ;
		this.oraArrivo = oraArrivo ;
		// ** a parita' di codice (colore), chiamo i pazienti in ordine di arrivo
		// cioe' non chi ha aspettato di piu' ma chi e' arrivato prima!!!
		// tra due codici rossi non prendo chi ha il codice rosso da piu' tempo
		// ma chi e' in sala d'attesa da piu' tempo
		// Quindi quando memorizziamo un paziente dobbiamo memorizzare
		// anche l'informazione relativa a quando e' arrivato in sala d'attesa
		this.stato = StatoPaziente.NEW ; // paziente che arriva dall'esterno 
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public StatoPaziente getStato() {
		return stato;
	}

	public void setStato(StatoPaziente stato) {
		this.stato = stato;
	}

	public LocalTime getOraArrivo() {
		return oraArrivo;
	}

	public void setOraArrivo(LocalTime oraArrivo) {
		this.oraArrivo = oraArrivo;
	}

	@Override
	public String toString() {
		return String.format("Paziente [id=%s, stato=%s, oraArrivo=%s]", id, stato, oraArrivo);
	}
	
	

}
