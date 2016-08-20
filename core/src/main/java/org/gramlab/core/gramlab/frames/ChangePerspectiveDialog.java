package org.gramlab.core.gramlab.frames;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.WindowConstants;

import org.gramlab.core.Main;
import org.gramlab.core.gramlab.util.KeyUtil;
import org.gramlab.core.umlv.unitex.common.project.manager.GlobalProjectManager;
import org.gramlab.core.umlv.unitex.config.ConfigManager;
import org.gramlab.core.umlv.unitex.frames.FrameUtil;

/**
 * Dialog to switch between the two perspectives of the Integrated IDE namely, Classic and Project-oriented.
 * This same dialog would be used both at the start of the IDE, to choose a perspective and switch to other when working with one.
 * 
 * @author Mukarram Tailor
 *
 */


@SuppressWarnings("serial")
public class ChangePerspectiveDialog extends JDialog {
	
	/**
	 * 
	 * @param currPerspective
	 * the currently active perspective, 'Classic', 'Project-oriented' or 'none'(in case of start up)
	 * 
	 * @param args
	 * command line arguments at start-up
	 * 
	 */
	public ChangePerspectiveDialog(String currPerspective, String[] args) {
		super(Main.getMainFrame(), "Choose perspective", true);
		setContentPane(constructPanel(currPerspective, args));
		setAlwaysOnTop(true);
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setVisible(true);
	}

	private JPanel constructPanel(final String currPerspective, final String[] args) {
		JPanel p=new JPanel(new BorderLayout());
		p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		p.setPreferredSize(new Dimension(400, 200));
		p.add(new JLabel("Select the perspective you want to use "),BorderLayout.NORTH);
		JPanel center=new JPanel(new GridLayout(7,7));
		center.add(new JLabel(""));
		final JRadioButton classic=new JRadioButton("Classic",true);
		final JRadioButton projectOriented=new JRadioButton("Project-oriented",false);
		ButtonGroup bg=new ButtonGroup();
		center.add(classic);
		center.add(projectOriented);
		bg.add(classic);
		bg.add(projectOriented);
		p.add(center,BorderLayout.CENTER);
		JPanel down=new JPanel();
		final JButton ok=new JButton("OK");
		if(currPerspective.equals("Classic")){
			classic.setEnabled(false);
			projectOriented.setSelected(true);
		}else if(currPerspective.equals("Project-oriented")){
			projectOriented.setEnabled(false);
			classic.setSelected(true);
		}
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (classic.isSelected()) {
					if(currPerspective.equals("none")){
						setVisible(false);
						openClassic(args);
					}
					else{
						setVisible(false);
						switchToClassic(args);
					}
				} else if (projectOriented.isSelected()) {
					if(currPerspective.equals("none")){
						setVisible(false);
						openProjectOriented(args);
					}
					else{
						setVisible(false);
						switchToProjectOriented(args);
					}
				} 
				pack();
				FrameUtil.center(getOwner(),ChangePerspectiveDialog.this);
			}
		});
		JButton cancel=new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});
		down.add(ok);
		down.add(cancel);
		KeyUtil.addCRListener(ok);
		KeyUtil.addCRListener(cancel);
		KeyUtil.addEnterListener(p, ok);
		KeyUtil.addEscListener(p, cancel);
		p.add(down,BorderLayout.SOUTH);
		return p;
	}
	
	protected void openClassic(String[] args) {
		Main.launchUnitex(args);
	}
	
	protected void openProjectOriented(String[] args) {
		Main.launchGramlab(args);
	}
	
	protected void switchToProjectOriented(String[] args) {
		if(Main.getProjectorientedMainFrame()!=null){
			Main.getProjectorientedMainFrame().setVisible(true);
			//set Current projectManager back to Initial GramlabProjectManager
			GlobalProjectManager.setGlobalProjectManager(GlobalProjectManager.getGramlabProjectManager()); 
			//set Current configManager back to initial GramlabPreferences
			ConfigManager.setManager(ConfigManager.getGramlabPreferences());
		}
		else{
			Main.launchGramlab(args);
		}
		Main.getClassicMainFrame().setVisible(false);
	}

	protected void switchToClassic(String[] args) {
		if(Main.getClassicMainFrame()!=null){
			Main.getClassicMainFrame().setVisible(true);
			//set Current projectManager back to Initial UnitexProjectManager
			GlobalProjectManager.setGlobalProjectManager(GlobalProjectManager.getUnitexProjectManager());
			//set Current configManager back to initial GramlabPreferences
			ConfigManager.setManager(ConfigManager.getUnitexConfigManager());
		}else{
			Main.launchUnitex(args);
		}
		Main.getProjectorientedMainFrame().setVisible(false);
	}
}
