/*
 *
 * Modified by SQLeo Visual Query Builder :: java database frontend with join definitions
 * Copyright (C) 2012 anudeepgade@users.sourceforge.net
 * 
 * SQLeonardo :: java database frontend
 * Copyright (C) 2004 nickyb@users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package com.sqleo.environment.mdi;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import com.sqleo.common.gui.Toolbar;
import com.sqleo.common.jdbc.ConnectionAssistant;
import com.sqleo.common.jdbc.ConnectionHandler;
import com.sqleo.common.util.Text;
import com.sqleo.environment.Application;
import com.sqleo.environment.Preferences;
import com.sqleo.environment.ctrl.CommandEditor;
import com.sqleo.environment.ctrl.editor.DialogCommand;
import com.sqleo.environment.ctrl.editor.DialogFindReplace;
import com.sqleo.environment.ctrl.editor.SQLStyledDocument;
import com.sqleo.querybuilder.DiagramLayout;
import com.sqleo.querybuilder.QueryModel;
import com.sqleo.querybuilder.syntax.SQLParser;


public class ClientCommandEditor extends MDIClientWithCRActions implements
		_ConnectionListener {
	public static final String DEFAULT_TITLE = "command editor";

	private CommandEditor control;
	private JMenuItem[] m_actions;
	private Toolbar toolbar;
	private JComboBox cbx;
	private JCheckBox cbxLimit;
	private JTextField txtLimit;

	private DialogFindReplace dlg;

	public ClientCommandEditor() {
		super(DEFAULT_TITLE);

		setComponentCenter(control = new CommandEditor());
		control.setBorder(new EmptyBorder(2, 2, 2, 2));

		createToolbar();
		initMenuActions();
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		InternalFrameListener ifl = new InternalFrameAdapter() {
			@Override
			public void internalFrameDeactivated(InternalFrameEvent e) {
				if (ClientCommandEditor.this.dlg != null) {
					ClientCommandEditor.this.dlg.setVisible(false);
				}
			}
			@Override
			public void internalFrameClosing(InternalFrameEvent e) {
				openSaveQueryDialog();
				ClientCommandEditor.this.dispose();
			}
			
		};
		addInternalFrameListener(ifl);

		Application.window.addListener(this);
	}
	
	private void openSaveQueryDialog(){
		if(control.getDocument().getLength()>0){
			int option = JOptionPane.showConfirmDialog(Application.window,"Do you want to save query to a file ?",Application.PROGRAM,JOptionPane.YES_NO_OPTION);
			if(option == JOptionPane.YES_OPTION){
				toolbar.getActionMap().get("save").actionPerformed(null);
			}
		}
	}

	private void createToolbar() {
		cbx = new JComboBox(ConnectionAssistant.getHandlers().toArray());
		cbxLimit = new JCheckBox("limit rows:", true);
		cbxLimit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ClientCommandEditor.this.txtLimit
						.setEnabled(ClientCommandEditor.this.cbxLimit
								.isSelected());
			}
		});
		txtLimit = new JTextField("100") {
			@Override
			public Dimension getPreferredSize() {
				Dimension dim = super.getPreferredSize();
				dim.width = 75;
				return dim;
			}

			@Override
			public Dimension getMaximumSize() {
				return getPreferredSize();
			}
		};

		toolbar = new Toolbar(Toolbar.HORIZONTAL);
		toolbar.add(new ActionOpen());
		Action saveAction = new ActionSave();
		toolbar.getActionMap().put("save",saveAction);
		toolbar.add(saveAction);
		toolbar.addSeparator();
		toolbar.add(new ActionShowFindReplace());
		toolbar.addSeparator();
		toolbar.add(control.getActionMap().get("start-task"));
		toolbar.add(control.getActionMap().get("stop-task"));
		
		toolbar.addSeparator();
		Action commit = new ActionCommit(getActiveConnection());
		Action rollback = new ActionRollback(getActiveConnection());
		toolbar.getActionMap().put("action-commit",commit);
		toolbar.getActionMap().put("action-rollback",rollback);
		toolbar.add(commit);
		toolbar.add(rollback);
		toolbar.addSeparator();
		
		toolbar.add(cbxLimit);
		toolbar.add(txtLimit);
		toolbar.addSeparator();
		toolbar.add(new JLabel("use connection: "));
		toolbar.add(cbx);

		setComponentEast(toolbar);
	}

	private void initMenuActions() {
		m_actions = new JMenuItem[] {
				MDIMenubar.createItem(new ActionCommand()), null,
				MDIMenubar.createItem(new ActionClearInput()),
				MDIMenubar.createItem(new ActionClearOutput()), null,
				MDIMenubar.createItem(new ActionReverseSyntax()), };
	}

	@Override
	public final void dispose() {
		String limit = txtLimit.getText().trim();
		if (limit == null || limit.length() == 0) {
			limit = "0";
		}

		Preferences.set("editor.limit.rows", new Integer(limit));
		Preferences.set("editor.limit.enabled",
				new Boolean(cbxLimit.isSelected()));

		super.dispose();
	}

	public final CommandEditor getControl() {
		return control;
	}

	@Override
	public JMenuItem[] getMenuActions() {
		return m_actions;
	}

	@Override
	public Toolbar getSubToolbar() {
		return toolbar;
	}

	@Override
	public final String getName() {
		return DEFAULT_TITLE;
	}

	@Override
	protected void setPreferences() {
		cbxLimit.setSelected(Preferences.getBoolean("editor.limit.enabled",
				true));
		txtLimit.setText(String.valueOf(Preferences.getInteger(
				"editor.limit.rows", 100)));
	}

	public int getLimitRows() {
		String limit = txtLimit.getText().trim();
		if (limit == null || limit.length() == 0) {
			limit = "0";
		}
		return cbxLimit.isSelected() ? Integer.valueOf(limit).intValue() : 0;
	}

	@Override
	public void onConnectionClosed(String keycah) {
		cbx.removeItem(keycah);
	}

	@Override
	public void onConnectionOpened(String keycah) {
		cbx.addItem(keycah);
	}

	public String getActiveConnection() {
		return cbx.getSelectedIndex() == -1 ? null : cbx.getSelectedItem()
				.toString();
	}
	@Override
	public void notifyResponseToView(boolean isCommitNotify){
		if(isCommitNotify){
			control.getResponseArea().append("\nCommit successfull!\n");
		}else{
			control.getResponseArea().append("\nRollback successfull!\n");
		}
		try {
			int line = control.getResponseArea().getLineCount();
			int off = control.getResponseArea().getLineStartOffset(line - 1);
			control.getResponseArea().setCaretPosition(off);
		} catch (BadLocationException e) {
			e.printStackTrace();
		}

	};
	
	public void setActiveConnection(String keycah) {
		cbx.setSelectedItem(keycah);
	}

	private class ActionCommand extends AbstractAction {
		ActionCommand() {
			putValue(NAME, "new command...");
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (getActiveConnection() != null) {
				new DialogCommand(getActiveConnection(), null).setVisible(true);
			} else {
				Application.alert(Application.PROGRAM, "No connection!");
			}
		}
	}

	private class ActionClearInput extends AbstractAction {
		ActionClearInput() {
			putValue(NAME, "clear request area");
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_MASK));
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			ClientCommandEditor.this.control
					.setDocument(new SQLStyledDocument());
			ClientCommandEditor.this.control.transferFocus();
		}
	}

	private class ActionClearOutput extends AbstractAction {
		ActionClearOutput() {
			putValue(NAME, "clear response area");
			putValue(Action.ACCELERATOR_KEY,
					KeyStroke.getKeyStroke(KeyEvent.VK_B, KeyEvent.CTRL_MASK));
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			ClientCommandEditor.this.control.clearResponse();
			ClientCommandEditor.this.control.transferFocus();
		}
	}

	private class ActionOpen extends MDIActions.AbstractBase {
		private ActionOpen() {
			setIcon(Application.ICON_EDITOR_OPEN);
			setTooltip("open");
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			String currentDirectory = Preferences.getString("lastDirectory",
					System.getProperty("user.home"));

			JFileChooser fc = new JFileChooser(currentDirectory);
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fc.setMultiSelectionEnabled(true);

			fc.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory()
							|| file.getName().endsWith(".sql");
				}

				@Override
				public String getDescription() {
					return "script files (*.sql)";
				}
			});

			if (fc.showOpenDialog(Application.window) == JFileChooser.APPROVE_OPTION) {
				Preferences.set("lastDirectory", fc.getCurrentDirectory()
						.toString());
				SQLStyledDocument doc = new SQLStyledDocument();

				for (int i = 0; i < fc.getSelectedFiles().length; i++) {
					String filename = fc.getSelectedFiles()[i].toString();
					try {
						load(doc, filename);
						doc.insertString(doc.getLength(), "\n", null);
					} catch (BadLocationException ble) {
						Application.println(ble, false);
					} catch (IOException ioe) {
						Application.println(ioe, false);
					}
				}

				ClientCommandEditor.this.control.setDocument(doc);
			}
		}

		private void load(SQLStyledDocument doc, String filename)
				throws IOException, BadLocationException {
			Reader in = new FileReader(filename);

			int nch;
			char[] buff = new char[4096];
			while ((nch = in.read(buff, 0, buff.length)) != -1) {
				doc.insertString(doc.getLength(), new String(buff, 0, nch),
						null);
			}
			in.close();
		}
	}

	private class ActionSave extends MDIActions.AbstractBase {
		private ActionSave() {
			setIcon(Application.ICON_EDITOR_SAVE);
			setTooltip("save");
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			String currentDirectory = Preferences.getString("lastDirectory",
					System.getProperty("user.home"));

			JFileChooser fc = new JFileChooser(currentDirectory);
			fc.setFileSelectionMode(JFileChooser.FILES_ONLY);

			fc.setFileFilter(new FileFilter() {
				@Override
				public boolean accept(File file) {
					return file.isDirectory()
							|| file.getName().endsWith(".sql");
				}

				@Override
				public String getDescription() {
					return "script files (*.sql)";
				}
			});

			if (fc.showSaveDialog(Application.window) == JFileChooser.APPROVE_OPTION) {
				Preferences.set("lastDirectory", fc.getCurrentDirectory()
						.toString());
				String filename = fc.getSelectedFile().toString();

				if (fc.getFileFilter().getDescription().endsWith("(*.sql)")) {
					if (!filename.endsWith(".sql")) {
						filename += ".sql";
					}
				}

				save(filename);
			}
		}

		private void save(String filename) {
			try {
				Document doc = ClientCommandEditor.this.control.getDocument();
				Writer out = new FileWriter(filename);
				out.write(doc.getText(0, doc.getLength()));
				out.flush();
				out.close();
			} catch (BadLocationException ble) {
				Application.println(ble, false);
			} catch (IOException ioe) {
				Application.println(ioe, false);
			}
		}
	}

	private class ActionReverseSyntax extends AbstractAction {
		ActionReverseSyntax() {
			putValue(NAME, "reverse syntax");
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (getActiveConnection() == null) {
				Application.alert(Application.PROGRAM, "No connection!");
				return;
			}

			String sql = ClientCommandEditor.this.control.getSelectedText();
			if (Text.isEmpty(sql)) {
				Application.alert(Application.PROGRAM, "Nothing selected!");
				return;
			}

			try {
				QueryModel qm = SQLParser.toQueryModel(sql);
				if (!Preferences.getBoolean("querybuilder.use-schema")) {
					ConnectionHandler ch = ConnectionAssistant
							.getHandler(getActiveConnection());
					ArrayList schemas = (ArrayList) ch
							.getObject("$schema_names");
					if (schemas.size() > 0) {
						Object schema = JOptionPane.showInputDialog(
								Application.window, "schema:",
								Application.PROGRAM, JOptionPane.PLAIN_MESSAGE,
								null, schemas.toArray(), null);
						if (schema == null) {
							return;
						}
						qm.setSchema(schema.toString());
					}
				}

				DiagramLayout dl = new DiagramLayout();
				dl.setQueryModel(qm);

				ClientQueryBuilder cqb = new ClientQueryBuilder(
						getActiveConnection());
				Application.window.add(cqb);
				cqb.setDiagramLayout(dl);
			} catch (IOException e) {
				Application.println(e, true);
			}
		}
	}

	private class ActionShowFindReplace extends AbstractAction {
		ActionShowFindReplace() {
			putValue(SMALL_ICON,
					Application.resources.getIcon(Application.ICON_FIND));
			putValue(SHORT_DESCRIPTION, "find/replace...");
			putValue(NAME, "find/replace...");
		}

		@Override
		public void actionPerformed(ActionEvent ae) {
			if (ClientCommandEditor.this.dlg == null) {
				ClientCommandEditor.this.dlg = new DialogFindReplace(
						getControl().getRequestArea());
			}
			ClientCommandEditor.this.dlg.setVisible(true);
		}
	}


}