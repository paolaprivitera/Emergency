package it.polito.tdp.emergency.model;

import java.util.Comparator;

import it.polito.tdp.emergency.model.Paziente.StatoPaziente;

public class PrioritaPaziente implements Comparator<Paziente> {

	@Override
	public int compare(Paziente p1, Paziente p2) {
		
		// se p1 ha la precedenza rispetto a p2 devo restituire un valore negativo
		if (p1.getStato() == StatoPaziente.WAITING_RED && p2.getStato() != StatoPaziente.WAITING_RED)
			return -1;
		if (p1.getStato() != StatoPaziente.WAITING_RED && p2.getStato() == StatoPaziente.WAITING_RED)
			return +1;

		if (p1.getStato() == StatoPaziente.WAITING_YELLOW && p2.getStato() != StatoPaziente.WAITING_YELLOW)
			return -1;
		if (p1.getStato() != StatoPaziente.WAITING_YELLOW && p2.getStato() == StatoPaziente.WAITING_YELLOW)
			return +1;
		
		// non faccio il controllo con i bianchi perche' gia' li ho controllati negli if precedenti
		
		// caso in cui abbiano lo stesso codice colore allora controlliamo il tempo di arrivo
		return p1.getOraArrivo().compareTo(p2.getOraArrivo());
	}

}
