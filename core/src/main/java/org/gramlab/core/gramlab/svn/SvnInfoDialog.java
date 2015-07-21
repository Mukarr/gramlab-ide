package fr.gramlab.svn;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import fr.gramlab.Main;
import fr.gramlab.project.Project;
import fr.umlv.unitex.frames.FrameUtil;
import fr.umlv.unitex.process.commands.SvnCommand;

@SuppressWarnings("serial")
public class SvnInfoDialog extends JDialog {
	
	public SvnInfoDialog(Project p) {
		super(Main.getMainFrame(), "SVN info on project "+p.getName(), true);
		JPanel pane=new JPanel(new BorderLayout());
		pane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		String output=SvnExecutor.getCommandOutput(new SvnCommand().info(p.getProjectDirectory(),false));
		JTextArea text=new JTextArea(output);
		text.setOpaque(false);
		text.setEditable(false);
		pane.add(text,BorderLayout.CENTER);
		JPanel down=new JPanel();
		JButton ok=new JButton("Ok");
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				dispose();
			}
		});
		down.add(ok);
		pane.add(down,BorderLayout.SOUTH);
		setContentPane(pane);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		FrameUtil.center(null,this);
		setVisible(true);
	}

}
