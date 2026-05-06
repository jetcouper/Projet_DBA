package ca.qc.cvm.dba.scoutlog.view;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import ca.qc.cvm.dba.scoutlog.app.Facade;
import ca.qc.cvm.dba.scoutlog.event.BackEvent;
import ca.qc.cvm.dba.scoutlog.view.FrameMain.Views;
import ca.qc.cvm.dba.scoutlog.view.util.BackgroundPanel;

public class PanelPlanetsViewer extends CommonPanel {
	private static final long serialVersionUID = 1L;

	private JLabel galaxyName;
	
	public PanelPlanetsViewer(int width, int height) throws Exception {
		super(width, height, true, "assets/images/background-galaxy.jpg");
	}
	
	@Override
	protected void jbInit() throws Exception {
		super.addLabel("Nom de la galaxie : ", 20, 20, 250, 30);
		galaxyName = super.addLabel("", 300, 20, 80, 30);
		galaxyName.setHorizontalAlignment(JLabel.CENTER);
		galaxyName.setBorder(BorderFactory.createLineBorder(Color.WHITE));
		
		// Utilisez super.addField et super.addButton et etc pour cr�er votre interface graphique
	}
	
	/**
	 * Cette méthode est appelée automatiquement à chaque fois qu'un panel est affiché (lorsqu'on arrive sur la page)
	 */
	@Override
	public void resetUI() {
		String result = JOptionPane.showInputDialog(PanelPlanetsViewer.this, "Pour quelle galaxie?");
		
		if (result != null && result.trim().length() > 0) {
			galaxyName.setText(result);
			// Aller chercher les résultats et afficher ici..
		}
		else {
			Facade.getInstance().processEvent(new BackEvent());
		}
	}

}
